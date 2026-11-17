package dev.greto.nightminigames.machine;

import dev.greto.nightminigames.key.KeyColor;
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

public final class MachineBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String[] insertedBy = new String[]{"", "", "", ""};
    private String ownerTeam = "";
    private long roundGeneration;

    public long roundGeneration() {
        return roundGeneration;
    }

    public void setRoundGeneration(long generation) {
        roundGeneration = generation;
        setChanged();
    }


    public MachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE, pos, state);
    }

    public void setInserter(KeyColor color, String playerName) {
        insertedBy[color.ordinal()] = playerName;
        setChanged();
    }

    public String getOwnerTeam() {
        return ownerTeam;
    }

    public void setOwnerTeam(String id) {
        ownerTeam = id;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public String getInserter(KeyColor color) {
        return insertedBy[color.ordinal()];
    }

    public void resetInserters() {
        java.util.Arrays.fill(insertedBy, "");
        setChanged();
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
        CompoundTag names = new CompoundTag();
        for (KeyColor color : KeyColor.values()) {
            names.putString(color.id, getInserter(color));
        }
        tag.put("InsertedBy", names);
        tag.putString("OwnerTeam", ownerTeam);
        tag.putLong("RoundGeneration", roundGeneration);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ownerTeam = tag.getString("OwnerTeam");
        roundGeneration = tag.getLong("RoundGeneration");
        CompoundTag names = tag.getCompound("InsertedBy");
        for (KeyColor color : KeyColor.values()) {
            insertedBy[color.ordinal()] = names.getString(color.id);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
