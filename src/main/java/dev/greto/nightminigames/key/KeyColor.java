package dev.greto.nightminigames.key;

import dev.greto.nightminigames.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;

public enum KeyColor {
    BLUE("blue", "Azul", ChatFormatting.AQUA, 0x49A9FF),
    YELLOW("yellow", "Amarilla", ChatFormatting.YELLOW, 0xFFD65A),
    RED("red", "Roja", ChatFormatting.RED, 0xFF6565),
    GREEN("green", "Verde", ChatFormatting.GREEN, 0x61E8A0);

    public final String id;
    public final String display;
    public final ChatFormatting formatting;
    public final int rgb;

    KeyColor(String id, String display, ChatFormatting formatting, int rgb) {
        this.id = id;
        this.display = display;
        this.formatting = formatting;
        this.rgb = rgb;
    }

    public Item keyItem() {
        return switch (this) {
            case BLUE -> ModItems.KEY_BLUE;
            case YELLOW -> ModItems.KEY_YELLOW;
            case RED -> ModItems.KEY_RED;
            case GREEN -> ModItems.KEY_GREEN;
        };
    }
}
