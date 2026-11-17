package dev.greto.nightminigames.integration;

import com.google.gson.*;
import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.environment.EnvironmentSystem;
import dev.greto.nightminigames.key.KeyColor;
import dev.greto.nightminigames.machine.TeamExitCompletionSystem;
import dev.greto.nightminigames.module.ModuleSystem;
import dev.greto.nightminigames.persistence.WorldStructureJson;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Event-driven bridge. Only changed team key waypoints are sent to MapaMystrix. */
public final class MapWaypointBridge {
    private static final Set<String> ACTIVE_TEAMS = new LinkedHashSet<>();
    private static final Set<String> MACHINE_TEAMS = new LinkedHashSet<>();
    private static final Map<UUID, String> PREVIEWS = new HashMap<>();
    private static final Map<String, Set<UUID>> GROUPS = new HashMap<>();
    private static MinecraftServer currentServer;
    private static boolean initialized;
    private static boolean dirty;
    private static boolean iconsReady;
    private static boolean startupPending;
    private static UUID stateRound;

    private MapWaypointBridge() {}

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            currentServer = server;
            GROUPS.clear();
            MACHINE_TEAMS.clear();
            PREVIEWS.clear();
            iconsReady = false;
            ACTIVE_TEAMS.clear();
            startupPending = true;
            dirty = true; // The first end tick runs after every mod's SERVER_STARTED callback.
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            currentServer = null;
            ACTIVE_TEAMS.clear();
            MACHINE_TEAMS.clear();
            GROUPS.clear();
            dirty = false;
            iconsReady = false;
            stateRound = null;
            startupPending = false;
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> refreshGroups(server));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PREVIEWS.remove(handler.player.getUUID());
            server.execute(() -> refreshGroups(server));
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server == currentServer && startupPending) {
                load(server);
                startupPending = false;
            }
            if (dirty && server == currentServer) synchronize(server);
        });
    }

    public static boolean available() { return FabricLoader.getInstance().isModLoaded("mapmitryx"); }

    public static void requestRefresh(MinecraftServer server) {
        if (currentServer == server) dirty = true;
    }

    public static void begin(MinecraftServer server, Collection<String> teams) {
        PREVIEWS.clear();
        if (!Objects.equals(stateRound, TeamExitCompletionSystem.roundId())) ACTIVE_TEAMS.clear();
        stateRound = TeamExitCompletionSystem.roundId();
        teams.stream().filter(ModuleSystem.TEAMS::contains).forEach(ACTIVE_TEAMS::add);
        save(server);
        requestRefresh(server);
    }

    public static void stop(MinecraftServer server, String team) {
        PREVIEWS.clear();
        if (team == null) ACTIVE_TEAMS.clear(); else ACTIVE_TEAMS.remove(team);
        stateRound = TeamExitCompletionSystem.roundId();
        save(server);
        requestRefresh(server);
    }

    public static boolean preview(ServerPlayer player, String team) {
        if (!player.hasPermissions(2) || !available()) return false;
        if (team == null) PREVIEWS.remove(player.getUUID());
        else {
            if (!ACTIVE_TEAMS.isEmpty() || TeamExitCompletionSystem.isFinished()) return false;
            if (!team.equals("todos") && !ModuleSystem.TEAMS.contains(team)) return false;
            PREVIEWS.put(player.getUUID(), team);
        }
        requestRefresh(player.server);
        return synchronize(player.server);
    }

    public static boolean canPreview() {
        return ACTIVE_TEAMS.isEmpty() && !TeamExitCompletionSystem.isFinished();
    }

    public static boolean machineWaypointsEnabled(String team) { return MACHINE_TEAMS.contains(team); }

    public static void setMachineWaypoints(MinecraftServer server, boolean enabled) {
        MACHINE_TEAMS.clear();
        if (enabled) MACHINE_TEAMS.addAll(ModuleSystem.TEAMS);
        stateRound = TeamExitCompletionSystem.roundId();
        save(server);
        requestRefresh(server);
    }

    public static boolean visibleFor(String team) {
        return ACTIVE_TEAMS.contains(team) && !EnvironmentSystem.isDarkness()
            && !TeamExitCompletionSystem.isFinished() && !TeamExitCompletionSystem.isQualified(team);
    }

    public static String status(String team) {
        if (!available()) return "MapaMystrix no instalado";
        if (TeamExitCompletionSystem.isFinished()) return "DETENIDOS: cupo de clasificación completado";
        if (!ACTIVE_TEAMS.contains(team)) return "OCULTOS: equipo sin iniciar o detenido";
        if (TeamExitCompletionSystem.isQualified(team)) return "OCULTOS: equipo ya clasificado";
        return EnvironmentSystem.isDarkness() ? "OCULTOS: luces apagadas" : "VISIBLES: luces encendidas";
    }

    public static void refreshGroups(MinecraftServer server) {
        requestRefresh(server);
        if (!available()) return;
        try { synchronizeGroups(server); }
        catch (ReflectiveOperationException | RuntimeException error) { log(error); }
    }

    private static void synchronizeGroups(MinecraftServer server) throws ReflectiveOperationException {
        Method replace = Class.forName("com.mapmitryx.api.MytrixWaypointGroups")
            .getMethod("replaceOnlineMembers", MinecraftServer.class, String.class, Collection.class);
        for (String team : ModuleSystem.TEAMS) {
            Set<UUID> members = new LinkedHashSet<>();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                var actual = server.getScoreboard().getPlayersTeam(player.getScoreboardName());
                if (actual != null && team.equals(actual.getName())) members.add(player.getUUID());
            }
            if (!members.equals(GROUPS.get(team))) {
                replace.invoke(null, server, "nm_" + team, members);
                GROUPS.put(team, Set.copyOf(members));
            }
        }
    }

    /** Legacy location command cannot bypass round or light visibility rules. */
    public static boolean publish(MinecraftServer server, String team, List<WorldStructureJson.KnownKey> ignored) {
        requestRefresh(server);
        return synchronize(server);
    }

    public static boolean synchronize(MinecraftServer server) {
        dirty = false;
        if (!available()) return false;
        try {
            installIcons();
            synchronizeGroups(server);
            Class<?> api = Class.forName("com.mapmitryx.api.MytrixWaypoints");
            Class<?> builderType = Class.forName("com.mapmitryx.api.MytrixWaypoints$Builder");
            Method builder = api.getMethod("builder", String.class, String.class, int.class, int.class, int.class);
            Map<String, Object> previous = new HashMap<>();
            for (Object marker : (List<?>) api.getMethod("snapshot").invoke(null)) {
                String id = (String) marker.getClass().getMethod("id").invoke(marker);
                if (id.startsWith("nmkey_") || id.startsWith("nmpreview_") || id.startsWith("nmm_") || id.startsWith("nmme_")) previous.put(id, marker);
            }
            Method upsert = api.getMethod("upsert", MinecraftServer.class, Class.forName("com.mapmitryx.waypoint.MytrixWaypoint"));
            for (String team : ModuleSystem.TEAMS) {
                if (!visibleFor(team)) continue;
                for (WorldStructureJson.KnownKey key : WorldStructureJson.knownKeys(server, team)) {
                    if (key.collected()) continue;
                    String id = "nmkey_" + team + "_" + UUID.nameUUIDFromBytes((key.dimension() + ":" + key.pos().asLong() + ":" + key.color())
                        .getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
                    Object point = builder.invoke(null, id, key.dimension(), key.pos().getX(), key.pos().getY(), key.pos().getZ());
                    String label = key.color();
                    for (KeyColor color : KeyColor.values()) if (color.id.equals(key.color())) label = color.display;
                    builderType.getMethod("name", String.class).invoke(point, "LLAVE " + label.toUpperCase(Locale.ROOT) + " · " + team.toUpperCase(Locale.ROOT));
                    builderType.getMethod("image", String.class).invoke(point, "nightminigames/key_" + key.color());
                    builderType.getMethod("size", int.class).invoke(point, 32);
                    builderType.getMethod("category", String.class).invoke(point, "nightminigames");
                    builderType.getMethod("color", int.class).invoke(point, 0xFFFFFFFF); // Preserve the authored PNG colors.
                    builderType.getMethod("group", String.class).invoke(point, "nm_" + team);
                    Object waypoint = builderType.getMethod("build").invoke(point);
                    if (!waypoint.equals(previous.remove(id))) upsert.invoke(null, server, waypoint);
                }
            }
            for (String team : MACHINE_TEAMS) {
                if (!visibleFor(team)) continue;
                for (var machine : WorldStructureJson.knownMachines(server, team)) {
                    Object point = machineWaypoint(builder, builderType, team, machine, null);
                    String id = (String) point.getClass().getMethod("id").invoke(point);
                    if (!point.equals(previous.remove(id))) upsert.invoke(null, server, point);
                }
            }
            if (!canPreview()) PREVIEWS.clear();
            for (var entry : new ArrayList<>(PREVIEWS.entrySet())) {
                ServerPlayer viewer = server.getPlayerList().getPlayer(entry.getKey());
                if (viewer == null || !viewer.hasPermissions(2)) {
                    PREVIEWS.remove(entry.getKey());
                    continue;
                }
                for (String team : ModuleSystem.TEAMS) {
                    if (!entry.getValue().equals("todos") && !entry.getValue().equals(team)) continue;
                    for (WorldStructureJson.KnownKey key : WorldStructureJson.knownKeys(server, team)) {
                        String id = "nmpreview_" + UUID.nameUUIDFromBytes((entry.getKey() + ":" + team + ":" + key.dimension() + ":" + key.pos().asLong() + ":" + key.color())
                            .getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
                        Object point = builder.invoke(null, id, key.dimension(), key.pos().getX(), key.pos().getY(), key.pos().getZ());
                        String label = key.color();
                        for (KeyColor color : KeyColor.values()) if (color.id.equals(key.color())) label = color.display;
                        builderType.getMethod("name", String.class).invoke(point, "EVAL · " + team.toUpperCase(Locale.ROOT) + " · LLAVE " + label.toUpperCase(Locale.ROOT)
                            + (key.collected() ? " · RECOGIDA" : " · DISPONIBLE"));
                        builderType.getMethod("image", String.class).invoke(point, "nightminigames/key_" + key.color());
                        builderType.getMethod("size", int.class).invoke(point, 32);
                        builderType.getMethod("category", String.class).invoke(point, "nightminigames_eval");
                        builderType.getMethod("color", int.class).invoke(point, 0xFFFFFFFF);
                        builderType.getMethod("personal", UUID.class).invoke(point, entry.getKey());
                        Object waypoint = builderType.getMethod("build").invoke(point);
                        if (!waypoint.equals(previous.remove(id))) upsert.invoke(null, server, waypoint);
                    }
                }
            }
            for (var entry : PREVIEWS.entrySet()) {
                for (String team : MACHINE_TEAMS) {
                    if (!entry.getValue().equals("todos") && !entry.getValue().equals(team)) continue;
                    for (var machine : WorldStructureJson.knownMachines(server, team)) {
                        Object point = machineWaypoint(builder, builderType, team, machine, entry.getKey());
                        String id = (String) point.getClass().getMethod("id").invoke(point);
                        if (!point.equals(previous.remove(id))) upsert.invoke(null, server, point);
                    }
                }
            }
            Method remove = api.getMethod("remove", MinecraftServer.class, String.class);
            for (String id : previous.keySet()) remove.invoke(null, server, id);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | java.io.IOException error) {
            log(error);
            return false;
        }
    }

    private static Object machineWaypoint(Method builder, Class<?> type, String team, WorldStructureJson.KnownStructure machine, UUID viewer) throws ReflectiveOperationException {
        String id = (viewer == null ? "nmm_" : "nmme_") + UUID.nameUUIDFromBytes((viewer + ":" + team + ":" + machine.dimension() + ":" + machine.pos().asLong())
            .getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
        Object point = builder.invoke(null, id, machine.dimension(), machine.pos().getX(), machine.pos().getY(), machine.pos().getZ());
        type.getMethod("name", String.class).invoke(point, (viewer == null ? "MÁQUINA · " : "EVAL · MÁQUINA · ") + team.toUpperCase(Locale.ROOT));
        type.getMethod("image", String.class).invoke(point, "factory");
        type.getMethod("size", int.class).invoke(point, 32);
        type.getMethod("category", String.class).invoke(point, "nightminigames_machine");
        type.getMethod("color", int.class).invoke(point, 0xFFFFFFFF);
        if (viewer == null) type.getMethod("group", String.class).invoke(point, "nm_" + team);
        else type.getMethod("personal", UUID.class).invoke(point, viewer);
        return type.getMethod("build").invoke(point);
    }

    private static void installIcons() throws ReflectiveOperationException, java.io.IOException {
        if (iconsReady) return;
        Path root = (Path) Class.forName("com.mapmitryx.waypoint.server.MytrixWaypointConfig").getMethod("imagesDirectory").invoke(null);
        Files.createDirectories(root.resolve("nightminigames"));
        for (KeyColor color : KeyColor.values()) {
            Path target = root.resolve("nightminigames/key_" + color.id + ".png");
            if (Files.exists(target)) continue; // Keep operator-customized icons.
            String resource = "/assets/nightminigames/textures/waypoints/key_" + color.id + ".png";
            try (var input = MapWaypointBridge.class.getResourceAsStream(resource)) {
                if (input == null) throw new java.io.IOException("Falta el icono " + resource);
                Files.copy(input, target);
            }
        }
        Class.forName("com.mapmitryx.asset.server.MytrixAssetStore").getMethod("reload").invoke(null);
        iconsReady = true;
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data/nightminigames_waypoints.json");
    }

    private static void load(MinecraftServer server) {
        ACTIVE_TEAMS.clear();
        MACHINE_TEAMS.clear();
        stateRound = TeamExitCompletionSystem.roundId();
        if (!Files.isRegularFile(file(server))) return;
        try {
            JsonObject data = JsonParser.parseString(Files.readString(file(server))).getAsJsonObject();
            if (data.has("machineTeams") && data.get("machineTeams").isJsonArray()) {
                for (var value : data.getAsJsonArray("machineTeams")) {
                    String team = value.getAsString();
                    if (ModuleSystem.TEAMS.contains(team)) MACHINE_TEAMS.add(team);
                }
            }
            if (!stateRound.toString().equals(data.get("round").getAsString()) || TeamExitCompletionSystem.isFinished()) return;
            for (var value : data.getAsJsonArray("teams")) {
                String team = value.getAsString();
                if (ModuleSystem.TEAMS.contains(team)) ACTIVE_TEAMS.add(team);
            }
        } catch (Exception error) { NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo leer el estado de waypoints", error); }
    }

    private static void save(MinecraftServer server) {
        try {
            JsonObject data = new JsonObject();
            data.addProperty("round", stateRound.toString());
            JsonArray teams = new JsonArray(); ACTIVE_TEAMS.forEach(teams::add); data.add("teams", teams);
            JsonArray machines = new JsonArray(); MACHINE_TEAMS.forEach(machines::add); data.add("machineTeams", machines);
            Path file = file(server), temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.createDirectories(file.getParent()); Files.writeString(temp, data.toString());
            try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (Exception error) { NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo guardar el estado de waypoints", error); }
    }

    private static void log(Throwable error) {
        Throwable cause = error instanceof InvocationTargetException target ? target.getTargetException() : error;
        NightminigamesMod.LOGGER.error("[Nightminigames] Error sincronizando waypoints de llaves con MapaMystrix", cause);
    }
}
