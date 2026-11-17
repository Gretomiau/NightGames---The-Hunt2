package dev.greto.nightminigames.machine;

import dev.greto.nightminigames.NightminigamesMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MachineCompletionPayload(String teamName, int teamColor, boolean visible) implements CustomPacketPayload {
    public static final Type<MachineCompletionPayload> TYPE = new Type<>(NightminigamesMod.id("machine_completion"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineCompletionPayload> CODEC = new StreamCodec<>() {
        @Override
        public MachineCompletionPayload decode(RegistryFriendlyByteBuf buffer) {
            return new MachineCompletionPayload(buffer.readUtf(32), buffer.readVarInt(), buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, MachineCompletionPayload payload) {
            buffer.writeUtf(payload.teamName(), 32);
            buffer.writeVarInt(payload.teamColor());
            buffer.writeBoolean(payload.visible());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
