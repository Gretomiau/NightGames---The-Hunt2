package dev.greto.nightminigames.key;

import dev.greto.nightminigames.NightminigamesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record KeyHoldPayload(BlockPos pos, boolean holding) implements CustomPacketPayload {
    public static final Type<KeyHoldPayload> TYPE = new Type<>(NightminigamesMod.id("key_hold"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KeyHoldPayload> CODEC = new StreamCodec<>() {
        @Override
        public KeyHoldPayload decode(RegistryFriendlyByteBuf buffer) {
            return new KeyHoldPayload(buffer.readBlockPos(), buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, KeyHoldPayload value) {
            buffer.writeBlockPos(value.pos());
            buffer.writeBoolean(value.holding());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
