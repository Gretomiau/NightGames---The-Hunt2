package dev.greto.nightminigames.key;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class KeyMessages {
    private KeyMessages() {
    }

    public static MutableComponent prefix() {
        return Component.literal("[OP] ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            .append(Component.literal("[Nightminigames] ").withStyle(ChatFormatting.DARK_AQUA));
    }

    public static Component message(String text, ChatFormatting color) {
        return prefix().append(Component.literal(text).withStyle(color));
    }

    public static Component pickup(String playerName, KeyColor color) {
        return Component.literal(playerName).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
            .append(Component.literal(" ha recogido la ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("LLAVE " + color.display.toUpperCase(java.util.Locale.ROOT)).withStyle(color.formatting, ChatFormatting.BOLD))
            .append(Component.literal("!").withStyle(ChatFormatting.WHITE));
    }

    public static Component key(String text, KeyColor color) {
        return prefix().append(Component.literal(text).withStyle(ChatFormatting.GRAY))
            .append(Component.literal(color.display).withStyle(color.formatting, ChatFormatting.BOLD));
    }
}
