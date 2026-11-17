package dev.greto.nightminigames.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.door.DoorSystem;
import dev.greto.nightminigames.key.KeyMessages;
import dev.greto.nightminigames.registry.ModItems;
import dev.greto.nightminigames.region.RegionKind;
import dev.greto.nightminigames.region.RegionSelectionSystem;
import dev.greto.nightminigames.team.TeamCapacitySystem;
import dev.greto.nightminigames.persistence.WorldStructureJson;
import dev.greto.nightminigames.team.TeamBinding;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.PlayerTeam;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ModuleSystem {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String REUNION = "reunion";
    public static final List<String> TEAMS = List.of("rojo", "azul", "verde", "amarillo", "naranja", "morado", "rosa");
    private static final List<String> DESTINATIONS = List.of("reunion", "rojo", "azul", "verde", "amarillo", "naranja", "morado", "rosa", "reunion_rojo", "reunion_azul", "reunion_verde", "reunion_amarillo", "reunion_naranja", "reunion_morado", "reunion_rosa");
    private static final Map<String, Station> POINTS = new HashMap<>();
    private static final Map<String, List<String>> KITS = new HashMap<>();
    private static final Map<String, Set<UUID>> DELIVERED = new HashMap<>();
    private static final Map<String, List<ItemStack>> PREPARED_KITS = new HashMap<>();
    private static final ArrayDeque<PendingTeleport> PENDING = new ArrayDeque<>();
    private static final int TELEPORTS_PER_TICK = 4;
    private static MinecraftServer activeServer;
    private static int successfulTeleports;
    private static int skippedTeleports;
    private static final Map<UUID, String> GLOBAL_ROSTER = new LinkedHashMap<>();
    private static final Set<UUID> GLOBAL_ARRIVED = new HashSet<>();
    private static boolean globalTeleporting;
    private static boolean globalReady;
    private static boolean startAfterTeleport;
    private static boolean globalStarted;
    private static boolean initialized;

    private ModuleSystem() {
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
            if (activeServer == server && !PENDING.isEmpty()) {
                save(server);
            }
            activeServer = null;
            POINTS.clear();
            KITS.clear();
            DELIVERED.clear();
            PREPARED_KITS.clear();
            PENDING.clear();
            GLOBAL_ROSTER.clear();
            GLOBAL_ARRIVED.clear();
            globalTeleporting = false;
            globalReady = false;
            startAfterTeleport = false;
            globalStarted = false;
        });
        ServerTickEvents.END_SERVER_TICK.register(ModuleSystem::tick);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> commands() {
        return Commands.literal("modulos")
            .then(Commands.literal("wand")
                .executes(context -> giveWand(context.getSource(), TEAMS.getFirst()))
                .then(Commands.argument("equipo", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(TEAMS, builder))
                    .executes(context -> giveWand(context.getSource(), StringArgumentType.getString(context, "equipo")))))
            .then(Commands.literal("punto")
                .then(Commands.literal("guardar")
                    .then(Commands.argument("modulo", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(DESTINATIONS, builder))
                        .executes(context -> saveSpot(context.getSource(), StringArgumentType.getString(context, "modulo")))))
                .then(Commands.literal("estado").executes(context -> spotStatus(context.getSource()))))
            .then(Commands.literal("kit")
                .then(Commands.literal("guardar")
                    .then(Commands.argument("equipo", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(TEAMS, builder))
                        .executes(context -> saveKit(context.getSource(), StringArgumentType.getString(context, "equipo")))))
                .then(Commands.literal("ver")
                    .then(Commands.argument("equipo", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(TEAMS, builder))
                        .executes(context -> showKit(context.getSource(), StringArgumentType.getString(context, "equipo")))))
                .then(Commands.literal("limpiar")
                    .then(Commands.argument("equipo", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(TEAMS, builder))
                        .executes(context -> clearKit(context.getSource(), StringArgumentType.getString(context, "equipo")))))
                .then(Commands.literal("reiniciar").executes(context -> clearDistribution(context.getSource()))))
            .then(Commands.literal("teleportar")
                .then(Commands.literal("todos").executes(context -> teleportTeams(context.getSource())))
                .then(Commands.argument("equipo", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(TEAMS, builder))
                    .executes(context -> teleportTeam(context.getSource(), StringArgumentType.getString(context, "equipo")))))
            .then(Commands.literal("estado").executes(context -> moduleStatus(context.getSource())))
            .then(Commands.literal("diagnostico").executes(context -> checkConfiguration(context.getSource(), null)));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> startCommands() {
        return Commands.literal("inicio")
            .then(Commands.literal("comenzar")
                .executes(context -> start(context.getSource()))
                .then(Commands.argument("equipo", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(TEAMS, builder))
                    .executes(context -> startTeam(context.getSource(), StringArgumentType.getString(context, "equipo")))))
            .then(Commands.literal("detener")
                .executes(context -> stop(context.getSource()))
                .then(Commands.argument("equipo", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(TEAMS, builder))
                    .executes(context -> stopTeam(context.getSource(), StringArgumentType.getString(context, "equipo")))))
            .then(Commands.literal("estado").executes(context -> startStatus(context.getSource())))
            .then(Commands.literal("comprobar")
                .executes(context -> checkConfiguration(context.getSource(), null))
                .then(Commands.argument("equipo", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(TEAMS, builder))
                    .executes(context -> checkConfiguration(context.getSource(), StringArgumentType.getString(context, "equipo")))))
            .then(Commands.literal("diagnostico").executes(context -> checkConfiguration(context.getSource(), null)))
            .then(Commands.literal("reiniciar").executes(context -> resetRound(context.getSource())));
    }

    private static boolean validTeam(CommandSourceStack source, String name) {
        if (TEAMS.contains(name)) {
            return true;
        }
        source.sendFailure(KeyMessages.message("Módulo inválido. Usa uno de los siete equipos.", ChatFormatting.RED));
        return false;
    }

    private static int giveWand(CommandSourceStack source, String team) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!validTeam(source, team)) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = new ItemStack(ModItems.WAND_MODULOS);
        TeamBinding.set(stack, team);
        ModuleWandItem.selectTeam(stack, team);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        source.sendSuccess(() -> KeyMessages.message("Varita de módulos " + team.toUpperCase() + " entregada. Clic derecho: guardar destino. El equipo queda fijado.", ChatFormatting.GREEN), false);
        return 1;
    }

    public static void markModulePoint(ServerPlayer player, String team, BlockPos stand) {
        if (!player.hasPermissions(2) || !TEAMS.contains(team)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos head = stand.above();
        if (!level.getBlockState(stand).getCollisionShape(level, stand).isEmpty()
            || !level.getBlockState(head).getCollisionShape(level, head).isEmpty()) {
            dev.greto.nightminigames.admin.AdminMessages.send(player, KeyMessages.message("Espacio ocupado. Selecciona una ubicación con dos bloques de altura libres.", ChatFormatting.RED));
            return;
        }
        POINTS.put(team, new Station(level.dimension().location().toString(), stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, player.getYRot(), player.getXRot()));
        save(player.server);
        dev.greto.nightminigames.admin.AdminMessages.send(player, KeyMessages.message("Destino del módulo " + team.toUpperCase() + " guardado: " + stand.toShortString() + " en " + level.dimension().location() + ".", ChatFormatting.GREEN));
    }

    public static void markMeetingPoint(ServerPlayer player, BlockPos position, String team) {
        if (!player.hasPermissions(2)) {
            dev.greto.nightminigames.admin.AdminMessages.send(player, KeyMessages.message("Solo los operadores pueden configurar puntos de reunión.", ChatFormatting.RED));
            return;
        }
        POINTS.put(team.isBlank() ? REUNION : "reunion_" + team, new Station(player.serverLevel().dimension().location().toString(), position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, player.getYRot(), player.getXRot()));
        save(player.server);
        dev.greto.nightminigames.admin.AdminMessages.send(player, KeyMessages.message("Punto de reunión " + team + " marcado en " + position.toShortString() + ".", ChatFormatting.GREEN));
    }

    private static int saveSpot(CommandSourceStack source, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!DESTINATIONS.contains(name)) {
            source.sendFailure(KeyMessages.message("Destino no válido.", ChatFormatting.RED));
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        POINTS.put(name, new Station(player.serverLevel().dimension().location().toString(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
        save(source.getServer());
        source.sendSuccess(() -> KeyMessages.message("Punto " + name + " guardado: " + player.blockPosition().toShortString() + " en " + player.serverLevel().dimension().location(), ChatFormatting.GREEN), false);
        return 1;
    }

    private static int spotStatus(CommandSourceStack source) {
        int configured = 0;
        for (String name : TEAMS) {
            Station point = POINTS.get(name);
            if (point != null) {
                configured++;
            }
            source.sendSuccess(() -> KeyMessages.message(name.toUpperCase() + ": " + (point == null ? "SIN CONFIGURAR" : point.dimension() + " [" + (int) point.x() + ", " + (int) point.y() + ", " + (int) point.z() + "]"), point == null ? ChatFormatting.YELLOW : ChatFormatting.AQUA), false);
        }
        return configured;
    }

    private static int saveKit(CommandSourceStack source, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!validTeam(source, name)) {
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        List<String> encoded = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                encoded.add(stack.save(source.getServer().registryAccess()).toString());
            }
        }
        KITS.put(name, encoded);
        PREPARED_KITS.remove(name);
        DELIVERED.remove(name);
        save(source.getServer());
        int size = encoded.size();
        source.sendSuccess(() -> KeyMessages.message("Kit del módulo " + name + " guardado con " + size + " stacks de la barra rápida.", ChatFormatting.GREEN), false);
        return size;
    }

    private static int showKit(CommandSourceStack source, String name) {
        if (!validTeam(source, name)) {
            return 0;
        }
        List<String> kit = KITS.get(name);
        int size = kit == null ? 0 : kit.size();
        source.sendSuccess(() -> KeyMessages.message("Kit de " + name + ": " + size + " stacks guardados.", ChatFormatting.AQUA), false);
        return size;
    }

    private static int clearKit(CommandSourceStack source, String name) {
        if (!validTeam(source, name)) {
            return 0;
        }
        KITS.remove(name);
        PREPARED_KITS.remove(name);
        DELIVERED.remove(name);
        save(source.getServer());
        source.sendSuccess(() -> KeyMessages.message("Kit de " + name + " borrado.", ChatFormatting.GREEN), false);
        return 1;
    }

    private static int clearDistribution(CommandSourceStack source) {
        DELIVERED.clear();
        save(source.getServer());
        source.sendSuccess(() -> KeyMessages.message("Entregas de kits reiniciadas para la próxima ronda.", ChatFormatting.GREEN), false);
        return 1;
    }

    private static boolean eligible(ServerPlayer player) {
        return player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL || player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE;
    }

    private static boolean canQueue(CommandSourceStack source) {
        if (PENDING.isEmpty()) {
            return true;
        }
        source.sendFailure(KeyMessages.message("Hay " + PENDING.size() + " teletransportes pendientes. Espera a que termine el lote.", ChatFormatting.YELLOW));
        return false;
    }

    private static void beginQueue(boolean global) {
        successfulTeleports = 0;
        skippedTeleports = 0;
        PREPARED_KITS.clear();
        GLOBAL_ROSTER.clear();
        GLOBAL_ARRIVED.clear();
        globalTeleporting = global;
        globalReady = false;
        startAfterTeleport = false;
        globalStarted = false;
    }

    private static int gather(CommandSourceStack source) {
        if (!canQueue(source)) {
            return 0;
        }
        Station spot = POINTS.get(REUNION);
        if (!ready(source, spot, REUNION)) {
            return 0;
        }
        beginQueue(false);
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            if (eligible(player)) {
                PENDING.addLast(new PendingTeleport(player.getUUID(), spot, ""));
            }
        }
        int count = PENDING.size();
        source.sendSuccess(() -> KeyMessages.message("Reunión programada para " + count + " participantes (hasta 4 por tick).", ChatFormatting.GREEN), false);
        return count;
    }

    private static int teleportTeams(CommandSourceStack source) {
        if (preventPostGameTeleport(source) || !canQueue(source)) return 0;
        Map<UUID, String> roster = participants(source.getServer(), null);
        if (roster.isEmpty()) {
            source.sendFailure(KeyMessages.message("No hay participantes conectados en Supervivencia o Aventura asignados a un equipo. Los jugadores sin equipo se ignoran.", ChatFormatting.YELLOW));
            return 0;
        }
        boolean valid = true;
        for (String team : new LinkedHashSet<>(roster.values())) {
            if (!ready(source, POINTS.get(team), team)) valid = false;
        }
        if (!valid) return 0;
        queueRoster(roster);
        int count = PENDING.size();
        source.sendSuccess(() -> KeyMessages.message("Distribución iniciada: " + count + " participantes. Los jugadores sin equipo y los equipos vacíos no bloquean.", ChatFormatting.GREEN), false);
        return count;
    }

    private static Map<UUID, String> participants(MinecraftServer server, String selectedTeam) {
        Map<UUID, String> roster = new LinkedHashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!eligible(player)) continue;
            PlayerTeam team = server.getScoreboard().getPlayersTeam(player.getScoreboardName());
            if (team != null && TEAMS.contains(team.getName()) && (selectedTeam == null || selectedTeam.equals(team.getName()))) {
                roster.put(player.getUUID(), team.getName());
            }
        }
        return roster;
    }

    private static void queueRoster(Map<UUID, String> roster) {
        beginQueue(true);
        GLOBAL_ROSTER.putAll(roster);
        for (var entry : roster.entrySet()) {
            PENDING.addLast(new PendingTeleport(entry.getKey(), POINTS.get(entry.getValue()), entry.getValue()));
        }
    }

    private static int teleportTeam(CommandSourceStack source, String name) {
        if (preventPostGameTeleport(source)) {
            return 0;
        }
        if (!validTeam(source, name) || !canQueue(source)) {
            return 0;
        }
        Station spot = POINTS.get(name);
        if (!ready(source, spot, name)) {
            return 0;
        }
        beginQueue(false);
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            if (eligible(player) && isMember(source.getServer(), player, name)) {
                PENDING.addLast(new PendingTeleport(player.getUUID(), spot, name));
            }
        }
        int count = PENDING.size();
        source.sendSuccess(() -> KeyMessages.message("Módulo " + name + ": " + count + " teletransportes programados.", ChatFormatting.GREEN), false);
        return count;
    }

    private static boolean isMember(MinecraftServer server, ServerPlayer player, String teamName) {
        PlayerTeam team = server.getScoreboard().getPlayersTeam(player.getScoreboardName());
        return team != null && team.getName().equals(teamName);
    }

    private static boolean ready(CommandSourceStack source, Station station, String label) {
        String problem = destinationProblem(source.getServer(), station);
        if (problem == null) return true;
        source.sendFailure(KeyMessages.message(label.toUpperCase() + ": " + problem, ChatFormatting.RED));
        return false;
    }

    private static String destinationProblem(MinecraftServer server, Station station) {
        if (station == null) return "falta el destino. Usa /nightminigames modulos punto guardar <equipo>.";
        if (!Double.isFinite(station.x()) || !Double.isFinite(station.y()) || !Double.isFinite(station.z())
            || !Float.isFinite(station.yaw()) || !Float.isFinite(station.pitch())) return "el destino contiene coordenadas inválidas; vuelve a guardarlo.";
        ServerLevel level = resolve(server, station);
        if (level == null) return "no existe la dimensión " + station.dimension() + ".";
        BlockPos feet = BlockPos.containing(station.x(), station.y(), station.z());
        if (feet.getY() < level.getMinBuildHeight() || feet.getY() + 1 >= level.getMaxBuildHeight()
            || !level.getWorldBorder().isWithinBounds(feet)) return "destino fuera de los límites del mundo.";
        return null;
    }

    private static ServerLevel resolve(MinecraftServer server, Station point) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(point.dimension())) {
                return level;
            }
        }
        return null;
    }

    private static boolean teleport(ServerPlayer player, Station point) {
        if (destinationProblem(player.server, point) != null) return false;
        ServerLevel level = resolve(player.server, point);
        BlockPos feet = BlockPos.containing(point.x(), point.y(), point.z());
        level.getChunk(feet.getX() >> 4, feet.getZ() >> 4);
        return player.teleportTo(level, point.x(), point.y(), point.z(), Collections.emptySet(), point.yaw(), point.pitch());
    }

    private static List<ItemStack> decodeKit(MinecraftServer server, String name) {
        List<ItemStack> result = new ArrayList<>();
        for (String snbt : KITS.getOrDefault(name, List.of())) {
            try {
                CompoundTag compound = TagParser.parseTag(snbt);
                ItemStack stack = ItemStack.parseOptional(server.registryAccess(), compound);
                if (!stack.isEmpty()) {
                    result.add(stack);
                }
            } catch (Exception exception) {
                NightminigamesMod.LOGGER.warn("[Nightminigames] Kit inválido en módulo {}", name, exception);
            }
        }
        return result;
    }

    private static void grantKit(MinecraftServer server, ServerPlayer player, String team) {
        List<ItemStack> kit = PREPARED_KITS.computeIfAbsent(team, name -> decodeKit(server, name));
        if (kit.isEmpty() || DELIVERED.computeIfAbsent(team, ignored -> new HashSet<>()).contains(player.getUUID())) {
            return;
        }
        for (ItemStack template : kit) {
            ItemStack stack = template.copy();
            if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                player.drop(stack, false);
            }
        }
        DELIVERED.get(team).add(player.getUUID());
    }

    private static void tick(MinecraftServer server) {
        if (server != activeServer || PENDING.isEmpty()) {
            return;
        }
        for (int processed = 0; processed < TELEPORTS_PER_TICK && !PENDING.isEmpty(); processed++) {
            PendingTeleport request = PENDING.removeFirst();
            ServerPlayer player = server.getPlayerList().getPlayer(request.playerId());
            if (player == null || !eligible(player)
                || !request.team().isBlank() && !isMember(server, player, request.team())) {
                skippedTeleports++;
                continue;
            }
            try {
                if (!teleport(player, request.spot())) {
                    skippedTeleports++;
                    notifyOperators(server, "No se pudo teletransportar a " + player.getGameProfile().getName() + " del equipo " + request.team() + ". Revisa su destino con inicio comprobar.", ChatFormatting.RED);
                    continue;
                }
            } catch (RuntimeException exception) {
                skippedTeleports++;
                NightminigamesMod.LOGGER.error("[Nightminigames] Error de teletransporte para {}", player.getGameProfile().getName(), exception);
                notifyOperators(server, "Error al teletransportar a " + player.getGameProfile().getName() + ": " + exception.getMessage(), ChatFormatting.RED);
                continue;
            }
            successfulTeleports++;
            if (!request.team().isBlank()) {
                grantKit(server, player, request.team());
                if (globalTeleporting) {
                    GLOBAL_ARRIVED.add(player.getUUID());
                }
            }
        }
        if (PENDING.isEmpty()) {
            save(server);
            int moved = successfulTeleports;
            int skipped = skippedTeleports;
            boolean wasGlobal = globalTeleporting;
            globalTeleporting = false;
            if (wasGlobal) {
                globalReady = rosterMatches(server);
            }
            notifyOperators(server, "Teletransportes finalizados: " + moved + " enviados, " + skipped + " omitidos."
                + (wasGlobal ? globalReady ? " Los participantes conectados pueden comenzar; los desconectados no bloquean el inicio." : " Inicio bloqueado: ningún participante válido o distribución incompleta." : ""),
                !wasGlobal ? (skipped == 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW) : (globalReady ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
            PREPARED_KITS.clear();
            if (wasGlobal && startAfterTeleport) {
                startAfterTeleport = false;
                if (dev.greto.nightminigames.round.RoundResetSystem.isRearming()) {
                    notifyOperators(server, "Inicio GLOBAL cancelado: todavía se están reactivando estructuras de la ronda.", ChatFormatting.YELLOW);
                } else if (globalReady) {
                    Map<String, String> dimensions = readyEntrances(server);
                    if (dimensions != null) {
                        launchAll(server, dimensions);
                    } else {
                        notifyOperators(server, "Inicio cancelado: falta región o puerta Entrada. Ejecuta /nightminigames inicio comprobar para ver el equipo y lo que falta.", ChatFormatting.RED);
                    }
                } else {
                    notifyOperators(server, "Inicio GLOBAL cancelado: quedan participantes conectados sin distribuir. Repite /nightminigames modulos teleportar todos.", ChatFormatting.RED);
                }
            }
        }
    }

    private static int gatherTeam(CommandSourceStack source, String team) {
        if (!validTeam(source, team) || !canQueue(source)) {
            return 0;
        }
        Station station = POINTS.get("reunion_" + team);
        if (!ready(source, station, "reunion_" + team)) {
            return 0;
        }
        beginQueue(false);
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            if (eligible(player) && isMember(source.getServer(), player, team)) {
                PENDING.addLast(new PendingTeleport(player.getUUID(), station, ""));
            }
        }
        int size = PENDING.size();
        source.sendSuccess(() -> KeyMessages.message(team + ": " + size + " jugadores programados para reunión.", TeamBinding.color(team)), false);
        return size;
    }

    private static int startTeam(CommandSourceStack source, String team) {
        if (!validTeam(source, team)) return 0;
        return startSelected(source, team);
    }

    private static int start(CommandSourceStack source) {
        return startSelected(source, null);
    }

    private static int startSelected(CommandSourceStack source, String selectedTeam) {
        if (preventPostGameTeleport(source)) return 0;
        if (dev.greto.nightminigames.round.RoundResetSystem.isRearming()) {
            source.sendFailure(KeyMessages.message("No se puede iniciar: el reinicio de estructuras sigue en progreso. Espera a que termine.", ChatFormatting.YELLOW));
            return 0;
        }
        if (!canQueue(source)) return 0;
        Map<UUID, String> roster = participants(source.getServer(), selectedTeam);
        if (roster.isEmpty()) {
            source.sendFailure(KeyMessages.message("No se puede iniciar: no hay participantes conectados en Supervivencia o Aventura "
                + (selectedTeam == null ? "asignados a los equipos del minijuego." : "en el equipo " + selectedTeam + ".")
                + " Los equipos incompletos son válidos y los jugadores sin equipo se ignoran.", ChatFormatting.YELLOW));
            return 0;
        }
        boolean valid = true;
        for (String team : new LinkedHashSet<>(roster.values())) {
            List<String> problems = configurationProblems(source.getServer(), team);
            for (String problem : problems) {
                source.sendFailure(KeyMessages.message(team.toUpperCase() + ": " + problem, ChatFormatting.RED));
                valid = false;
            }
        }
        if (!valid) {
            source.sendFailure(KeyMessages.message("Corrige lo indicado. Detalle: /nightminigames inicio comprobar [equipo].", ChatFormatting.YELLOW));
            return 0;
        }
        if (globalStarted && selectedTeam == null) {
            source.sendFailure(KeyMessages.message("La partida ya está iniciada. Usa inicio detener antes de volver a comenzar.", ChatFormatting.YELLOW));
            return 0;
        }
        queueRoster(roster);
        startAfterTeleport = true;
        int count = roster.size();
        source.sendSuccess(() -> KeyMessages.message("Inicio en proceso: " + count + " participantes. Se teletransportan y reciben su kit; después se abren sus entradas. No se exige llenar equipos ni asignar a todos los conectados.", ChatFormatting.GREEN), false);
        return count;
    }

    private static Set<String> activeTeams(MinecraftServer server) {
        return new LinkedHashSet<>(participants(server, null).values());
    }

    private static Map<String, String> readyEntrances(MinecraftServer server) {
        Map<String, String> result = new LinkedHashMap<>();
        Set<String> participating = new LinkedHashSet<>();
        for (var entry : GLOBAL_ROSTER.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null && eligible(player) && isMember(server, player, entry.getValue())) participating.add(entry.getValue());
        }
        for (String team : participating) {
            String dimension = RegionSelectionSystem.dimension(RegionKind.ENTRADA, team);
            WorldStructureJson.knownStructures(server, team);
            if (dimension == null || DoorSystem.loadedDoors(dimension, RegionKind.ENTRADA, team) == 0) return null;
            result.put(team, dimension);
        }
        return result.isEmpty() ? null : result;
    }

    private static boolean rosterMatches(MinecraftServer server) {
        int connected = 0;
        for (var entry : GLOBAL_ROSTER.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !eligible(player) || !isMember(server, player, entry.getValue())) continue;
            connected++;
            Station destination = POINTS.get(entry.getValue());
            if (!GLOBAL_ARRIVED.contains(entry.getKey()) || destination == null
                || !player.serverLevel().dimension().location().toString().equals(destination.dimension())) return false;
        }
        return connected > 0;
    }

    private static void launchAll(MinecraftServer server, Map<String, String> dimensions) {
        DoorSystem.openEntrancesFor(server, dimensions);
        globalStarted = true;
        globalReady = false;
        dev.greto.nightminigames.integration.MapWaypointBridge.begin(server, dimensions.keySet());
        notifyOperators(server, "PARTIDA INICIADA: " + dimensions.size() + " equipo(s) con participantes activos. Los equipos vacíos permanecen cerrados y no requieren kits.", ChatFormatting.GREEN);
    }

    private static void notifyOperators(MinecraftServer server, String message, ChatFormatting color) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.hasPermissions(2)) {
                dev.greto.nightminigames.admin.AdminMessages.send(player, KeyMessages.message(message, color));
            }
        }
        NightminigamesMod.LOGGER.info("[Nightminigames] {}", message);
    }

    private static int stopTeam(CommandSourceStack source, String team) {
        if (!validTeam(source, team)) {
            return 0;
        }
        dev.greto.nightminigames.integration.MapWaypointBridge.stop(source.getServer(), team);
        String dimension = RegionSelectionSystem.dimension(RegionKind.ENTRADA, team);
        if (dimension != null) {
            DoorSystem.clearEntranceOverride(dimension, team);
        }
        source.sendSuccess(() -> KeyMessages.message("Apertura inicial de " + team + " desactivada.", TeamBinding.color(team)), false);
        return 1;
    }

    private static int stop(CommandSourceStack source) {
        dev.greto.nightminigames.integration.MapWaypointBridge.stop(source.getServer(), null);
        startAfterTeleport = false;
        globalStarted = false;
        for (String team : TEAMS) {
            String dimension = RegionSelectionSystem.dimension(RegionKind.ENTRADA, team);
            if (dimension != null) {
                DoorSystem.clearEntranceOverride(dimension, team);
            }
        }
        DoorSystem.refreshEntranceStates(source.getServer());
        source.sendSuccess(() -> KeyMessages.message("Inicio detenido. Las puertas de Entrada se cierran cuando el paso está libre. Las máquinas no se modificaron.", ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int moduleStatus(CommandSourceStack source) {
        int configured = 0;
        int withKits = 0;
        for (String team : TEAMS) {
            if (POINTS.containsKey(team)) {
                configured++;
            }
            if (!KITS.getOrDefault(team, List.of()).isEmpty()) {
                withKits++;
            }
        }
        int destinations = configured;
        int kitsReady = withKits;
        source.sendSuccess(() -> KeyMessages.message("MÓDULOS: " + destinations + "/7 destinos · " + kitsReady + "/7 KITS DE JUGADORES · " + PENDING.size() + " teletransportes pendientes.", ChatFormatting.AQUA), false);
        source.sendSuccess(() -> KeyMessages.message("El KIT DE CONFIGURACIÓN es distinto al KIT DE JUGADORES. Si aparece 0 stacks, guarda los ítems de la barra rápida con /nightminigames modulos kit guardar <equipo>.", ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> KeyMessages.message("Solo se requieren destinos y puertas de equipos con participantes. Detalle: /nightminigames inicio comprobar", ChatFormatting.GRAY), false);
        return 1;
    }

    private static int startStatus(CommandSourceStack source) {
        String phase = dev.greto.nightminigames.machine.TeamExitCompletionSystem.isFinished() ? "FINALIZADA" : globalStarted ? "EN CURSO" : startAfterTeleport ? "INICIO PENDIENTE" : globalTeleporting ? "TELETRANSPORTANDO" : globalReady ? "LISTO" : "SIN INICIAR";
        source.sendSuccess(() -> KeyMessages.message("PARTIDA: " + phase + " | Equipos activos: " + activeTeams(source.getServer()).size() + "/7 | Distribuidos inicialmente " + GLOBAL_ARRIVED.size() + "/" + GLOBAL_ROSTER.size() + " | Pendientes " + PENDING.size() + ".", ChatFormatting.AQUA), false);
        source.sendSuccess(() -> KeyMessages.message("Para el detalle completo: /nightminigames inicio comprobar", ChatFormatting.GRAY), false);
        return 1;
    }

    public static void finishAfterQualification(MinecraftServer server) {
        dev.greto.nightminigames.integration.MapWaypointBridge.stop(server, null);
        PENDING.clear();
        PREPARED_KITS.clear();
        globalTeleporting = false;
        globalReady = false;
        startAfterTeleport = false;
        globalStarted = false;
        for (String team : TEAMS) {
            String dimension = RegionSelectionSystem.dimension(RegionKind.ENTRADA, team);
            if (dimension != null) {
                DoorSystem.clearEntranceOverride(dimension, team);
            }
        }
        DoorSystem.refreshEntranceStates(server);
        notifyOperators(server, "Minijuego detenido. Nadie puede volver a comenzar hasta reiniciar.", ChatFormatting.YELLOW);
    }

    private static boolean preventPostGameTeleport(CommandSourceStack source) {
        if (!dev.greto.nightminigames.machine.TeamExitCompletionSystem.isFinished()) {
            return false;
        }
        source.sendFailure(KeyMessages.message("La ronda terminó. Usa /nightminigames inicio reiniciar antes de enviar jugadores.", ChatFormatting.RED));
        return true;
    }

    private static int resetRound(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        PENDING.clear();
        PREPARED_KITS.clear();
        GLOBAL_ROSTER.clear();
        GLOBAL_ARRIVED.clear();
        DELIVERED.clear();
        successfulTeleports = 0;
        skippedTeleports = 0;
        globalTeleporting = false;
        globalReady = false;
        globalStarted = false;
        startAfterTeleport = false;
        save(server);
        dev.greto.nightminigames.integration.MapWaypointBridge.stop(server, null);
        DoorSystem.resetRound(server);
        dev.greto.nightminigames.machine.TeamExitCompletionSystem.resetRound(server);
        dev.greto.nightminigames.feed.HudFeedSystem.clear(server);
        dev.greto.nightminigames.feed.ClassificationFeedSystem.clearForRound(server);
        int pending = dev.greto.nightminigames.round.RoundResetSystem.beginNewRound(server);
        source.sendSuccess(() -> KeyMessages.message("Ronda reiniciada: las puertas de Entrada están bloqueadas hasta comenzar. Clasificaciones y entregas de kits reiniciadas. "
            + pending + " estructuras cargadas pendientes de reactivar; las descargadas se reactivarán al cargar. Regiones, módulos, equipos y kits conservados.", ChatFormatting.GREEN), true);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        int queued = PENDING.size();
        source.sendSuccess(() -> KeyMessages.message("Flujo GLOBAL: " + (globalStarted ? "PARTIDA INICIADA" : startAfterTeleport ? "INICIO PENDIENTE" : globalTeleporting ? "DISTRIBUYENDO" : globalReady ? "LISTO PARA COMENZAR" : "SIN DISTRIBUCIÓN COMPLETA")
            + " | Jugadores enviados " + GLOBAL_ARRIVED.size() + "/" + GLOBAL_ROSTER.size() + " | pendientes " + queued + ".", ChatFormatting.AQUA), false);
        Map<String, String> dimensions = readyEntrances(source.getServer());
        source.sendSuccess(() -> KeyMessages.message("Puertas de Entrada: " + (dimensions == null ? "FALTA CONFIGURAR UN EQUIPO ACTIVO O NO HAY JUGADORES" : dimensions.size() + "/" + activeTeams(source.getServer()).size() + " EQUIPOS ACTIVOS LISTOS"), dimensions == null ? ChatFormatting.YELLOW : ChatFormatting.GREEN), false);
        for (String name : TEAMS) {
            Station point = POINTS.get(name);
            String target = point == null ? "SIN DESTINO" : point.dimension() + " [" + (int) point.x() + ", " + (int) point.y() + ", " + (int) point.z() + "]";
            source.sendSuccess(() -> KeyMessages.message("DESTINO " + name.toUpperCase() + ": " + target, point == null ? ChatFormatting.YELLOW : ChatFormatting.AQUA), false);
        }
        for (String name : TEAMS) {
            PlayerTeam team = source.getServer().getScoreboard().getPlayerTeam(name);
            int members = team == null ? 0 : team.getPlayers().size();
            int capacity = TeamCapacitySystem.capacity(name);
            int kit = KITS.getOrDefault(name, List.of()).size();
            source.sendSuccess(() -> KeyMessages.message("Módulo " + name + ": " + members + "/" + capacity + " cupos, kit " + kit + " stacks", members >= capacity ? ChatFormatting.YELLOW : ChatFormatting.AQUA), false);
        }
        return 1;
    }

    private static List<String> configurationProblems(MinecraftServer server, String team) {
        List<String> problems = new ArrayList<>();
        String destination = destinationProblem(server, POINTS.get(team));
        if (destination != null) problems.add(destination);
        String entrance = RegionSelectionSystem.dimension(RegionKind.ENTRADA, team);
        if (entrance == null) problems.add("falta completar la región de ENTRADA con su varita del kit de configuración.");
        WorldStructureJson.knownStructures(server, team);
        if (entrance != null && DoorSystem.loadedDoors(entrance, RegionKind.ENTRADA, team) == 0) {
            problems.add("falta una puerta de ENTRADA vinculada al equipo en " + entrance + ". Colócala con el kit de configuración.");
        }
        return problems;
    }

    private static int checkConfiguration(CommandSourceStack source, String selectedTeam) {
        if (selectedTeam != null && !validTeam(source, selectedTeam)) return 0;
        MinecraftServer server = source.getServer();
        Map<UUID, String> roster = participants(server, null);
        int ready = 0;
        for (String team : TEAMS) {
            if (selectedTeam != null && !selectedTeam.equals(team)) continue;
            long count = roster.values().stream().filter(team::equals).count();
            List<String> problems = configurationProblems(server, team);
            List<WorldStructureJson.KnownStructure> structures = WorldStructureJson.knownStructures(server, team);
            List<String> completion = new ArrayList<>();
            String exit = RegionSelectionSystem.dimension(RegionKind.SALIDA, team);
            if (exit == null) completion.add("falta la región de SALIDA");
            if (exit != null && DoorSystem.loadedDoors(exit, RegionKind.SALIDA, team) == 0) completion.add("falta puerta de SALIDA en " + exit);
            if (structures.stream().noneMatch(s -> s.type().equals("maquina"))) completion.add("falta máquina vinculada al equipo");
            for (dev.greto.nightminigames.key.KeyColor color : dev.greto.nightminigames.key.KeyColor.values()) {
                if (structures.stream().noneMatch(s -> s.type().equals("llave_" + color.id))) completion.add("falta spawner de llave " + color.display);
            }
            String label = team.toUpperCase() + " | " + count + " participantes | "
                + (problems.isEmpty() ? "LISTO PARA INICIAR" : "FALTA CONFIGURACIÓN DE INICIO")
                + (count == 0 ? " | INACTIVO: no bloquea a otros equipos" : "");
            source.sendSuccess(() -> KeyMessages.message(label, problems.isEmpty() ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
            for (String problem : problems) source.sendSuccess(() -> KeyMessages.message("  Inicio: " + problem, ChatFormatting.RED), false);
            for (String problem : completion) source.sendSuccess(() -> KeyMessages.message("  Recorrido: " + problem + ". No impide abrir la entrada, pero debes configurarlo para jugar el recorrido completo.", ChatFormatting.YELLOW), false);
            if (problems.isEmpty() && completion.isEmpty()) {
                source.sendSuccess(() -> KeyMessages.message("  Configuración del recorrido completa: entrada, salida, máquina y cuatro colores de llave.", ChatFormatting.GREEN), false);
            }
            if (KITS.getOrDefault(team, List.of()).isEmpty()) source.sendSuccess(() -> KeyMessages.message("  Kit de jugadores vacío (opcional). El kit de configuración entrega herramientas; no guarda el kit de jugadores.", ChatFormatting.GRAY), false);
            if (problems.isEmpty() && count > 0) ready++;
        }
        int readyCount = ready;
        source.sendSuccess(() -> KeyMessages.message("Equipos activos listos para iniciar: " + readyCount + ". Se admiten equipos incompletos y un solo equipo. Jugadores sin equipo: ignorados.", ChatFormatting.AQUA), false);
        if (dev.greto.nightminigames.machine.TeamExitCompletionSystem.isFinished()) source.sendFailure(KeyMessages.message("La ronda terminó: ejecuta /nightminigames inicio reiniciar.", ChatFormatting.YELLOW));
        if (dev.greto.nightminigames.round.RoundResetSystem.isRearming()) source.sendFailure(KeyMessages.message("Reinicio de estructuras en progreso: espera a que termine antes de iniciar.", ChatFormatting.YELLOW));
        return ready;
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data/nightminigames_modulos.json");
    }

    private static void load(MinecraftServer server) {
        POINTS.clear();
        KITS.clear();
        DELIVERED.clear();
        Path file = file(server);
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("points") && root.get("points").isJsonObject()) {
                for (var entry : root.getAsJsonObject("points").entrySet()) {
                    if (DESTINATIONS.contains(entry.getKey())) {
                        JsonObject point = entry.getValue().getAsJsonObject();
                        POINTS.put(entry.getKey(), new Station(point.get("dimension").getAsString(), point.get("x").getAsDouble(), point.get("y").getAsDouble(), point.get("z").getAsDouble(), point.get("yaw").getAsFloat(), point.get("pitch").getAsFloat()));
                    }
                }
            }
            if (root.has("kits") && root.get("kits").isJsonObject()) {
                for (var entry : root.getAsJsonObject("kits").entrySet()) {
                    if (TEAMS.contains(entry.getKey())) {
                        List<String> kit = new ArrayList<>();
                        for (var value : entry.getValue().getAsJsonArray()) {
                            kit.add(value.getAsString());
                        }
                        KITS.put(entry.getKey(), kit);
                    }
                }
            }
            if (root.has("delivered") && root.get("delivered").isJsonObject()) {
                for (var entry : root.getAsJsonObject("delivered").entrySet()) {
                    if (TEAMS.contains(entry.getKey())) {
                        Set<UUID> members = new HashSet<>();
                        for (var value : entry.getValue().getAsJsonArray()) {
                            members.add(UUID.fromString(value.getAsString()));
                        }
                        DELIVERED.put(entry.getKey(), members);
                    }
                }
            }
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] Error al leer los módulos", exception);
        }
    }

    private static void save(MinecraftServer server) {
        JsonObject root = new JsonObject();
        JsonObject points = new JsonObject();
        for (var entry : POINTS.entrySet()) {
            Station station = entry.getValue();
            JsonObject point = new JsonObject();
            point.addProperty("dimension", station.dimension());
            point.addProperty("x", station.x());
            point.addProperty("y", station.y());
            point.addProperty("z", station.z());
            point.addProperty("yaw", station.yaw());
            point.addProperty("pitch", station.pitch());
            points.add(entry.getKey(), point);
        }
        root.add("points", points);
        JsonObject kits = new JsonObject();
        for (var entry : KITS.entrySet()) {
            JsonArray items = new JsonArray();
            for (String snbt : entry.getValue()) {
                items.add(snbt);
            }
            kits.add(entry.getKey(), items);
        }
        root.add("kits", kits);
        JsonObject delivered = new JsonObject();
        for (var entry : DELIVERED.entrySet()) {
            JsonArray members = new JsonArray();
            for (UUID id : entry.getValue()) {
                members.add(id.toString());
            }
            delivered.add(entry.getKey(), members);
        }
        root.add("delivered", delivered);
        Path file = file(server);
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] Error al guardar los módulos", exception);
        }
    }

    private record PendingTeleport(UUID playerId, Station spot, String team) {
    }

    private record Station(String dimension, double x, double y, double z, float yaw, float pitch) {
    }
}
