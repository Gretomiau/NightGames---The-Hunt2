package dev.greto.nightminigames.machine;

import dev.greto.nightminigames.NightminigamesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MachineProgressPayload(
    String dimension,
    BlockPos position,
    int mask,
    String blue,
    String yellow,
    String red,
    String green,
    String actor,
    int eventColor,
    boolean removed
) implements CustomPacketPayload {
    public static final Type<MachineProgressPayload> TYPE = new Type<>(NightminigamesMod.id("machine_progress"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineProgressPayload> CODEC = new StreamCodec<>() {
        @Override
        public MachineProgressPayload decode(RegistryFriendlyByteBuf buffer) {
            return new MachineProgressPayload(
                buffer.readUtf(128), buffer.readBlockPos(), buffer.readVarInt(),
                buffer.readUtf(32), buffer.readUtf(32), buffer.readUtf(32), buffer.readUtf(32),
                buffer.readUtf(32), buffer.readVarInt(), buffer.readBoolean()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, MachineProgressPayload payload) {
            buffer.writeUtf(payload.dimension(), 128);
            buffer.writeBlockPos(payload.position());
            buffer.writeVarInt(payload.mask());
            buffer.writeUtf(payload.blue(), 32);
            buffer.writeUtf(payload.yellow(), 32);
            buffer.writeUtf(payload.red(), 32);
            buffer.writeUtf(payload.green(), 32);
            buffer.writeUtf(payload.actor(), 32);
            buffer.writeVarInt(payload.eventColor());
            buffer.writeBoolean(payload.removed());
        }
    };

    public String inserter(int index) {
        return switch (index) {
            case 0 -> blue;
            case 1 -> yellow;
            case 2 -> red;
            case 3 -> green;
            default -> "";
        };
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
