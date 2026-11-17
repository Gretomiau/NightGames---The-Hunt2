package dev.greto.nightminigames.flashlight;

import com.mojang.serialization.MapCodec;
import dev.greto.nightminigames.registry.ModItems;
import dev.greto.nightminigames.team.TeamBinding;
import dev.greto.nightminigames.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.network.chat.Component;

public final class FlashlightSpawnBlock extends BaseEntityBlock {
    public static final BooleanProperty COLLECTED = BooleanProperty.create("collected");
    private static final VoxelShape SELECTABLE = Block.box(3, 0, 3, 13, 15, 13);
    private static final MapCodec<FlashlightSpawnBlock> CODEC = simpleCodec(FlashlightSpawnBlock::new);

    public FlashlightSpawnBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(COLLECTED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FlashlightSpawnBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SELECTABLE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (dev.greto.nightminigames.machine.TeamExitCompletionSystem.isFinished()) {
            return InteractionResult.FAIL;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || state.getValue(COLLECTED)) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof FlashlightSpawnBlockEntity spawner
            && !TeamBinding.canInteract(serverPlayer, spawner.ownerTeam())) {
            return InteractionResult.FAIL;
        }
        ItemStack item = new ItemStack(ModItems.FLASHLIGHT);
        if (level.getBlockEntity(pos) instanceof FlashlightSpawnBlockEntity entity) {
            TeamBinding.set(item, entity.ownerTeam());
        }
        if (!player.getInventory().add(item)) {
            serverPlayer.displayClientMessage(Component.literal("Inventario lleno"), true);
            return InteractionResult.SUCCESS;
        }
        level.setBlock(pos, state.setValue(COLLECTED, true), Block.UPDATE_CLIENTS);
        if (level instanceof ServerLevel serverLevel) {
            dev.greto.nightminigames.persistence.WorldStructureJson.markBlockStateChanged(serverLevel, pos);
        }
        level.playSound(null, pos, ModSounds.KEY_COLLECT, SoundSource.PLAYERS, 0.75F, 0.9F);
        serverPlayer.displayClientMessage(Component.literal("Linterna recogida"), true);
        return InteractionResult.SUCCESS;
    }
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock() && level instanceof ServerLevel serverLevel) {
            dev.greto.nightminigames.persistence.WorldStructureJson.remove(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

}
