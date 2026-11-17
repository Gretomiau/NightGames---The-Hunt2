package dev.greto.nightminigames.door;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.machine.MachineBlock;
import dev.greto.nightminigames.machine.MachineBlockEntity;
import dev.greto.nightminigames.team.TeamBinding;
import dev.greto.nightminigames.region.RegionKind;
import dev.greto.nightminigames.region.RegionSelectionSystem;
import dev.greto.nightminigames.registry.ModBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class DoorSystem {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<DoorId, DoorRef> DOORS = new HashMap<>();
    private static final Map<DoorId, Integer> RECONCILE = new LinkedHashMap<>();
    private static final Map<DoorId, CollisionCleanup> CLEANUP = new LinkedHashMap<>();
    private static final Map<DoorId, Integer> COLLISION_RETRY = new HashMap<>();
    private static final Map<MachineId, MachineRef> MACHINES_TO_CHECK = new LinkedHashMap<>();
    private static final int COLLISION_BUDGET_PER_TICK = 12;
    private static final int TOTAL_PARTS = 81;
    private static final Set<MachineId> COMPLETED = new HashSet<>();
    private static final Set<String> EXIT_CLOSED = new HashSet<>();
    private static final Map<String, Boolean> ENTRANCE_OVERRIDES = new HashMap<>();
    private static MinecraftServer activeServer;
    private static boolean serverReady;
    private static boolean initialized;

    private DoorSystem() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            activeServer = server;
            serverReady = false;
            DOORS.clear();
            RECONCILE.clear();
            CLEANUP.clear();
            COLLISION_RETRY.clear();
            MACHINES_TO_CHECK.clear();
            ENTRANCE_OVERRIDES.clear();
            load(server);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (server == activeServer) {
                serverReady = true;
                for (DoorId id : DOORS.keySet()) {
                    RECONCILE.put(id, 0);
                }
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            activeServer = null;
            serverReady = false;
            DOORS.clear();
            RECONCILE.clear();
            CLEANUP.clear();
            COLLISION_RETRY.clear();
            MACHINES_TO_CHECK.clear();
            COMPLETED.clear();
            EXIT_CLOSED.clear();
            ENTRANCE_OVERRIDES.clear();
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof DoorBlockEntity) {
                register(level, entity.getBlockPos());
            } else if (entity instanceof MachineBlockEntity) {
                BlockPos pos = entity.getBlockPos().immutable();
                MACHINES_TO_CHECK.put(new MachineId(dimension(level), pos.asLong(), ""), new MachineRef(level, pos));
            }
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof DoorBlockEntity) {
                unregister(level, entity.getBlockPos());
            } else if (entity instanceof MachineBlockEntity) {
                MACHINES_TO_CHECK.remove(new MachineId(dimension(level), entity.getBlockPos().asLong(), ""));
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(DoorSystem::tick);
    }

    public static void register(ServerLevel level, BlockPos pos) {
        DoorId id = new DoorId(dimension(level), pos.asLong());
        DOORS.put(id, new DoorRef(level, pos.immutable()));
        CLEANUP.remove(id);
        COLLISION_RETRY.remove(id);
        RECONCILE.put(id, 0);
    }

    public static void unregister(ServerLevel level, BlockPos pos) {
        DoorId id = new DoorId(dimension(level), pos.asLong());
        DOORS.remove(id);
        RECONCILE.remove(id);
        COLLISION_RETRY.remove(id);
    }

    public static void onMachineChanged(ServerLevel level, BlockPos position, int mask) {
        MACHINES_TO_CHECK.remove(new MachineId(dimension(level), position.asLong(), ""));
        updateMachineState(level, position, mask, true);
    }

    private static void onMachineLoaded(ServerLevel level, BlockPos position, int mask) {
        updateMachineState(level, position, mask, false);
    }

    private static void updateMachineState(ServerLevel level, BlockPos position, int mask, boolean persist) {
        String dimension = dimension(level);
        String team = level.getBlockEntity(position) instanceof MachineBlockEntity machine
            ? machine.getOwnerTeam() : "";
        MachineId id = new MachineId(dimension, position.asLong(), team);
        boolean hadCompletedMachine = hasCompletedMachine(dimension, team);
        boolean changed;
        if (mask == 15) {
            boolean cleaned = COMPLETED.removeIf(old -> old.dimension().equals(dimension) && old.position() == position.asLong() && !old.team().equals(team));
            changed = COMPLETED.add(id) || cleaned;
        } else {
            changed = COMPLETED.removeIf(old -> old.dimension().equals(dimension) && old.position() == position.asLong());
        }
        boolean hasCompletedMachine = hasCompletedMachine(dimension, team);
        boolean reopened = false;
        if (!hasCompletedMachine || !hadCompletedMachine && hasCompletedMachine) {
            reopened = EXIT_CLOSED.remove(scope(dimension, team));
        }
        if (persist && (changed || reopened) && activeServer == level.getServer()) {
            save(level.getServer());
        }
    }

    public static void onMachineRemoved(ServerLevel level, BlockPos pos) {
        onMachineChanged(level, pos, 0);
    }

    private static String scope(String dimension, String team) {
        return dimension + "|" + team;
    }

    public static void onNewTeamCompletion(String dimension, String team) {
        if (EXIT_CLOSED.remove(scope(dimension, team)) && activeServer != null) {
            save(activeServer);
        }
    }

    public static void openEntranceFor(MinecraftServer server, String dimension, String team) {
        ENTRANCE_OVERRIDES.put(scope(dimension, team), false);
        updateDoorStates(server);
    }

    public static void openEntrancesFor(MinecraftServer server, Map<String, String> teamDimensions) {
        for (var entry : teamDimensions.entrySet()) {
            ENTRANCE_OVERRIDES.put(scope(entry.getValue(), entry.getKey()), false);
        }
        updateDoorStates(server);
    }

    public static void clearEntranceOverride(String dimension, String team) {
        ENTRANCE_OVERRIDES.remove(scope(dimension, team));
    }

    public static void resetRound(MinecraftServer server) {
        ENTRANCE_OVERRIDES.clear();
        COMPLETED.clear();
        EXIT_CLOSED.clear();
        MACHINES_TO_CHECK.clear();
        save(server);
        updateDoorStates(server);
    }

    public static void refreshEntranceStates(MinecraftServer server) {
        updateDoorStates(server);
    }

    public static boolean hasCompletedMachine(String dimension, String team) {
        for (MachineId key : COMPLETED) {
            if (key.dimension().equals(dimension) && key.team().equals(team)) {
                return true;
            }
        }
        return false;
    }

    public static int completedMachines(String dimension, String team) {
        int count = 0;
        for (MachineId key : COMPLETED) {
            if (key.dimension().equals(dimension) && key.team().equals(team)) {
                count++;
            }
        }
        return count;
    }

    public static int loadedDoors(String dimension, RegionKind kind, String team) {
        int count = 0;
        for (var ref : DOORS.values()) {
            if (dimension(ref.level()).equals(dimension) && ref.level().hasChunkAt(ref.pos())) {
                BlockState state = ref.level().getBlockState(ref.pos());
                if (state.getBlock() instanceof DoorBlock block && block.kind() == kind
                    && ref.level().getBlockEntity(ref.pos()) instanceof DoorBlockEntity door
                    && door.ownerTeam().equals(team)) {
                    count++;
                }
            }
        }
        return count;
    }

    public static BlockPos partPosition(BlockPos master, Direction facing, int offset, int vertical) {
        Direction side = facing.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
        return master.relative(side, offset).above(vertical);
    }

    public static boolean canClose(ServerLevel level, BlockPos origin, Direction facing) {
        BlockPos start = partPosition(origin, facing, -4, 0);
        BlockPos end = partPosition(origin, facing, 4, 8);
        double x0 = Math.min(start.getX(), end.getX());
        double y0 = origin.getY();
        double z0 = Math.min(start.getZ(), end.getZ());
        double x1 = Math.max(start.getX(), end.getX()) + 1.0D;
        double y1 = origin.getY() + 9.0D;
        double z1 = Math.max(start.getZ(), end.getZ()) + 1.0D;
        AABB doorway = new AABB(x0, y0, z0, x1, y1, z1);
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator() && player.getBoundingBox().intersects(doorway)) {
                return false;
            }
        }
        for (int y = 0; y <= 8; y++) {
            for (int offset = -4; offset <= 4; offset++) {
                if (offset == 0 && y == 0) {
                    continue;
                }
                BlockPos target = partPosition(origin, facing, offset, y);
                if (!level.hasChunkAt(target)) {
                    return false;
                }
                BlockState block = level.getBlockState(target);
                if (!block.isAir() && block.getBlock() != ModBlocks.DOOR_GHOST) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void removeCollision(ServerLevel level, BlockPos origin, Direction facing) {
        DoorId id = new DoorId(dimension(level), origin.asLong());
        RECONCILE.remove(id);
        COLLISION_RETRY.remove(id);
        CLEANUP.put(id, new CollisionCleanup(level, origin.immutable(), facing, 0));
    }

    public static void reconcile(ServerLevel level, BlockPos pos) {
        DoorId id = new DoorId(dimension(level), pos.asLong());
        if (DOORS.containsKey(id)) {
            COLLISION_RETRY.remove(id);
            RECONCILE.put(id, 0);
        }
    }

    private static void tick(MinecraftServer server) {
        if (server != activeServer || !serverReady) {
            return;
        }
        checkLoadedMachines();
        if (!COLLISION_RETRY.isEmpty()) {
            var iterator = COLLISION_RETRY.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (server.getTickCount() >= entry.getValue()) {
                    RECONCILE.putIfAbsent(entry.getKey(), 0);
                    iterator.remove();
                }
            }
        }
        if (server.getTickCount() % 5 == 0) {
            updateDoorStates(server);
        }
        processCollisions();
    }

    private static void checkLoadedMachines() {
        int budget = 4;
        while (budget-- > 0 && !MACHINES_TO_CHECK.isEmpty()) {
            MachineId id = MACHINES_TO_CHECK.keySet().iterator().next();
            MachineRef ref = MACHINES_TO_CHECK.remove(id);
            if (!ref.level().hasChunkAt(ref.pos())) {
                continue;
            }
            BlockState state = ref.level().getBlockState(ref.pos());
            if (state.getBlock() == ModBlocks.MACHINE) {
                onMachineLoaded(ref.level(), ref.pos(), state.getValue(MachineBlock.MASK));
            }
        }
    }

    private static void updateDoorStates(MinecraftServer server) {
        Map<String, Boolean> entranceOccupancy = new HashMap<>();
        Map<String, Boolean> exitOccupancy = new HashMap<>();
        for (var entry : new ArrayList<>(DOORS.entrySet())) {
            DoorId id = entry.getKey();
            DoorRef ref = entry.getValue();
            ServerLevel level = ref.level();
            BlockPos pos = ref.pos();
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof DoorBlock block)) {
                DOORS.remove(id);
                RECONCILE.remove(id);
                continue;
            }
            String team = level.getBlockEntity(pos) instanceof DoorBlockEntity door
                ? door.ownerTeam() : "";
            String scoped = scope(id.dimension(), team);
            boolean desired;
            if (block.kind() == RegionKind.ENTRADA) {
                boolean occupied = entranceOccupancy.computeIfAbsent(scoped, ignored ->
                    RegionSelectionSystem.occupied(server, RegionKind.ENTRADA, id.dimension(), team));
                Boolean visited = ENTRANCE_OVERRIDES.get(scoped);
                if (visited != null && occupied) {
                    ENTRANCE_OVERRIDES.put(scoped, true);
                } else if (Boolean.TRUE.equals(visited) && !occupied) {
                    ENTRANCE_OVERRIDES.remove(scoped);
                }
                desired = ENTRANCE_OVERRIDES.containsKey(scoped);
            } else {
                boolean completed = hasCompletedMachine(id.dimension(), team);
                if (completed && !EXIT_CLOSED.contains(scoped)) {
                    boolean ready = dev.greto.nightminigames.machine.TeamExitCompletionSystem.hasTeamCompletion(id.dimension(), team)
                        ? dev.greto.nightminigames.machine.TeamExitCompletionSystem.consumeDoorClose(server, id.dimension(), team)
                        : exitOccupancy.computeIfAbsent(scoped, ignored ->
                            RegionSelectionSystem.occupied(server, RegionKind.SALIDA, id.dimension(), team));
                    if (ready) {
                        EXIT_CLOSED.add(scoped);
                        save(server);
                    }
                }
                desired = completed && !EXIT_CLOSED.contains(scoped);
            }
            if (desired != state.getValue(DoorBlock.OPEN) && level.getBlockEntity(pos) instanceof DoorBlockEntity door) {
                door.setOpen(desired);
            }
        }
    }

    private static void processCollisions() {
        int remaining = COLLISION_BUDGET_PER_TICK;
        while (remaining > 0 && (!CLEANUP.isEmpty() || !RECONCILE.isEmpty())) {
            if (!CLEANUP.isEmpty()) {
                DoorId id = CLEANUP.keySet().iterator().next();
                CollisionCleanup task = CLEANUP.remove(id);
                ServerLevel level = task.level();
                int step = task.nextPart();
                while (step < TOTAL_PARTS && remaining > 0) {
                    int vertical = step / 9;
                    int offset = step % 9 - 4;
                    step++;
                    if (vertical == 0 && offset == 0) {
                        continue;
                    }
                    remaining--;
                    BlockPos target = partPosition(task.origin(), task.facing(), offset, vertical);
                    if (!level.hasChunkAt(target)) {
                        continue;
                    }
                    if (level.getBlockState(target).getBlock() == ModBlocks.DOOR_GHOST) {
                        level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
                if (step < TOTAL_PARTS) {
                    CLEANUP.put(id, new CollisionCleanup(level, task.origin(), task.facing(), step));
                }
                continue;
            }
            DoorId id = RECONCILE.keySet().iterator().next();
            int step = RECONCILE.remove(id);
            DoorRef ref = DOORS.get(id);
            if (ref == null || !ref.level().hasChunkAt(ref.pos())) {
                continue;
            }
            ServerLevel level = ref.level();
            BlockPos origin = ref.pos();
            BlockState state = level.getBlockState(origin);
            if (!(state.getBlock() instanceof DoorBlock)) {
                DOORS.remove(id);
                continue;
            }
            Direction facing = state.getValue(DoorBlock.FACING);
            boolean open = state.getValue(DoorBlock.OPEN);
            while (step < TOTAL_PARTS && remaining > 0) {
                int vertical = step / 9;
                int offset = step % 9 - 4;
                step++;
                if (vertical == 0 && offset == 0) {
                    continue;
                }
                remaining--;
                BlockPos target = partPosition(origin, facing, offset, vertical);
                if (!level.hasChunkAt(target)) {
                    continue;
                }
                BlockState current = level.getBlockState(target);
                if (open && current.getBlock() == ModBlocks.DOOR_GHOST) {
                    level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                } else if (!open && current.isAir()) {
                    if (isPlayerStandingHere(level, target)) {
                        COLLISION_RETRY.put(id, activeServer.getTickCount() + 20);
                    } else {
                        level.setBlock(target, ModBlocks.DOOR_GHOST.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
            if (step < TOTAL_PARTS) {
                RECONCILE.putIfAbsent(id, step);
            }
        }
    }

    private static boolean isPlayerStandingHere(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator() && player.getBoundingBox().intersects(box)) {
                return true;
            }
        }
        return false;
    }

    private static String dimension(ServerLevel level) {
        return level.dimension().location().toString();
    }

    private static Path dataFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("nightminigames_completed_machines.json");
    }

    private static void load(MinecraftServer server) {
        COMPLETED.clear();
        EXIT_CLOSED.clear();
        Path file = dataFile(server);
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("machines") && root.get("machines").isJsonArray()) {
                for (var value : root.getAsJsonArray("machines")) {
                    if (!value.isJsonObject()) {
                        continue;
                    }
                    JsonObject item = value.getAsJsonObject();
                    if (item.has("dimension") && item.has("position")) {
                        COMPLETED.add(new MachineId(item.get("dimension").getAsString(), item.get("position").getAsLong(), item.has("team") ? item.get("team").getAsString() : ""));
                    }
                }
            }
            if (root.has("exitClosedDimensions") && root.get("exitClosedDimensions").isJsonArray()) {
                for (var value : root.getAsJsonArray("exitClosedDimensions")) {
                    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                        String dimension = value.getAsString();
                        if (COMPLETED.stream().anyMatch(m -> scope(m.dimension(), m.team()).equals(dimension))) {
                            EXIT_CLOSED.add(dimension);
                        }
                    }
                }
            }
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo cargar el estado de las puertas", exception);
        }
    }

    private static void save(MinecraftServer server) {
        Path file = dataFile(server);
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        JsonArray machines = new JsonArray();
        for (MachineId machine : COMPLETED) {
            JsonObject item = new JsonObject();
            item.addProperty("dimension", machine.dimension());
            item.addProperty("position", machine.position());
            item.addProperty("team", machine.team());
            machines.add(item);
        }
        JsonObject root = new JsonObject();
        root.add("machines", machines);
        JsonArray exits = new JsonArray();
        for (String dimension : EXIT_CLOSED) {
            if (COMPLETED.stream().anyMatch(m -> scope(m.dimension(), m.team()).equals(dimension))) {
                exits.add(dimension);
            }
        }
        root.add("exitClosedDimensions", exits);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo guardar el estado de las puertas", exception);
        }
    }

    private record DoorId(String dimension, long position) {
    }

    private record DoorRef(ServerLevel level, BlockPos pos) {
    }

    private record CollisionCleanup(ServerLevel level, BlockPos origin, Direction facing, int nextPart) {
    }

    private record MachineId(String dimension, long position, String team) {
    }

    private record MachineRef(ServerLevel level, BlockPos pos) {
    }
}
