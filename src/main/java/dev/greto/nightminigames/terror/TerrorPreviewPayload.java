package dev.greto.nightminigames.terror;

import dev.greto.nightminigames.NightminigamesMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TerrorPreviewPayload(boolean active, String effect, String dimension, double x, double y, double z, float yaw, int face) implements CustomPacketPayload {
    public static final Type<TerrorPreviewPayload> TYPE = new Type<>(NightminigamesMod.id("terror_preview"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TerrorPreviewPayload> CODEC = new StreamCodec<>() {
        public TerrorPreviewPayload decode(RegistryFriendlyByteBuf b) {
            return new TerrorPreviewPayload(b.readBoolean(), b.readUtf(16), b.readUtf(256), b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat(), b.readVarInt());
        }
        public void encode(RegistryFriendlyByteBuf b, TerrorPreviewPayload p) {
            b.writeBoolean(p.active()); b.writeUtf(p.effect(),16); b.writeUtf(p.dimension(),256);
            b.writeDouble(p.x()); b.writeDouble(p.y()); b.writeDouble(p.z()); b.writeFloat(p.yaw()); b.writeVarInt(p.face());
        }
    };
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
