package dev.greto.nightminigames.feed;

import dev.greto.nightminigames.NightminigamesMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record HudFeedPayload(boolean replace, List<Entry> entries) implements CustomPacketPayload {
    public static final Type<HudFeedPayload> TYPE = new Type<>(NightminigamesMod.id("hud_feed"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HudFeedPayload> CODEC = new StreamCodec<>() {
        @Override
        public HudFeedPayload decode(RegistryFriendlyByteBuf buffer) {
            boolean replace = buffer.readBoolean();
            int count = buffer.readVarInt();
            if (count < 0 || count > 128) {
                throw new IllegalArgumentException("Invalid HUD feed size: " + count);
            }
            List<Entry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                entries.add(new Entry(
                    buffer.readUtf(48),
                    buffer.readUtf(48),
                    buffer.readUtf(80),
                    buffer.readUtf(160),
                    buffer.readInt()
                ));
            }
            return new HudFeedPayload(replace, List.copyOf(entries));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, HudFeedPayload payload) {
            buffer.writeBoolean(payload.replace());
            buffer.writeVarInt(payload.entries().size());
            for (Entry entry : payload.entries()) {
                buffer.writeUtf(entry.subject(), 48);
                buffer.writeUtf(entry.verb(), 48);
                buffer.writeUtf(entry.highlight(), 80);
                buffer.writeUtf(entry.subtitle(), 160);
                buffer.writeInt(entry.rgb());
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(String subject, String verb, String highlight, String subtitle, int rgb) {
    }
}
