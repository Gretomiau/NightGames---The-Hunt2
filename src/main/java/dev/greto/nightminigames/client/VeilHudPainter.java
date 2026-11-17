package dev.greto.nightminigames.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class VeilHudPainter {
    private VeilHudPainter() {
    }

    public static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, int rgb, float opacity, double time) {
        int panel = rgba(0x07131D, (int) (225 * opacity));
        int panel2 = rgba(0x0A1B29, (int) (236 * opacity));
        int accent = rgba(rgb, (int) (245 * opacity));
        int accentSoft = rgba(brighten(rgb, 0.55D), (int) (170 * opacity));
        int haze = rgba(rgb, (int) (34 * opacity));
        graphics.fill(x - 5, y - 5, x + width + 5, y + height + 5, haze);
        graphics.fillGradient(x, y, x + width, y + height, panel, panel2);
        graphics.fill(x, y, x + width, y + 2, accent);
        graphics.fill(x, y + height - 2, x + width, y + height, rgba(rgb, (int) (145 * opacity)));
        graphics.fill(x, y, x + 2, y + height, rgba(rgb, (int) (135 * opacity)));
        graphics.fill(x + width - 2, y, x + width, y + height, rgba(rgb, (int) (135 * opacity)));
        graphics.fill(x, y, x + 18, y + 2, accent);
        graphics.fill(x, y, x + 2, y + 13, accent);
        graphics.fill(x + width - 18, y, x + width, y + 2, accent);
        graphics.fill(x + width - 2, y, x + width, y + 13, accent);
        graphics.fill(x, y + height - 2, x + 18, y + height, accent);
        graphics.fill(x, y + height - 13, x + 2, y + height, accent);
        graphics.fill(x + width - 18, y + height - 2, x + width, y + height, accent);
        graphics.fill(x + width - 2, y + height - 13, x + width, y + height, accent);
        int scan = x + (int) (((time * 86.0D) % (width + 38)) - 19);
        graphics.fill(Math.max(x + 1, scan), y + 3, Math.min(x + width - 1, scan + 18), y + height - 3, rgba(rgb, (int) (18 * opacity)));
        for (int i = 0; i < 5; i++) {
            int sy = y + 8 + i * Math.max(10, (height - 18) / 5);
            graphics.fill(x + 10, sy, x + width - 10, sy + 1, rgba(rgb, (int) (12 * opacity)));
        }
        for (int i = 0; i < 8; i++) {
            int px = x + 12 + i * (width - 24) / 8;
            int py = y + 6 + (i % 2) * 3;
            drawDisc(graphics, px, py, 1, accentSoft);
            drawDisc(graphics, px, y + height - 7 - (i % 2) * 2, 1, accentSoft);
        }
    }

    public static void drawSegmentBar(GuiGraphics graphics, int x, int y, int width, int height, float progress, int rgb, float opacity, double time) {
        int track = rgba(0x06131F, (int) (236 * opacity));
        int frame = rgba(darken(rgb, 0.56D), (int) (160 * opacity));
        int fillA = rgba(darken(rgb, 0.25D), (int) (250 * opacity));
        int fillB = rgba(brighten(rgb, 0.22D), (int) (245 * opacity));
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, frame);
        graphics.fill(x, y, x + width, y + height, track);
        int fill = Mth.clamp(Math.round(width * progress), 0, width);
        if (fill > 0) {
            graphics.fillGradient(x, y, x + fill, y + height, fillA, fillB);
            graphics.fill(x, y, x + fill, y + 2, rgba(0xFFFFFF, (int) (120 * opacity)));
            int sweep = x + Mth.clamp((int) (progress * width), 0, width - 1);
            graphics.fill(sweep - 3, y - 2, sweep + 3, y + height + 2, rgba(brighten(rgb, 0.6D), (int) (95 * opacity)));
        }
        int segments = Math.max(8, width / 12);
        for (int i = 1; i < segments; i++) {
            int sx = x + i * width / segments;
            graphics.fill(sx, y + 1, sx + 1, y + height - 1, rgba(0x081219, (int) (150 * opacity)));
        }
        int middle = x + width / 2;
        graphics.fill(middle, y - 3, middle + 1, y + height + 3, rgba(brighten(rgb, 0.75D), (int) (130 * opacity)));
        int sweep2 = x + (int) (((time * 0.7D) % 1.0D) * width);
        graphics.fill(sweep2, y + 1, Math.min(x + width, sweep2 + 2), y + height - 1, rgba(brighten(rgb, 0.4D), (int) (55 * opacity)));
    }

    public static void drawEnergyOrb(GuiGraphics graphics, int cx, int cy, int radius, int rgb, float opacity, double time) {
        int outer = rgba(rgb, (int) (55 * opacity));
        int mid = rgba(brighten(rgb, 0.3D), (int) (95 * opacity));
        int inner = rgba(brighten(rgb, 0.72D), (int) (155 * opacity));
        drawDisc(graphics, cx, cy, radius + 8, outer);
        drawRing(graphics, cx, cy, radius + 12, 2, rgba(brighten(rgb, 0.62D), (int) (110 * opacity)));
        drawDisc(graphics, cx, cy, radius, mid);
        drawDisc(graphics, cx, cy, Math.max(3, (int) (radius * 0.56F)), inner);
        for (int i = 0; i < 10; i++) {
            double angle = time * 0.9D + i * 0.6283185307D;
            int px = (int) Math.round(cx + Math.cos(angle) * (radius + 5));
            int py = (int) Math.round(cy + Math.sin(angle) * (radius + 5));
            drawDisc(graphics, px, py, 2, inner);
        }
    }

    public static void drawDropletBurst(GuiGraphics graphics, int cx, int cy, int radius, int rgb, float opacity, double time) {
        int liquid = rgba(rgb, (int) (135 * opacity));
        int bright = rgba(brighten(rgb, 0.55D), (int) (180 * opacity));
        for (int i = 0; i < 18; i++) {
            double angle = i * 0.3490658504D + time * 1.8D;
            double dist = radius * (1.0D + 0.16D * Math.sin(time * 5.0D + i));
            int x = (int) Math.round(cx + Math.cos(angle) * dist);
            int y = (int) Math.round(cy + Math.sin(angle) * dist * 0.82D);
            drawDisc(graphics, x, y, 2 + (i % 3), i % 4 == 0 ? bright : liquid);
        }
    }

    public static void drawDisc(GuiGraphics graphics, int cx, int cy, int radius, int argb) {
        if ((argb >>> 24) == 0 || radius <= 0) {
            return;
        }
        for (int dy = -radius; dy <= radius; dy += 2) {
            int dx = (int) Math.sqrt((double) radius * radius - (double) dy * dy);
            if (dx > 0) {
                graphics.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 2, argb);
            }
        }
    }

    public static void drawRing(GuiGraphics graphics, int cx, int cy, int radius, int thickness, int argb) {
        if (radius <= 0 || (argb >>> 24) == 0) {
            return;
        }
        int inner = Math.max(0, radius - thickness);
        for (int dy = -radius; dy <= radius; dy += 2) {
            int outerX = (int) Math.sqrt((double) radius * radius - (double) dy * dy);
            int innerX = Math.abs(dy) < inner ? (int) Math.sqrt((double) inner * inner - (double) dy * dy) : 0;
            graphics.fill(cx - outerX, cy + dy, cx - innerX, cy + dy + 2, argb);
            graphics.fill(cx + innerX, cy + dy, cx + outerX + 1, cy + dy + 2, argb);
        }
    }

    public static int rgba(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    public static int brighten(int rgb, double amount) {
        return channel(rgb, 1.0D - amount, 255.0D * amount);
    }

    public static int darken(int rgb, double amount) {
        return channel(rgb, 1.0D - amount, 0.0D);
    }

    private static int channel(int rgb, double multiplier, double extra) {
        int r = Mth.clamp((int) (((rgb >>> 16) & 0xFF) * multiplier + extra), 0, 255);
        int g = Mth.clamp((int) (((rgb >>> 8) & 0xFF) * multiplier + extra), 0, 255);
        int b = Mth.clamp((int) ((rgb & 0xFF) * multiplier + extra), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    public static float ease(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public static double ease(double value) {
        double t = Mth.clamp(value, 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }
}
