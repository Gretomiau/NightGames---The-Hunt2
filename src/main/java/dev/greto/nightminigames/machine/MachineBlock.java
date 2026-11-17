package dev.greto.nightminigames.machine;

import com.mojang.serialization.MapCodec;
import dev.greto.nightminigames.registry.ModBlocks;
import dev.greto.nightminigames.door.DoorSystem;
import dev.greto.nightminigames.team.TeamBinding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class MachineBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty MASK = IntegerProperty.create("mask", 0, 15);
    private static final MapCodec<MachineBlock> CODEC = simpleCodec(MachineBlock::new);

    public MachineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MASK, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MASK);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineBlockEntity(pos, state);
    }

    public static BlockPos partPosition(BlockPos master, Direction facing, int index) {
        return master.relative(facing.getClockWise(), index % 3 - 1).above(index / 3);
    }

    public static BlockPos resolveMaster(BlockGetter world, BlockPos clicked) {
        if (world instanceof Level level && !level.hasChunkAt(clicked)) {
            return null;
        }
        BlockState state = world.getBlockState(clicked);
        if (state.getBlock() == ModBlocks.MACHINE) {
            return clicked;
        }
        if (state.getBlock() != ModBlocks.MACHINE_PART) {
            return null;
        }
        int index = state.getValue(MachinePartBlock.INDEX);
        Direction facing = state.getValue(MachinePartBlock.FACING);
        BlockPos master = clicked.relative(facing.getClockWise(), 1 - index % 3).below(index / 3);
        if (world instanceof Level level && !level.hasChunkAt(master)) {
            return null;
        }
        BlockState masterState = world.getBlockState(master);
        return masterState.getBlock() == ModBlocks.MACHINE
            && masterState.getValue(FACING) == facing
            && partPosition(master, facing, index).equals(clicked) ? master : null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        BlockPos origin = context.getClickedPos();
        for (int index = 0; index < 6; index++) {
            if (index == 1) {
                continue;
            }
            BlockPos part = partPosition(origin, facing, index);
            if (!level.hasChunkAt(part) || !level.getWorldBorder().isWithinBounds(part) || !level.getBlockState(part).canBeReplaced()) {
                return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel)) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            if (TeamBinding.valid(TeamBinding.of(stack))) {
                machine.setOwnerTeam(TeamBinding.of(stack));
            }
            dev.greto.nightminigames.round.RoundResetSystem.markNew(machine);
        }
        Direction facing = state.getValue(FACING);
        for (int index = 0; index < 6; index++) {
            if (index == 1) {
                continue;
            }
            BlockPos target = partPosition(pos, facing, index);
            if (level.hasChunkAt(target) && level.getBlockState(target).canBeReplaced()) {
                level.setBlock(target, ModBlocks.MACHINE_PART.defaultBlockState()
                    .setValue(MachinePartBlock.FACING, facing)
                    .setValue(MachinePartBlock.INDEX, index), Block.UPDATE_ALL);
            }
        }
        MachineProgressSync.update((ServerLevel) level, pos, "", -1);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock() && level instanceof ServerLevel serverLevel) {
            dev.greto.nightminigames.persistence.WorldStructureJson.remove(serverLevel, pos);
            DoorSystem.onMachineRemoved(serverLevel, pos);
            MachineCompletionSync.reset(serverLevel, pos);
            Direction facing = state.getValue(FACING);
            for (int index = 0; index < 6; index++) {
                if (index == 1) {
                    continue;
                }
                BlockPos partPos = partPosition(pos, facing, index);
                if (!level.hasChunkAt(partPos)) {
                    continue;
                }
                BlockState part = level.getBlockState(partPos);
                if (part.getBlock() == ModBlocks.MACHINE_PART
                    && part.getValue(MachinePartBlock.INDEX) == index
                    && part.getValue(MachinePartBlock.FACING) == facing) {
                    level.removeBlock(partPos, false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
