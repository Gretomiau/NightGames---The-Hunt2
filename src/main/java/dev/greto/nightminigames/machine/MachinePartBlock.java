package dev.greto.nightminigames.machine;

import dev.greto.nightminigames.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class MachinePartBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty INDEX = IntegerProperty.create("index", 0, 5);

    public MachinePartBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(INDEX, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, INDEX);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean movedByPiston) {
        if (state.getBlock() != replacement.getBlock() && level instanceof ServerLevel serverLevel) {
            BlockPos master = pos.relative(state.getValue(FACING).getClockWise(), 1 - state.getValue(INDEX) % 3)
                .below(state.getValue(INDEX) / 3);
            if (serverLevel.hasChunkAt(master) && serverLevel.getBlockState(master).getBlock() == ModBlocks.MACHINE) {
                serverLevel.destroyBlock(master, true);
            }
        }
        super.onRemove(state, level, pos, replacement, movedByPiston);
    }
}
