package dev.greto.nightminigames.admin;

import dev.greto.nightminigames.NightminigamesMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AdminShowPayload(boolean enabled, String team) implements CustomPacketPayload {
    public static final Type<AdminShowPayload> TYPE = new Type<>(NightminigamesMod.id("admin_show"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AdminShowPayload> CODEC = new StreamCodec<>() {
        @Override
        public AdminShowPayload decode(RegistryFriendlyByteBuf buffer) {
            return new AdminShowPayload(buffer.readBoolean(), buffer.readUtf(16));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, AdminShowPayload payload) {
            buffer.writeBoolean(payload.enabled());
            buffer.writeUtf(payload.team(), 16);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
