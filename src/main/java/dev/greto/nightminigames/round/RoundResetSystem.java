package dev.greto.nightminigames.round;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.flashlight.FlashlightSpawnBlock;
import dev.greto.nightminigames.flashlight.FlashlightSpawnBlockEntity;
import dev.greto.nightminigames.key.KeySpawnBlock;
import dev.greto.nightminigames.key.KeySpawnBlockEntity;
import dev.greto.nightminigames.machine.MachineBlock;
import dev.greto.nightminigames.machine.MachineBlockEntity;
import dev.greto.nightminigames.registry.ModBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public final class RoundResetSystem {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<Id, BlockEntity> LOADED = new HashMap<>();
    private static final ArrayDeque<Id> PENDING = new ArrayDeque<>();
    private static MinecraftServer activeServer;
    private static long generation;
    private static long spawnerGeneration;
    private static boolean initialized;

    private RoundResetSystem() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            activeServer = server;
            generation = load(server);
            spawnerGeneration = loadSpawnerGeneration(server);
            LOADED.clear();
            PENDING.clear();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LOADED.clear();
            PENDING.clear();
            activeServer = null;
            generation = 0;
            spawnerGeneration = 0;
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((entity, level) -> {
            if (tracked(entity)) {
                Id id = new Id(level, entity.getBlockPos().asLong());
                LOADED.put(id, entity);
                if (needsReset(entity) && !PENDING.contains(id)) {
                    PENDING.addLast(id);
                }
            }
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((entity, level) -> {
            if (tracked(entity)) {
                Id id = new Id(level, entity.getBlockPos().asLong());
                if (LOADED.get(id) == entity) {
                    LOADED.remove(id);
                }
                PENDING.remove(id);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(RoundResetSystem::tick);
    }

    public static void markNew(BlockEntity entity) {
        if (!tracked(entity)) {
            return;
        }
        applyGeneration(entity, generation);
        applySpawnerGeneration(entity, spawnerGeneration);
        if (entity.getLevel() instanceof ServerLevel level) {
            Id id = new Id(level, entity.getBlockPos().asLong());
            LOADED.put(id, entity);
            PENDING.remove(id);
        }
    }

    public static int beginNewRound(MinecraftServer server) {
        dev.greto.nightminigames.integration.MapWaypointBridge.requestRefresh(server);
        if (server != activeServer) {
            return 0;
        }
        generation++;
        save(server);
        PENDING.clear();
        for (var entry : LOADED.entrySet()) {
            if (entry.getKey().level().hasChunkAt(BlockPos.of(entry.getKey().pos()))) {
                PENDING.addLast(entry.getKey());
            }
        }
        return PENDING.size();
    }

    public static int resetSpawners(MinecraftServer server) {
        dev.greto.nightminigames.integration.MapWaypointBridge.requestRefresh(server);
        if (server != activeServer) {
            return 0;
        }
        spawnerGeneration++;
        save(server);
        for (var entry : LOADED.entrySet()) {
            if (isSpawner(entry.getValue()) && entry.getKey().level().hasChunkAt(BlockPos.of(entry.getKey().pos()))
                && !PENDING.contains(entry.getKey())) {
                PENDING.addLast(entry.getKey());
            }
        }
        return (int) PENDING.stream().filter(id -> isSpawner(LOADED.get(id))).count();
    }

    private static boolean isSpawner(BlockEntity entity) {
        return entity instanceof KeySpawnBlockEntity || entity instanceof FlashlightSpawnBlockEntity;
    }

    private static long spawnerVersion(BlockEntity entity) {
        if (entity instanceof KeySpawnBlockEntity key) {
            return key.spawnerGeneration();
        }
        if (entity instanceof FlashlightSpawnBlockEntity flashlight) {
            return flashlight.spawnerGeneration();
        }
        return spawnerGeneration;
    }

    private static void applySpawnerGeneration(BlockEntity entity, long version) {
        if (entity instanceof KeySpawnBlockEntity key) {
            key.setSpawnerGeneration(version);
        } else if (entity instanceof FlashlightSpawnBlockEntity flashlight) {
            flashlight.setSpawnerGeneration(version);
        }
    }

    private static boolean needsReset(BlockEntity entity) {
        return roundGeneration(entity) < generation || isSpawner(entity) && spawnerVersion(entity) < spawnerGeneration;
    }

    public static boolean isCurrentKeyGeneration(long round, long spawners) {
        return round >= generation && spawners >= spawnerGeneration;
    }

    public static boolean isRearming() {
        return !PENDING.isEmpty();
    }

    private static void tick(MinecraftServer server) {
        if (server != activeServer) {
            return;
        }
        int budget = 8;
        while (budget-- > 0 && !PENDING.isEmpty()) {
            Id id = PENDING.removeFirst();
            BlockEntity entity = LOADED.get(id);
            if (entity == null || entity.isRemoved()) {
                continue;
            }
            ServerLevel level = id.level();
            BlockPos pos = BlockPos.of(id.pos());
            if (!level.hasChunkAt(pos) || entity.getBlockPos().asLong() != id.pos()) {
                continue;
            }
            if (!needsReset(entity)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (entity instanceof MachineBlockEntity machine && state.getBlock() == ModBlocks.MACHINE) {
                if (state.getValue(MachineBlock.MASK) != 0) {
                    level.setBlock(pos, state.setValue(MachineBlock.MASK, 0), Block.UPDATE_CLIENTS);
                }
                machine.resetInserters();
                level.sendBlockUpdated(pos, state, level.getBlockState(pos), Block.UPDATE_CLIENTS);
                dev.greto.nightminigames.door.DoorSystem.onMachineChanged(level, pos, 0);
            } else if (entity instanceof KeySpawnBlockEntity && state.getBlock() instanceof KeySpawnBlock) {
                if (state.getValue(KeySpawnBlock.COLLECTED)) {
                    level.setBlock(pos, state.setValue(KeySpawnBlock.COLLECTED, false), Block.UPDATE_CLIENTS);
                }
            } else if (entity instanceof FlashlightSpawnBlockEntity && state.getBlock() == ModBlocks.FLASHLIGHT_SPAWN) {
                if (state.getValue(FlashlightSpawnBlock.COLLECTED)) {
                    level.setBlock(pos, state.setValue(FlashlightSpawnBlock.COLLECTED, false), Block.UPDATE_CLIENTS);
                }
            } else {
                LOADED.remove(id);
                continue;
            }
            applyGeneration(entity, generation);
            applySpawnerGeneration(entity, spawnerGeneration);
        }
    }

    private static boolean tracked(BlockEntity entity) {
        return entity instanceof MachineBlockEntity || entity instanceof KeySpawnBlockEntity || entity instanceof FlashlightSpawnBlockEntity;
    }

    private static long roundGeneration(BlockEntity entity) {
        if (entity instanceof MachineBlockEntity machine) {
            return machine.roundGeneration();
        }
        if (entity instanceof KeySpawnBlockEntity key) {
            return key.roundGeneration();
        }
        if (entity instanceof FlashlightSpawnBlockEntity flashlight) {
            return flashlight.roundGeneration();
        }
        return generation;
    }

    private static void applyGeneration(BlockEntity entity, long value) {
        if (entity instanceof MachineBlockEntity machine) {
            machine.setRoundGeneration(value);
        } else if (entity instanceof KeySpawnBlockEntity key) {
            key.setRoundGeneration(value);
        } else if (entity instanceof FlashlightSpawnBlockEntity flashlight) {
            flashlight.setRoundGeneration(value);
        }
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data/nightminigames_round_generation.json");
    }

    private static long load(MinecraftServer server) {
        Path file = file(server);
        if (!Files.isRegularFile(file)) {
            return 0L;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            return root.has("generation") ? Math.max(0L, root.get("generation").getAsLong()) : 0L;
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo leer el contador de rondas", exception);
            return 0L;
        }
    }

    private static long loadSpawnerGeneration(MinecraftServer server) {
        Path saved = file(server);
        if (!Files.isRegularFile(saved)) {
            return 0L;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(saved, StandardCharsets.UTF_8)).getAsJsonObject();
            return root.has("spawnerGeneration") ? Math.max(0L, root.get("spawnerGeneration").getAsLong()) : 0L;
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo leer la generación de spawners", exception);
            return 0L;
        }
    }

    private static void save(MinecraftServer server) {
        Path file = file(server);
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        JsonObject root = new JsonObject();
        root.addProperty("generation", generation);
        root.addProperty("spawnerGeneration", spawnerGeneration);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo guardar el contador de rondas", exception);
        }
    }

    private record Id(ServerLevel level, long pos) {
    }
}
