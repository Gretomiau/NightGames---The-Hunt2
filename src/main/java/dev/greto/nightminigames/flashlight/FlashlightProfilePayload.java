package dev.greto.nightminigames.flashlight;

import dev.greto.nightminigames.NightminigamesMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record FlashlightProfilePayload(FlashlightLightingProfile profile) implements CustomPacketPayload {
    public static final Type<FlashlightProfilePayload> TYPE = new Type<>(NightminigamesMod.id("flashlight_profile"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlashlightProfilePayload> CODEC = new StreamCodec<>() {
        @Override
        public FlashlightProfilePayload decode(RegistryFriendlyByteBuf buffer) {
            return new FlashlightProfilePayload(new FlashlightLightingProfile(
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat()
            ));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FlashlightProfilePayload value) {
            FlashlightLightingProfile p = value.profile();
            buffer.writeFloat(p.rangeBlocks());
            buffer.writeFloat(p.coreBrightness());
            buffer.writeFloat(p.spillBrightness());
            buffer.writeFloat(p.coreAngleDegrees());
            buffer.writeFloat(p.spillAngleDegrees());
            buffer.writeDouble(p.coreSize());
            buffer.writeDouble(p.spillSize());
            buffer.writeFloat(p.spillRangeMultiplier());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
