package dev.greto.nightminigames.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.admin.AdminMessages;
import dev.greto.nightminigames.door.DoorBlock;
import dev.greto.nightminigames.door.DoorBlockEntity;
import dev.greto.nightminigames.flashlight.FlashlightSpawnBlock;
import dev.greto.nightminigames.flashlight.FlashlightSpawnBlockEntity;
import dev.greto.nightminigames.key.KeyColor;
import dev.greto.nightminigames.key.KeySpawnBlock;
import dev.greto.nightminigames.key.KeySpawnBlockEntity;
import dev.greto.nightminigames.machine.MachineBlock;
import dev.greto.nightminigames.machine.MachineBlockEntity;
import dev.greto.nightminigames.module.ModuleSystem;
import dev.greto.nightminigames.flashlight.FlashlightSettingsSystem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.PlayerTeam;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorldStructureJson {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<StructureId, JsonObject> RECORDS = new LinkedHashMap<>();
    private static final Map<StructureId, BlockEntity> LOADED = new LinkedHashMap<>();
    private static final Set<StructureId> PENDING_CHANGES = new HashSet<>();
    private static MinecraftServer activeServer;
    private static boolean initialized;
    private static boolean dirty;
    private static boolean readOnly;
    private static String lastRoster = "";
    private static String lastDimensions = "";

    private WorldStructureJson() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            activeServer = server;
            RECORDS.clear();
            LOADED.clear();
            PENDING_CHANGES.clear();
            dirty = false;
            readOnly = false;
            lastRoster = "";
            lastDimensions = "";
            load(server);
        });
        ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> savePending(server));
        ServerLifecycleEvents.SERVER_STOPPING.register(WorldStructureJson::savePending);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            RECORDS.clear();
            LOADED.clear();
            PENDING_CHANGES.clear();
            activeServer = null;
            dirty = false;
            readOnly = false;
            lastRoster = "";
            lastDimensions = "";
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((entity, level) -> {
            if (isTracked(entity)) {
                StructureId id = key(level, entity.getBlockPos());
                LOADED.put(id, entity);
                PENDING_CHANGES.add(id);
                dev.greto.nightminigames.integration.MapWaypointBridge.requestRefresh(level.getServer());
            }
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((entity, level) -> {
            if (isTracked(entity)) {
                StructureId id = key(level, entity.getBlockPos());
                update(id, entity);
                LOADED.remove(id);
                PENDING_CHANGES.remove(id);
            }
        });
    }

    public static LiteralArgumentBuilder<CommandSourceStack> commands() {
        return Commands.literal("persistencia")
            .then(Commands.literal("estado").executes(context -> report(context.getSource())))
            .then(Commands.literal("dimensiones").executes(context -> listDimensions(context.getSource())))
            .then(Commands.literal("linterna")
                .then(Commands.literal("recargar").executes(context -> reloadLight(context.getSource()))));
    }

    private static int report(CommandSourceStack source) {
        ensure(source.getServer());
        refreshPending();
        Map<String, Integer> counts = new HashMap<>();
        for (StructureId id : RECORDS.keySet()) {
            counts.merge(id.dimension(), 1, Integer::sum);
        }
        line(source, "JSON: " + RECORDS.size() + " estructuras registradas, " + LOADED.size() + " cargadas. "
            + counts.size() + " dimensiones con estructuras.", ChatFormatting.AQUA);
        line(source, "Modo de archivos: " + (readOnly ? "SOLO LECTURA (JSON dañado)" : "normal")
            + " | cambios pendientes: " + (dirty || !PENDING_CHANGES.isEmpty()), readOnly ? ChatFormatting.RED : ChatFormatting.GREEN);
        line(source, "Guardado por cambios: durante el guardado de Minecraft y al detener el servidor.", ChatFormatting.GREEN);
        line(source, "Datos independientes: módulos, kits, regiones, cupos, clasificación, rondas, puertas, puntos y HUD.", ChatFormatting.GRAY);
        return 1;
    }

    public static void markRosterChanged(MinecraftServer server) {
        if (activeServer == server) {
            dirty = true;
        }
    }

    public static void markStructureChanged(BlockEntity entity) {
        if (entity == null || entity.isRemoved() || !isTracked(entity) || !(entity.getLevel() instanceof ServerLevel level)
            || level.getServer() != activeServer) {
            return;
        }
        StructureId id = key(level, entity.getBlockPos());
        LOADED.put(id, entity);
        PENDING_CHANGES.add(id);
        dev.greto.nightminigames.integration.MapWaypointBridge.requestRefresh(level.getServer());
    }

    public static void markBlockStateChanged(ServerLevel level, BlockPos pos) {
        if (level.getServer() == activeServer && level.hasChunkAt(pos)) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (isTracked(entity)) {
                markStructureChanged(entity);
            }
        }
    }

    private static int listDimensions(CommandSourceStack source) {
        ensure(source.getServer());
        refreshPending();
        ArrayList<String> names = new ArrayList<>();
        for (ServerLevel level : source.getServer().getAllLevels()) {
            names.add(level.dimension().location().toString());
        }
        Collections.sort(names);
        line(source, "Dimensiones activas del servidor Fabric: " + names.size(), ChatFormatting.AQUA);
        for (String name : names) {
            int structures = 0;
            for (StructureId id : RECORDS.keySet()) {
                if (id.dimension().equals(name)) {
                    structures++;
                }
            }
            line(source, name + " · " + structures + " estructuras conocidas", ChatFormatting.WHITE);
        }
        return names.size();
    }

    private static int reloadLight(CommandSourceStack source) {
        boolean loaded = FlashlightSettingsSystem.reload(source.getServer());
        line(source, loaded ? "Perfil de iluminación JSON recargado y sincronizado con los clientes."
            : "No se pudo recargar el perfil de linterna.", loaded ? ChatFormatting.GREEN : ChatFormatting.RED);
        return loaded ? 1 : 0;
    }

    private static void line(CommandSourceStack source, String message, ChatFormatting color) {
        Component text = Component.literal("[OP] ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal("[Nightminigames] ").withStyle(ChatFormatting.AQUA))
            .append(Component.literal(message).withStyle(color));
        if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            AdminMessages.send(player, text);
        } else {
            source.sendSuccess(() -> text, false);
        }
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        if (level.getServer() != activeServer) {
            return;
        }
        StructureId id = key(level, pos);
        LOADED.remove(id);
        PENDING_CHANGES.remove(id);
        if (RECORDS.remove(id) != null) {
            dirty = true;
            dev.greto.nightminigames.integration.MapWaypointBridge.requestRefresh(level.getServer());
        }
    }

    private static void savePending(MinecraftServer server) {
        if (server != activeServer) {
            return;
        }
        refreshPending();
        String roster = GSON.toJson(roster(server));
        if (!roster.equals(lastRoster)) {
            dirty = true;
        }
        String dimensions = GSON.toJson(loadedDimensionNames(server));
        if (!dimensions.equals(lastDimensions)) {
            dirty = true;
        }
        persist(server, false);
    }

    private static void refreshPending() {
        if (PENDING_CHANGES.isEmpty()) {
            return;
        }
        for (StructureId id : new ArrayList<>(PENDING_CHANGES)) {
            BlockEntity entity = LOADED.get(id);
            if (entity == null || entity.isRemoved() || !(entity.getLevel() instanceof ServerLevel level)) {
                LOADED.remove(id);
                continue;
            }
            if (level.hasChunkAt(entity.getBlockPos())) {
                update(id, entity);
            }
        }
        PENDING_CHANGES.clear();
    }

    private static void update(StructureId id, BlockEntity entity) {
        if (!(entity.getLevel() instanceof ServerLevel level)) {
            return;
        }
        JsonObject state = snapshot(level, entity);
        if (state == null) {
            return;
        }
        JsonObject old = RECORDS.get(id);
        if (!state.equals(old)) {
            RECORDS.put(id, state);
            dirty = true;
            dev.greto.nightminigames.integration.MapWaypointBridge.requestRefresh(level.getServer());
        }
    }

    private static JsonObject snapshot(ServerLevel level, BlockEntity entity) {
        BlockPos pos = entity.getBlockPos();
        BlockState block = level.hasChunkAt(pos) ? level.getBlockState(pos) : entity.getBlockState();
        JsonObject data = new JsonObject();
        data.addProperty("dimension", level.dimension().location().toString());
        data.addProperty("x", pos.getX());
        data.addProperty("y", pos.getY());
        data.addProperty("z", pos.getZ());
        if (entity instanceof DoorBlockEntity door && block.getBlock() instanceof DoorBlock type) {
            data.addProperty("type", "puerta_" + type.kind().id);
            data.addProperty("team", door.ownerTeam());
            data.addProperty("open", block.getValue(DoorBlock.OPEN));
            data.addProperty("facing", block.getValue(DoorBlock.FACING).getName());
        } else if (entity instanceof MachineBlockEntity machine && block.getBlock() instanceof MachineBlock) {
            data.addProperty("type", "maquina");
            data.addProperty("team", machine.getOwnerTeam());
            data.addProperty("mask", block.getValue(MachineBlock.MASK));
            data.addProperty("roundGeneration", machine.roundGeneration());
            JsonObject inserted = new JsonObject();
            for (KeyColor color : KeyColor.values()) {
                inserted.addProperty(color.id, machine.getInserter(color));
            }
            data.add("insertedBy", inserted);
        } else if (entity instanceof KeySpawnBlockEntity key && block.getBlock() instanceof KeySpawnBlock spawner) {
            data.addProperty("type", "llave_" + spawner.color.id);
            data.addProperty("team", key.ownerTeam());
            data.addProperty("collected", block.getValue(KeySpawnBlock.COLLECTED));
            data.addProperty("roundGeneration", key.roundGeneration());
            data.addProperty("spawnerGeneration", key.spawnerGeneration());
        } else if (entity instanceof FlashlightSpawnBlockEntity flashlight && block.getBlock() instanceof FlashlightSpawnBlock) {
            data.addProperty("type", "linterna");
            data.addProperty("team", flashlight.ownerTeam());
            data.addProperty("collected", block.getValue(FlashlightSpawnBlock.COLLECTED));
            data.addProperty("roundGeneration", flashlight.roundGeneration());
            data.addProperty("spawnerGeneration", flashlight.spawnerGeneration());
        } else {
            return null;
        }
        return data;
    }

    private static boolean isTracked(BlockEntity entity) {
        return entity instanceof DoorBlockEntity || entity instanceof MachineBlockEntity
            || entity instanceof KeySpawnBlockEntity || entity instanceof FlashlightSpawnBlockEntity;
    }

    private static StructureId key(ServerLevel level, BlockPos pos) {
        return new StructureId(level.dimension().location().toString(), pos.asLong());
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data/nightminigames_structures.json");
    }

    private static void ensure(MinecraftServer server) {
        if (server != activeServer) {
            activeServer = server;
            load(server);
        }
    }

    private static void load(MinecraftServer server) {
        Path file = file(server);
        if (!Files.isRegularFile(file)) {
            dirty = true;
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray teams = root.getAsJsonArray("teams");
            JsonArray dimensions = root.getAsJsonArray("loadedDimensions");
            lastRoster = teams == null ? "" : GSON.toJson(teams);
            lastDimensions = dimensions == null ? "" : GSON.toJson(dimensions);
            JsonArray structures = root.getAsJsonArray("structures");
            if (structures != null) {
                for (var item : structures) {
                    JsonObject state = item.getAsJsonObject();
                    String dimension = state.get("dimension").getAsString();
                    BlockPos pos = new BlockPos(state.get("x").getAsInt(), state.get("y").getAsInt(), state.get("z").getAsInt());
                    RECORDS.put(new StructureId(dimension, pos.asLong()), state);
                }
            }
        } catch (Exception exception) {
            readOnly = true;
            NightminigamesMod.LOGGER.error("[Nightminigames] Índice de estructuras corrupto: no se sobrescribirá. Comprueba .bak.", exception);
        }
    }

    public static List<KnownKey> knownKeys(MinecraftServer server, String team) {
        ensure(server);
        refreshPending();
        List<KnownKey> result = new ArrayList<>();
        for (var entry : RECORDS.entrySet()) {
            JsonObject data = entry.getValue();
            if (!data.has("type") || !data.has("team") || !team.equals(data.get("team").getAsString())) {
                continue;
            }
            String type = data.get("type").getAsString();
            if (!type.startsWith("llave_")) {
                continue;
            }
            String color = type.substring(6);
            boolean collected = data.has("collected") && data.get("collected").getAsBoolean()
                && dev.greto.nightminigames.round.RoundResetSystem.isCurrentKeyGeneration(
                    data.has("roundGeneration") ? data.get("roundGeneration").getAsLong() : 0L,
                    data.has("spawnerGeneration") ? data.get("spawnerGeneration").getAsLong() : 0L);
            result.add(new KnownKey(entry.getKey().dimension(), BlockPos.of(entry.getKey().position()), color, collected));
        }
        result.sort(Comparator.comparing(KnownKey::dimension)
            .thenComparing(KnownKey::color)
            .thenComparingInt(key -> key.pos().getX())
            .thenComparingInt(key -> key.pos().getZ()));
        return List.copyOf(result);
    }

    /** Resolves indexed structures against the real world, including unloaded chunks. */
    public static List<KnownStructure> knownStructures(MinecraftServer server, String team) {
        ensure(server);
        refreshPending();
        List<KnownStructure> result = new ArrayList<>();
        for (var entry : new ArrayList<>(RECORDS.entrySet())) {
            JsonObject data = entry.getValue();
            if (!data.has("team") || !team.equals(data.get("team").getAsString())) continue;
            ServerLevel level = null;
            for (ServerLevel candidate : server.getAllLevels()) {
                if (candidate.dimension().location().toString().equals(entry.getKey().dimension())) {
                    level = candidate;
                    break;
                }
            }
            if (level == null) continue;
            BlockPos pos = BlockPos.of(entry.getKey().position());
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            BlockEntity entity = level.getBlockEntity(pos);
            JsonObject actual = entity == null ? null : snapshot(level, entity);
            if (actual == null || !team.equals(actual.get("team").getAsString())) continue;
            result.add(new KnownStructure(level.dimension().location().toString(), pos, actual.get("type").getAsString()));
        }
        return List.copyOf(result);
    }

    /** Indexed machine locations: does not force chunks to load for map markers. */
    public static List<KnownStructure> knownMachines(MinecraftServer server, String team) {
        ensure(server);
        refreshPending();
        List<KnownStructure> result = new ArrayList<>();
        for (var entry : RECORDS.entrySet()) {
            JsonObject data = entry.getValue();
            if (data.has("type") && "maquina".equals(data.get("type").getAsString())
                && data.has("team") && team.equals(data.get("team").getAsString())) {
                result.add(new KnownStructure(entry.getKey().dimension(), BlockPos.of(entry.getKey().position()), "maquina"));
            }
        }
        return List.copyOf(result);
    }

    public record KnownStructure(String dimension, BlockPos pos, String type) {
    }

    public record KnownKey(String dimension, BlockPos pos, String color, boolean collected) {
    }

    private static JsonArray roster(MinecraftServer server) {
        JsonArray teams = new JsonArray();
        for (String name : ModuleSystem.TEAMS) {
            PlayerTeam team = server.getScoreboard().getPlayerTeam(name);
            JsonObject entry = new JsonObject();
            entry.addProperty("team", name);
            JsonArray members = new JsonArray();
            if (team != null) {
                ArrayList<String> names = new ArrayList<>(team.getPlayers());
                Collections.sort(names);
                for (String member : names) {
                    members.add(member);
                }
            }
            entry.add("members", members);
            teams.add(entry);
        }
        return teams;
    }

    private static void persist(MinecraftServer server, boolean force) {
        if (readOnly || !force && !dirty) {
            return;
        }
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        root.addProperty("worldData", "nightminigames");
        JsonArray dimensions = new JsonArray();
        for (String dimension : loadedDimensionNames(server)) {
            dimensions.add(dimension);
        }
        root.add("loadedDimensions", dimensions);
        root.add("teams", roster(server));
        JsonArray structures = new JsonArray();
        ArrayList<StructureId> keys = new ArrayList<>(RECORDS.keySet());
        keys.sort(Comparator.comparing(StructureId::dimension).thenComparingLong(StructureId::position));
        for (StructureId id : keys) {
            structures.add(RECORDS.get(id));
        }
        root.add("structures", structures);
        JsonArray existingJson = new JsonArray();
        for (String data : List.of("nightminigames_modulos.json", "nightminigames_team_regions.json",
            "nightminigames_team_cupos.json", "nightminigames_team_exit.json", "nightminigames_completed_machines.json",
            "nightminigames_round_generation.json", "nightminigames_hud_history.json",
            "nightminigames_classification_hud.json", "nightminigames_flashlight.json",
            "nightminigames_environment.json", "nightminigames_admin_show.json", "nightminigames_structures.json")) {
            existingJson.add(data);
        }
        root.add("configurationFiles", existingJson);
        Path file = file(server);
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
            if (Files.isRegularFile(file)) {
                Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            lastRoster = GSON.toJson(roster(server));
            lastDimensions = GSON.toJson(loadedDimensionNames(server));
            dirty = false;
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo guardar nightminigames_structures.json", exception);
        }
    }

    private static List<String> loadedDimensionNames(MinecraftServer server) {
        ArrayList<String> result = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            result.add(level.dimension().location().toString());
        }
        Collections.sort(result);
        return result;
    }

    private record StructureId(String dimension, long position) {
    }
}
