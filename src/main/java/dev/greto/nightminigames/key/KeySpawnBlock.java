package dev.greto.nightminigames.key;

import dev.greto.nightminigames.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class KeySpawnBlock extends Block implements EntityBlock {
    public static final BooleanProperty COLLECTED = BooleanProperty.create("collected");
    private static final VoxelShape TARGET = Block.box(2, 0, 2, 14, 16, 14);
    public final KeyColor color;

    public KeySpawnBlock(KeyColor color) {
        super(BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F).noOcclusion());
        this.color = color;
        registerDefaultState(stateDefinition.any().setValue(COLLECTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLLECTED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return TARGET;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KeySpawnBlockEntity(pos, state);
    }
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock() && level instanceof ServerLevel serverLevel) {
            dev.greto.nightminigames.persistence.WorldStructureJson.remove(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

}
