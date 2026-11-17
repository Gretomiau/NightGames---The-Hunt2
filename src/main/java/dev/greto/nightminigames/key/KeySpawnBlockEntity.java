package dev.greto.nightminigames.key;

import dev.greto.nightminigames.persistence.WorldStructureJson;
import dev.greto.nightminigames.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class KeySpawnBlockEntity extends BlockEntity {
    private String ownerTeam = "";
    private String pickupToken = "";

    public String pickupToken() {
        return pickupToken;
    }

    public void setPickupToken(String token) {
        pickupToken = token;
        setChanged();
    }
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
        tag.putString("PickupToken", pickupToken);
        tag.putLong("RoundGeneration", roundGeneration);
        tag.putLong("SpawnerGeneration", spawnerGeneration);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ownerTeam = tag.getString("OwnerTeam");
        pickupToken = tag.getString("PickupToken");
        roundGeneration = tag.getLong("RoundGeneration");
        spawnerGeneration = tag.getLong("SpawnerGeneration");
    }

    public KeySpawnBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KEY_SPAWNER, pos, state);
    }
}
