package dev.greto.nightminigames.door;

import com.mojang.serialization.MapCodec;
import dev.greto.nightminigames.region.RegionKind;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DoorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private final MapCodec<DoorBlock> doorCodec;
    private final RegionKind kind;

    public DoorBlock(RegionKind kind, BlockBehaviour.Properties properties) {
        super(properties);
        this.kind = kind;
        this.doorCodec = simpleCodec(blockProperties -> new DoorBlock(kind, blockProperties));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }

    public RegionKind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return doorCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DoorBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos origin = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level world = context.getLevel();
        for (int y = 0; y <= 8; y++) {
            for (int offset = -4; offset <= 4; offset++) {
                if (y == 0 && offset == 0) {
                    continue;
                }
                BlockPos target = DoorSystem.partPosition(origin, facing, offset, y);
                if (!world.hasChunkAt(target) || !world.getWorldBorder().isWithinBounds(target) || !world.getBlockState(target).isAir()) {
                    return null;
                }
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel) {
            if (level.getBlockEntity(pos) instanceof DoorBlockEntity door) {
                door.setOwnerTeam(TeamBinding.of(stack));
            }
            DoorSystem.register(serverLevel, pos);
            DoorSystem.reconcile(serverLevel, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock() && level instanceof ServerLevel serverLevel) {
            dev.greto.nightminigames.persistence.WorldStructureJson.remove(serverLevel, pos);
            DoorSystem.removeCollision(serverLevel, pos, state.getValue(FACING));
            DoorSystem.unregister(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
