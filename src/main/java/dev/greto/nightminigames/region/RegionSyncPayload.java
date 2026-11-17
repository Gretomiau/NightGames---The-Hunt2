package dev.greto.nightminigames.region;

import dev.greto.nightminigames.NightminigamesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RegionSyncPayload(String team, String region, String dimension, boolean hasFirst, BlockPos first, boolean hasSecond, BlockPos second) implements CustomPacketPayload {
    public static final Type<RegionSyncPayload> TYPE = new Type<>(NightminigamesMod.id("region_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RegionSyncPayload> CODEC = new StreamCodec<>() {
        @Override
        public RegionSyncPayload decode(RegistryFriendlyByteBuf buffer) {
            String team = buffer.readUtf(16);
            String kind = buffer.readUtf(16);
            String dimension = buffer.readUtf(128);
            boolean hasFirst = buffer.readBoolean();
            BlockPos first = buffer.readBlockPos();
            boolean hasSecond = buffer.readBoolean();
            BlockPos second = buffer.readBlockPos();
            return new RegionSyncPayload(team, kind, dimension, hasFirst, first, hasSecond, second);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RegionSyncPayload payload) {
            buffer.writeUtf(payload.team(), 16);
            buffer.writeUtf(payload.region(), 16);
            buffer.writeUtf(payload.dimension(), 128);
            buffer.writeBoolean(payload.hasFirst());
            buffer.writeBlockPos(payload.first());
            buffer.writeBoolean(payload.hasSecond());
            buffer.writeBlockPos(payload.second());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
