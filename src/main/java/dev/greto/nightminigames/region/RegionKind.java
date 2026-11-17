package dev.greto.nightminigames.region;

import net.minecraft.ChatFormatting;

public enum RegionKind {
    ENTRADA("entrada", "ENTRADA", ChatFormatting.AQUA, 0x36DBEE),
    SALIDA("salida", "SALIDA", ChatFormatting.GOLD, 0xFFAD51);

    public final String id;
    public final String label;
    public final ChatFormatting format;
    public final int rgb;

    RegionKind(String id, String label, ChatFormatting format, int rgb) {
        this.id = id;
        this.label = label;
        this.format = format;
        this.rgb = rgb;
    }

    public static RegionKind fromId(String id) {
        for (RegionKind kind : values()) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }
        return null;
    }
}
