package dev.greto.nightminigames.machine;

import dev.greto.nightminigames.registry.ModBlocks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MachineStructureRepair {
    private static final Map<String, Work> PENDING = new LinkedHashMap<>();

    private MachineStructureRepair() {
    }

    public static void initialize() {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof MachineBlockEntity) {
                schedule(level, entity.getBlockPos());
            }
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof MachineBlockEntity) {
                PENDING.remove(key(level, entity.getBlockPos()));
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int handled = 0;
            for (Work work : new ArrayList<>(PENDING.values())) {
                if (handled >= 8) {
                    break;
                }
                if (work.level().getServer() != server || server.getTickCount() < work.dueTick()) {
                    continue;
                }
                PENDING.remove(key(work.level(), work.pos()));
                repair(work.level(), work.pos());
                handled++;
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
    }

    private static String key(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + ":" + pos.asLong();
    }

    private static void schedule(ServerLevel level, BlockPos pos) {
        PENDING.put(key(level, pos), new Work(level, pos.immutable(), level.getServer().getTickCount() + 2));
    }

    private static void repair(ServerLevel level, BlockPos master) {
        if (!level.hasChunkAt(master)) {
            return;
        }
        BlockState state = level.getBlockState(master);
        if (state.getBlock() != ModBlocks.MACHINE) {
            return;
        }
        Direction facing = state.getValue(MachineBlock.FACING);
        for (int index = 0; index < 6; index++) {
            if (index == 1) {
                continue;
            }
            BlockPos part = MachineBlock.partPosition(master, facing, index);
            if (!level.hasChunkAt(part)) {
                continue;
            }
            BlockState existing = level.getBlockState(part);
            if (existing.isAir() || existing.canBeReplaced()) {
                level.setBlock(part, ModBlocks.MACHINE_PART.defaultBlockState()
                    .setValue(MachinePartBlock.FACING, facing)
                    .setValue(MachinePartBlock.INDEX, index), Block.UPDATE_CLIENTS);
            }
        }
    }

    private record Work(ServerLevel level, BlockPos pos, long dueTick) {
    }
}
