package dev.greto.nightminigames.environment;

import dev.greto.nightminigames.NightminigamesMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EnvironmentStatePayload(boolean darkness, boolean music, float volume) implements CustomPacketPayload {
    public static final Type<EnvironmentStatePayload> TYPE = new Type<>(NightminigamesMod.id("environment_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EnvironmentStatePayload> CODEC = new StreamCodec<>() {
        @Override
        public EnvironmentStatePayload decode(RegistryFriendlyByteBuf buffer) {
            return new EnvironmentStatePayload(buffer.readBoolean(), buffer.readBoolean(), buffer.readFloat());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, EnvironmentStatePayload payload) {
            buffer.writeBoolean(payload.darkness());
            buffer.writeBoolean(payload.music());
            buffer.writeFloat(payload.volume());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
