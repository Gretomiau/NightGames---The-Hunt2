package dev.greto.nightminigames.feed;

import dev.greto.nightminigames.NightminigamesMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record ClassificationFeedPayload(boolean replace, int visibleLimit, List<HudFeedPayload.Entry> entries) implements CustomPacketPayload {
    public static final Type<ClassificationFeedPayload> TYPE = new Type<>(NightminigamesMod.id("classification_feed"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClassificationFeedPayload> CODEC = new StreamCodec<>() {
        @Override
        public ClassificationFeedPayload decode(RegistryFriendlyByteBuf buffer) {
            boolean replace = buffer.readBoolean();
            int limit = buffer.readVarInt();
            int count = buffer.readVarInt();
            if (limit < 1 || limit > 8 || count < 0 || count > 32) {
                throw new IllegalArgumentException("Invalid classification feed payload");
            }
            List<HudFeedPayload.Entry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                entries.add(new HudFeedPayload.Entry(
                    buffer.readUtf(48),
                    buffer.readUtf(48),
                    buffer.readUtf(80),
                    "",
                    buffer.readInt()
                ));
            }
            return new ClassificationFeedPayload(replace, limit, List.copyOf(entries));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ClassificationFeedPayload payload) {
            buffer.writeBoolean(payload.replace());
            buffer.writeVarInt(payload.visibleLimit());
            buffer.writeVarInt(payload.entries().size());
            for (HudFeedPayload.Entry entry : payload.entries()) {
                buffer.writeUtf(entry.subject(), 48);
                buffer.writeUtf(entry.verb(), 48);
                buffer.writeUtf(entry.highlight(), 80);
                buffer.writeInt(entry.rgb());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
