package dev.greto.nightminigames.key;

import dev.greto.nightminigames.NightminigamesMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record KeyAnnouncementPayload(UUID playerId, String playerName, String color) implements CustomPacketPayload {
    public static final Type<KeyAnnouncementPayload> TYPE = new Type<>(NightminigamesMod.id("key_announcement"));
    public static final StreamCodec<RegistryFriendlyByteBuf, KeyAnnouncementPayload> CODEC = new StreamCodec<>() {
        @Override
        public KeyAnnouncementPayload decode(RegistryFriendlyByteBuf buffer) {
            return new KeyAnnouncementPayload(buffer.readUUID(), buffer.readUtf(48), buffer.readUtf(16));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, KeyAnnouncementPayload value) {
            buffer.writeUUID(value.playerId());
            buffer.writeUtf(value.playerName(), 48);
            buffer.writeUtf(value.color(), 16);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
