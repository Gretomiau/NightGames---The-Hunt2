package dev.greto.nightminigames.door;

import dev.greto.nightminigames.persistence.WorldStructureJson;
import dev.greto.nightminigames.registry.ModBlockEntities;
import dev.greto.nightminigames.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class DoorBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation OPEN_IDLE = RawAnimation.begin().thenLoop("open_idle");
    private static final RawAnimation OPENING = RawAnimation.begin().thenPlay("open");
    private static final RawAnimation CLOSING = RawAnimation.begin().thenPlay("close");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private String ownerTeam = "";

    public String ownerTeam() {
        return ownerTeam;
    }

    public void setOwnerTeam(String team) {
        ownerTeam = team;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        WorldStructureJson.markStructureChanged(this);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("OwnerTeam", ownerTeam);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ownerTeam = tag.getString("OwnerTeam");
    }

    public DoorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DOOR, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "door", 0, animation ->
            animation.setAndContinue(getBlockState().getValue(DoorBlock.OPEN) ? OPEN_IDLE : IDLE))
            .triggerableAnim("open", OPENING)
            .triggerableAnim("close", CLOSING));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public boolean setOpen(boolean open) {
        if (!(level instanceof ServerLevel serverLevel) || !(getBlockState().getBlock() instanceof DoorBlock)) {
            return false;
        }
        BlockState oldState = getBlockState();
        if (oldState.getValue(DoorBlock.OPEN) == open) {
            return true;
        }
        if (!open && !DoorSystem.canClose(serverLevel, worldPosition, oldState.getValue(DoorBlock.FACING))) {
            return false;
        }
        if (!serverLevel.setBlock(worldPosition, oldState.setValue(DoorBlock.OPEN, open), Block.UPDATE_ALL)) {
            return false;
        }
        DoorSystem.reconcile(serverLevel, worldPosition);
        triggerAnim("door", open ? "open" : "close");
        serverLevel.playSound(null, worldPosition, open ? ModSounds.DOOR_OPEN : ModSounds.DOOR_CLOSE, SoundSource.BLOCKS, 0.9F, 1.0F);
        setChanged();
        return true;
    }
}
