package dev.greto.nightminigames.key;

import dev.greto.nightminigames.NightminigamesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record KeySpawnerPlacementPayload(BlockPos pos, String color) implements CustomPacketPayload {
    public static final Type<KeySpawnerPlacementPayload> TYPE = new Type<>(NightminigamesMod.id("key_spawner_placed"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KeySpawnerPlacementPayload> CODEC = new StreamCodec<>() {
        @Override
        public KeySpawnerPlacementPayload decode(RegistryFriendlyByteBuf buffer) {
            return new KeySpawnerPlacementPayload(buffer.readBlockPos(), buffer.readUtf(16));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, KeySpawnerPlacementPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeUtf(payload.color(), 16);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
