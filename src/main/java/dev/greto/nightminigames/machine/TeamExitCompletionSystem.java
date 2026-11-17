package dev.greto.nightminigames.machine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.door.DoorSystem;
import dev.greto.nightminigames.feed.ClassificationFeedSystem;
import dev.greto.nightminigames.key.KeyMessages;
import dev.greto.nightminigames.module.ModuleSystem;
import dev.greto.nightminigames.region.RegionKind;
import dev.greto.nightminigames.region.RegionSelectionSystem;
import dev.greto.nightminigames.team.TeamBinding;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.PlayerTeam;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.List;
import java.util.Map;

public final class TeamExitCompletionSystem {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Session> SESSIONS = new HashMap<>();
    private static final List<String> QUALIFIED = new ArrayList<>();
    private static final Map<String, Map<UUID, String>> QUALIFIED_MEMBERS = new HashMap<>();
    private static final Map<String, Integer> AWARDED_POINTS = new HashMap<>();
    private static UUID roundId = UUID.randomUUID();
    private static int qualifyingSlots = 7;
    private static boolean finished;
    private static MinecraftServer activeServer;
    private static boolean initialized;

    private TeamExitCompletionSystem() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            activeServer = server;
            load(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            SESSIONS.clear();
            QUALIFIED.clear();
            QUALIFIED_MEMBERS.clear();
            AWARDED_POINTS.clear();
            roundId = UUID.randomUUID();
            qualifyingSlots = 7;
            finished = false;
            activeServer = null;
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> syncPlayer(handler.player)));
        ServerTickEvents.END_SERVER_TICK.register(TeamExitCompletionSystem::tick);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> commands() {
        return Commands.literal("clasificacion")
            .then(Commands.literal("estado").executes(context -> report(context.getSource())))
            .then(Commands.literal("lista").executes(context -> list(context.getSource())))
            .then(Commands.literal("cupos")
                .executes(context -> report(context.getSource()))
                .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 7))
                    .executes(context -> setSlots(context.getSource(), IntegerArgumentType.getInteger(context, "cantidad")))));
    }

    public static void begin(ServerLevel level, BlockPos position, String teamName) {
        if (finished) {
            return;
        }
        if (!TeamBinding.valid(teamName)) {
            return;
        }
        MinecraftServer server = level.getServer();
        if (server.getScoreboard().getPlayerTeam(teamName) == null || SESSIONS.containsKey(teamName)) {
            return;
        }
        SESSIONS.put(teamName, new Session(level.dimension().location().toString(), position.asLong(), false, false));
        save(server);
        DoorSystem.onNewTeamCompletion(level.dimension().location().toString(), teamName);
        broadcast(server, teamName, true);
    }

    public static void reset(ServerLevel level, BlockPos position) {
        if (finished) {
            return;
        }
        String dimension = level.dimension().location().toString();
        long packedPosition = position.asLong();
        MinecraftServer server = level.getServer();
        var iterator = SESSIONS.entrySet().iterator();
        boolean modified = false;
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Session session = entry.getValue();
            if (session.dimension().equals(dimension) && session.machinePos() == packedPosition) {
                broadcast(server, entry.getKey(), false);
                QUALIFIED.remove(entry.getKey());
                QUALIFIED_MEMBERS.remove(entry.getKey());
                AWARDED_POINTS.remove(entry.getKey());
                iterator.remove();
                modified = true;
            }
        }
        if (modified) {
            save(server);
            ClassificationFeedSystem.classificationStatus(server, QUALIFIED.size(), qualifyingSlots);
        }
    }

    public static void resetRound(MinecraftServer server) {
        for (String team : new ArrayList<>(SESSIONS.keySet())) {
            broadcast(server, team, false);
        }
        SESSIONS.clear();
        QUALIFIED.clear();
        QUALIFIED_MEMBERS.clear();
        AWARDED_POINTS.clear();
        roundId = UUID.randomUUID();
        finished = false;
        save(server);
    }

    public static void syncPlayer(ServerPlayer player) {
        if (finished) {
            return;
        }
        MinecraftServer server = player.server;
        for (var entry : SESSIONS.entrySet()) {
            if (memberOf(server, player, entry.getKey()) && !entry.getValue().arrived()) {
                MachineCompletionSync.send(player, entry.getKey(), true);
            }
        }
    }

    public static boolean hasTeamCompletion(String dimension, String team) {
        Session session = SESSIONS.get(team);
        return session != null && session.dimension().equals(dimension);
    }

    public static boolean consumeDoorClose(MinecraftServer server, String dimension, String team) {
        Session session = SESSIONS.get(team);
        if (session == null || !session.dimension().equals(dimension) || !session.arrived() || session.doorClosed()) {
            return false;
        }
        SESSIONS.put(team, new Session(session.dimension(), session.machinePos(), true, true));
        save(server);
        return true;
    }

    private static void tick(MinecraftServer server) {
        if (server != activeServer || finished || server.getTickCount() % 5 != 0) {
            return;
        }
        boolean modified = false;
        var iterator = SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            String teamName = entry.getKey();
            Session session = entry.getValue();
            PlayerTeam team = server.getScoreboard().getPlayerTeam(teamName);
            if (team == null) {
                QUALIFIED.remove(teamName);
                QUALIFIED_MEMBERS.remove(teamName);
                AWARDED_POINTS.remove(teamName);
                iterator.remove();
                modified = true;
                continue;
            }
            if (!session.arrived() && allActiveMembersInExit(server, team, session.dimension(), teamName)) {
                entry.setValue(new Session(session.dimension(), session.machinePos(), true, false));
                broadcast(server, teamName, false);
                if (QUALIFIED.size() < qualifyingSlots && !QUALIFIED.contains(teamName)) {
                    QUALIFIED.add(teamName);
                    dev.greto.nightminigames.integration.MapWaypointBridge.requestRefresh(server);
                    QUALIFIED_MEMBERS.put(teamName, qualifiedMembers(server, team, session.dimension(), teamName));
                    ClassificationFeedSystem.teamQualified(server, teamName, QUALIFIED.size(), qualifyingSlots);
                    int recipients = QUALIFIED_MEMBERS.get(teamName).size();
                    dev.greto.nightminigames.admin.AdminMessages.broadcast(server,
                        KeyMessages.message("EQUIPO " + teamName.toUpperCase() + " CLASIFICADO · " + recipients
                            + " jugadores con derecho a puntos · " + QUALIFIED.size() + "/" + qualifyingSlots
                            + " plazas. Esperando entrega manual: /nightminigames puntos equipo " + teamName + " <cantidad>",
                            TeamBinding.color(teamName)));
                } else if (!QUALIFIED.contains(teamName)) {
                    ClassificationFeedSystem.teamWithoutSlot(server, teamName, QUALIFIED.size(), qualifyingSlots);
                }
                modified = true;
                if (QUALIFIED.size() >= qualifyingSlots) {
                    break;
                }
            }
        }
        if (QUALIFIED.size() >= qualifyingSlots && !finished) {
            completeRound(server);
        }
        if (modified) {
            save(server);
        }
    }

    private static void completeRound(MinecraftServer server) {
        finished = true;
        for (String team : SESSIONS.keySet()) {
            broadcast(server, team, false);
        }
        ModuleSystem.finishAfterQualification(server);
        String names = String.join(", ", QUALIFIED.stream().map(String::toUpperCase).toList());
        dev.greto.nightminigames.admin.AdminMessages.broadcast(server,
            KeyMessages.message("RONDA FINALIZADA · " + QUALIFIED.size() + "/" + qualifyingSlots +
                " equipos clasificados: " + names + ". Puntos disponibles; usa /nightminigames inicio reiniciar para otra ronda.", ChatFormatting.GREEN));
        for (int i = 0; i < QUALIFIED.size(); i++) {
            String team = QUALIFIED.get(i);
            int place = i + 1;
            int players = QUALIFIED_MEMBERS.getOrDefault(team, Map.of()).size();
            dev.greto.nightminigames.admin.AdminMessages.broadcast(server,
                KeyMessages.message(place + ". EQUIPO " + team.toUpperCase() + " · " + players
                    + " participantes registrados · PUNTOS PENDIENTES", TeamBinding.color(team)));
        }
        NightminigamesMod.LOGGER.info("[Nightminigames] Ronda finalizada: {}", names);
    }

    public static boolean isFinished() {
        return finished;
    }

    public static List<String> qualifiedTeams() {
        return List.copyOf(QUALIFIED);
    }

    private static boolean allActiveMembersInExit(MinecraftServer server, PlayerTeam team, String dimension, String teamName) {
        if (!RegionSelectionSystem.complete(RegionKind.SALIDA, teamName)
            || !dimension.equals(RegionSelectionSystem.dimension(RegionKind.SALIDA, teamName))) {
            return false;
        }
        int activeMembers = 0;
        for (String memberName : team.getPlayers()) {
            ServerPlayer player = server.getPlayerList().getPlayerByName(memberName);
            if (!eligible(player)) {
                continue;
            }
            activeMembers++;
            if (!dimension.equals(player.serverLevel().dimension().location().toString())
                || !RegionSelectionSystem.isInside(RegionKind.SALIDA, player, teamName)) {
                return false;
            }
        }
        return activeMembers > 0;
    }

    public static Map<UUID, String> qualifiedMembers(MinecraftServer server, PlayerTeam team, String dimension, String teamName) {
        Map<UUID, String> members = new LinkedHashMap<>();
        for (String name : team.getPlayers()) {
            ServerPlayer player = server.getPlayerList().getPlayerByName(name);
            if (eligible(player) && dimension.equals(player.serverLevel().dimension().location().toString())
                && RegionSelectionSystem.isInside(RegionKind.SALIDA, player, teamName)) {
                members.put(player.getUUID(), player.getGameProfile().getName());
            }
        }
        return members;
    }

    public static boolean isQualified(String team) {
        return QUALIFIED.contains(team);
    }

    public static int qualifyingSlots() {
        return qualifyingSlots;
    }

    public static int qualifiedCount() {
        return QUALIFIED.size();
    }

    public static Map<UUID, String> recipients(String team) {
        return Map.copyOf(QUALIFIED_MEMBERS.getOrDefault(team, Map.of()));
    }

    public static boolean hasSnapshot(String team) {
        return QUALIFIED_MEMBERS.containsKey(team);
    }

    public static Integer awardedAmount(String team) {
        return AWARDED_POINTS.get(team);
    }

    public static UUID roundId() {
        return roundId;
    }

    public static void markAwarded(MinecraftServer server, String team, int amount) {
        if (!ModuleSystem.TEAMS.contains(team)) {
            return;
        }
        AWARDED_POINTS.put(team, amount);
        save(server);
    }

    public static boolean eligible(ServerPlayer player) {
        if (player == null || !player.isAlive()) {
            return false;
        }
        GameType mode = player.gameMode.getGameModeForPlayer();
        return mode == GameType.SURVIVAL || mode == GameType.ADVENTURE;
    }

    private static int setSlots(CommandSourceStack source, int slots) {
        MinecraftServer server = source.getServer();
        if (finished) {
            source.sendFailure(KeyMessages.message("La ronda ya finalizó. Reinicia antes de cambiar los cupos.", ChatFormatting.RED));
            return 0;
        }
        if (slots < QUALIFIED.size()) {
            source.sendFailure(KeyMessages.message(
                "No puedes establecer " + slots + " cupos: ya clasificaron " + QUALIFIED.size() + " equipos. Reinicia sus máquinas para liberar plazas.",
                ChatFormatting.RED));
            return 0;
        }
        qualifyingSlots = slots;
        save(server);
        ClassificationFeedSystem.classificationStatus(server, QUALIFIED.size(), qualifyingSlots);
        source.sendSuccess(() -> KeyMessages.message(
            "Cupos de clasificación: " + slots + " equipos. Clasificados: " + QUALIFIED.size()
                + ". Disponibles: " + (slots - QUALIFIED.size()) + ".", ChatFormatting.GREEN), false);
        return slots;
    }

    private static int report(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        source.sendSuccess(() -> KeyMessages.message(
            "CLASIFICACIÓN " + (finished ? "· FINALIZADA: " : "· EN CURSO: ") + QUALIFIED.size() + "/" + qualifyingSlots
                + " equipos · FALTAN " + (qualifyingSlots - QUALIFIED.size()) + " plaza(s).", ChatFormatting.AQUA), false);
        for (String name : ModuleSystem.TEAMS) {
            PlayerTeam team = server.getScoreboard().getPlayerTeam(name);
            int active = 0;
            int arrived = 0;
            int inactive = 0;
            if (team != null) {
                for (String playerName : team.getPlayers()) {
                    ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
                    if (eligible(player)) {
                        active++;
                        if (RegionSelectionSystem.isInside(RegionKind.SALIDA, player, name)) {
                            arrived++;
                        }
                    } else {
                        inactive++;
                    }
                }
            }
            Session session = SESSIONS.get(name);
            String state = QUALIFIED.contains(name) ? "CLASIFICADO"
                : session == null ? "SIN MAQUINA COMPLETA"
                : session.arrived() ? "LLEGÓ SIN CUPO" : "EN CURSO";
            int activeCount = active;
            int arrivedCount = arrived;
            int excluded = inactive;
            source.sendSuccess(() -> KeyMessages.message(
                name.toUpperCase() + " · " + state + " · salida " + arrivedCount + "/" + activeCount
                    + " activos · excluidos " + excluded, TeamBinding.color(name)), false);
        }
        return QUALIFIED.size();
    }

    private static int list(CommandSourceStack source) {
        source.sendSuccess(() -> KeyMessages.message(
            "Equipos clasificados " + (finished ? "(RONDA FINALIZADA)" : "(EN CURSO)") + ": " + QUALIFIED.size() + "/" + qualifyingSlots
                + " · Quedan " + (qualifyingSlots - QUALIFIED.size()) + " cupos.", ChatFormatting.AQUA), false);
        for (int index = 0; index < QUALIFIED.size(); index++) {
            String team = QUALIFIED.get(index);
            int place = index + 1;
            source.sendSuccess(() -> KeyMessages.message(place + ". " + team.toUpperCase(), TeamBinding.color(team)), false);
        }
        return QUALIFIED.size();
    }

    private static void broadcast(MinecraftServer server, String teamName, boolean visible) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!visible || memberOf(server, player, teamName)) {
                MachineCompletionSync.send(player, teamName, visible);
            }
        }
    }

    private static boolean memberOf(MinecraftServer server, ServerPlayer player, String teamName) {
        PlayerTeam assigned = server.getScoreboard().getPlayersTeam(player.getScoreboardName());
        return assigned != null && assigned.getName().equals(teamName);
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("nightminigames_team_exit.json");
    }

    private static void load(MinecraftServer server) {
        SESSIONS.clear();
        QUALIFIED.clear();
        QUALIFIED_MEMBERS.clear();
        AWARDED_POINTS.clear();
        qualifyingSlots = 7;
        finished = false;
        roundId = UUID.randomUUID();
        Path file = file(server);
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.has("roundId")) {
                roundId = UUID.fromString(json.get("roundId").getAsString());
            }
            if (json.has("qualifyingSlots")) {
                qualifyingSlots = Math.clamp(json.get("qualifyingSlots").getAsInt(), 1, 7);
            }
            if (json.has("finished")) {
                finished = json.get("finished").getAsBoolean();
            }
            if (json.has("teams") && json.get("teams").isJsonArray()) {
                for (var element : json.getAsJsonArray("teams")) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject value = element.getAsJsonObject();
                    String name = value.get("team").getAsString();
                    if (!TeamBinding.valid(name) || server.getScoreboard().getPlayerTeam(name) == null) {
                        continue;
                    }
                    SESSIONS.put(name, new Session(
                        value.get("dimension").getAsString(),
                        value.get("machinePos").getAsLong(),
                        value.get("arrived").getAsBoolean(),
                        value.get("doorClosed").getAsBoolean()
                    ));
                }
            }
            if (json.has("qualifiedTeams") && json.get("qualifiedTeams").isJsonArray()) {
                for (var item : json.getAsJsonArray("qualifiedTeams")) {
                    String team = item.getAsString();
                    if (SESSIONS.containsKey(team) && SESSIONS.get(team).arrived() && !QUALIFIED.contains(team) && QUALIFIED.size() < 7) {
                        QUALIFIED.add(team);
                    }
                }
            } else {
                for (String team : ModuleSystem.TEAMS) {
                    if (SESSIONS.containsKey(team) && SESSIONS.get(team).arrived() && QUALIFIED.size() < qualifyingSlots) {
                        QUALIFIED.add(team);
                    }
                }
            }
            qualifyingSlots = Math.max(qualifyingSlots, QUALIFIED.size());
            if (QUALIFIED.size() >= qualifyingSlots) {
                finished = true;
            }
            if (json.has("qualifiedMembers") && json.get("qualifiedMembers").isJsonObject()) {
                JsonObject members = json.getAsJsonObject("qualifiedMembers");
                for (String team : QUALIFIED) {
                    if (!members.has(team) || !members.get(team).isJsonObject()) {
                        continue;
                    }
                    Map<UUID, String> roster = new LinkedHashMap<>();
                    for (var member : members.getAsJsonObject(team).entrySet()) {
                        roster.put(UUID.fromString(member.getKey()), member.getValue().getAsString());
                    }
                    if (!roster.isEmpty()) {
                        QUALIFIED_MEMBERS.put(team, roster);
                    }
                }
            }
            if (json.has("awardedPoints") && json.get("awardedPoints").isJsonObject()) {
                for (var award : json.getAsJsonObject("awardedPoints").entrySet()) {
                    if (QUALIFIED.contains(award.getKey())) {
                        AWARDED_POINTS.put(award.getKey(), award.getValue().getAsInt());
                    }
                }
            }
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] Error cargando progreso de clasificación", exception);
            SESSIONS.clear();
            QUALIFIED.clear();
            QUALIFIED_MEMBERS.clear();
            AWARDED_POINTS.clear();
            roundId = UUID.randomUUID();
            qualifyingSlots = 7;
            finished = false;
        }
    }

    private static void save(MinecraftServer server) {
        Path file = file(server);
        Path temporary = file.resolveSibling(file.getFileName().toString() + ".tmp");
        JsonArray teams = new JsonArray();
        for (var entry : SESSIONS.entrySet()) {
            Session session = entry.getValue();
            JsonObject value = new JsonObject();
            value.addProperty("team", entry.getKey());
            value.addProperty("dimension", session.dimension());
            value.addProperty("machinePos", session.machinePos());
            value.addProperty("arrived", session.arrived());
            value.addProperty("doorClosed", session.doorClosed());
            teams.add(value);
        }
        JsonObject data = new JsonObject();
        data.add("teams", teams);
        data.addProperty("qualifyingSlots", qualifyingSlots);
        data.addProperty("finished", finished);
        JsonArray classified = new JsonArray();
        for (String team : QUALIFIED) {
            classified.add(team);
        }
        data.add("qualifiedTeams", classified);
        data.addProperty("roundId", roundId.toString());
        JsonObject recipients = new JsonObject();
        for (var entry : QUALIFIED_MEMBERS.entrySet()) {
            JsonObject players = new JsonObject();
            entry.getValue().forEach((uuid, name) -> players.addProperty(uuid.toString(), name));
            recipients.add(entry.getKey(), players);
        }
        data.add("qualifiedMembers", recipients);
        JsonObject awards = new JsonObject();
        AWARDED_POINTS.forEach(awards::addProperty);
        data.add("awardedPoints", awards);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temporary, GSON.toJson(data), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] Error guardando progreso de clasificación", exception);
        }
    }

    private record Session(String dimension, long machinePos, boolean arrived, boolean doorClosed) {
    }
}
