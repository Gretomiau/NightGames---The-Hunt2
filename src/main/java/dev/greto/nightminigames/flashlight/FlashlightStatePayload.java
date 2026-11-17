package dev.greto.nightminigames.flashlight;

import dev.greto.nightminigames.NightminigamesMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record FlashlightStatePayload(UUID playerId, String dimension, boolean active) implements CustomPacketPayload {
    public static final Type<FlashlightStatePayload> TYPE = new Type<>(NightminigamesMod.id("flashlight_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlashlightStatePayload> CODEC = new StreamCodec<>() {
        @Override
        public FlashlightStatePayload decode(RegistryFriendlyByteBuf buffer) {
            return new FlashlightStatePayload(buffer.readUUID(), buffer.readUtf(128), buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FlashlightStatePayload value) {
            buffer.writeUUID(value.playerId());
            buffer.writeUtf(value.dimension(), 128);
            buffer.writeBoolean(value.active());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
