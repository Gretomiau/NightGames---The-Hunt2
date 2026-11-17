package dev.greto.nightminigames.key;

import dev.greto.nightminigames.NightminigamesMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record KeyPickupPayload(String color) implements CustomPacketPayload {
    public static final Type<KeyPickupPayload> TYPE = new Type<>(NightminigamesMod.id("key_pickup"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KeyPickupPayload> CODEC = new StreamCodec<>() {
        @Override
        public KeyPickupPayload decode(RegistryFriendlyByteBuf buffer) {
            return new KeyPickupPayload(buffer.readUtf(16));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, KeyPickupPayload value) {
            buffer.writeUtf(value.color(), 16);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
