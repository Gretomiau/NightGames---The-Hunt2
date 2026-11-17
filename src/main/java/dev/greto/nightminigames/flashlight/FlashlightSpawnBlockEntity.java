package dev.greto.nightminigames.flashlight;

import dev.greto.nightminigames.persistence.WorldStructureJson;
import dev.greto.nightminigames.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class FlashlightSpawnBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private String ownerTeam = "";
    private long roundGeneration;
    private long spawnerGeneration;

    public long spawnerGeneration() {
        return spawnerGeneration;
    }

    public void setSpawnerGeneration(long generation) {
        spawnerGeneration = generation;
        setChanged();
    }

    public long roundGeneration() {
        return roundGeneration;
    }

    public void setRoundGeneration(long generation) {
        roundGeneration = generation;
        setChanged();
    }


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
        tag.putLong("RoundGeneration", roundGeneration);
        tag.putLong("SpawnerGeneration", spawnerGeneration);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ownerTeam = tag.getString("OwnerTeam");
        roundGeneration = tag.getLong("RoundGeneration");
        spawnerGeneration = tag.getLong("SpawnerGeneration");
    }

    public FlashlightSpawnBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLASHLIGHT_SPAWN, pos, state);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }
}
