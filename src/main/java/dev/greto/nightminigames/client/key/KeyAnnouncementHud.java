package dev.greto.nightminigames.client.key;

import dev.greto.nightminigames.key.KeyAnnouncementPayload;
import dev.greto.nightminigames.client.VeilHudShader;
import dev.greto.nightminigames.key.KeyColor;
import dev.greto.nightminigames.client.HudScale;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Locale;
import java.util.UUID;

public final class KeyAnnouncementHud {
    private static final double DURATION = 5.2D;
    private static final int PANEL_WIDTH = 344;
    private static final int PANEL_HEIGHT = 110;
    private static final ArrayDeque<KeyAnnouncementPayload> PENDING = new ArrayDeque<>();
    private static KeyAnnouncementPayload active;
    private static long startedAt;
    private static boolean initialized;

    private KeyAnnouncementHud() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientPlayNetworking.registerGlobalReceiver(KeyAnnouncementPayload.TYPE, (payload, context) -> context.client().execute(() -> {
            if (PENDING.size() < 6) {
                PENDING.addLast(payload);
            }
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        HudRenderCallback.EVENT.register(KeyAnnouncementHud::render);
    }

    private static void reset() {
        PENDING.clear();
        active = null;
        startedAt = 0L;
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            reset();
            return;
        }
        long now = System.nanoTime();
        if (active != null && (now - startedAt) * 1.0E-9D >= DURATION) {
            active = null;
        }
        if (active == null && !PENDING.isEmpty()) {
            active = PENDING.removeFirst();
            startedAt = now;
        }
        if (active == null || client.options.hideGui || client.screen != null) {
            return;
        }
        KeyColor keyColor = colorOf(active.color());
        double elapsed = (now - startedAt) * 1.0E-9D;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float fit = HudScale.fit(graphics, PANEL_WIDTH, PANEL_HEIGHT, 16.0F, 24.0F);
        int panelX = Math.round((width - PANEL_WIDTH * fit) / 2.0F);
        int panelY = Math.round((height - PANEL_HEIGHT * fit) * 0.24F);
        int orbX = Math.round(panelX + 58.0F * fit);
        int orbY = Math.round(panelY + 56.0F * fit);
        int orbRadius = Math.max(16, Math.round(26.0F * fit));
        boolean veilBackground = VeilHudShader.draw(graphics, panelX, panelY, PANEL_WIDTH * fit, PANEL_HEIGHT * fit, keyColor.rgb, 1.0F, (float) Math.min(1.0D, elapsed / 1.36D), 3, elapsed);
        if (!veilBackground) {
            if (elapsed < 0.82D) {
                renderDropletFormation(graphics, orbX, orbY, orbRadius, elapsed / 0.82D, keyColor.rgb);
            } else if (elapsed < 1.36D) {
                renderOrbExplosion(graphics, orbX, orbY, orbRadius, (elapsed - 0.82D) / 0.54D, keyColor.rgb);
            }
        }
        if (elapsed >= 0.88D) {
            renderAnnouncement(graphics, client, elapsed, fit, panelX, panelY, keyColor, orbX, orbY, orbRadius, veilBackground);
        }
    }

    private static void renderDropletFormation(GuiGraphics graphics, int cx, int cy, int orbRadius, double time, int rgb) {
        double t = ease(time);
        int pathRadius = (int) (orbRadius * (2.8D - 1.75D * t));
        int alphaBase = (int) (150 + 90 * t);
        int liquid = rgba(rgb, alphaBase);
        int bright = rgba(brighten(rgb, 0.62D), Math.min(255, alphaBase + 25));
        int core = rgba(brighten(rgb, 0.38D), (int) (90 * t));
        int background = rgba(darken(rgb, 0.78D), (int) (70 * t));
        drawDisc(graphics, cx, cy, orbRadius + Math.round(orbRadius * 0.35F), background);
        for (int i = 0; i < 36; i++) {
            double angle = i * 0.43633231299D + time * 3.2D;
            double ripple = Math.sin(time * 9.0D + i * 1.37D) * orbRadius * 0.18D;
            double radius = pathRadius + ripple;
            int x = (int) Math.round(cx + Math.cos(angle) * radius);
            int y = (int) Math.round(cy + Math.sin(angle) * radius * 0.84D);
            int size = 3 + (i % 4) + (int) Math.round((1.0D - t) * 4.0D);
            drawDisc(graphics, x, y, size, i % 3 == 0 ? bright : liquid);
        }
        for (int i = 0; i < 18; i++) {
            double angle = i * 0.69813170079D - time * 2.1D;
            double radius = orbRadius * (1.8D - t * 1.2D) + Math.sin(time * 11.0D + i) * 3.5D;
            int x = (int) Math.round(cx + Math.cos(angle) * radius);
            int y = (int) Math.round(cy + Math.sin(angle) * radius * 0.9D);
            int size = 2 + (i % 3);
            drawDisc(graphics, x, y, size, bright);
        }
        drawDisc(graphics, cx, cy, Math.max(4, (int) (orbRadius * (0.26D + 0.54D * t))), core);
        drawRing(graphics, cx, cy, Math.max(8, (int) (orbRadius * (1.2D + 0.2D * Math.sin(time * Math.PI * 2.0D)))), Math.max(2, orbRadius / 6), rgba(brighten(rgb, 0.3D), (int) (75 + 90 * t)));
    }

    private static void renderOrbExplosion(GuiGraphics graphics, int cx, int cy, int orbRadius, double time, int rgb) {
        double t = ease(time);
        int liquid = rgba(rgb, (int) (210 * (1.0D - t)));
        int bright = rgba(brighten(rgb, 0.68D), (int) (230 * (1.0D - t)));
        int ballRadius = Math.max(4, (int) (orbRadius * (1.15D - 0.45D * t)));
        drawDisc(graphics, cx, cy, ballRadius, rgba(brighten(rgb, 0.22D), (int) (180 * (1.0D - t * 0.35D))));
        drawDisc(graphics, cx, cy, Math.max(3, (int) (ballRadius * 0.72D)), rgba(brighten(rgb, 0.58D), (int) (210 * (1.0D - t * 0.55D))));
        drawRing(graphics, cx, cy, Math.max(ballRadius + 2, (int) (orbRadius * (1.0D + 1.65D * t))), Math.max(2, orbRadius / 7), bright);
        for (int i = 0; i < 40; i++) {
            double angle = i * 0.31415926535D + i * 0.11D;
            double speed = 0.42D + (i % 7) * 0.09D;
            double distance = orbRadius * (0.2D + 2.1D * t * speed);
            double wobble = Math.sin(i * 0.9D + t * 9.0D) * orbRadius * 0.12D;
            int x = (int) Math.round(cx + Math.cos(angle) * distance + Math.sin(angle * 2.0D) * wobble);
            int y = (int) Math.round(cy + Math.sin(angle) * distance * 0.82D);
            int size = 2 + (i % 4);
            drawDisc(graphics, x, y, size, i % 4 == 0 ? bright : liquid);
        }
        for (int i = 0; i < 12; i++) {
            double angle = i * 0.52359877559D + t * 0.35D;
            int x2 = (int) Math.round(cx + Math.cos(angle) * orbRadius * (2.0D + t * 1.2D));
            int y2 = (int) Math.round(cy + Math.sin(angle) * orbRadius * (1.4D + t * 0.9D));
            drawStreak(graphics, cx, cy, x2, y2, rgba(brighten(rgb, 0.55D), (int) (130 * (1.0D - t))));
        }
    }

    private static void renderAnnouncement(GuiGraphics graphics, Minecraft client, double elapsed, float fit, int panelX, int panelY, KeyColor color, int orbX, int orbY, int orbRadius, boolean veilBackground) {
        double appear = ease((elapsed - 0.88D) / 0.45D);
        double disappear = 1.0D - ease((elapsed - 4.55D) / 0.45D);
        float opacity = (float) Math.min(appear, disappear);
        if (opacity <= 0.0F) {
            return;
        }
        float reveal = (float) ease((elapsed - 1.02D) / 0.32D);
        graphics.pose().pushPose();
        graphics.pose().translate(panelX, panelY, 0.0F);
        graphics.pose().scale(fit, fit, 1.0F);
        int localOrbX = Math.round((orbX - panelX) / fit);
        int localOrbY = Math.round((orbY - panelY) / fit);
        int accent = rgba(color.rgb, (int) (255.0F * opacity));
        int accentSoft = rgba(brighten(color.rgb, 0.58D), (int) (190.0F * opacity));
        int panelA = (int) (212.0F * opacity);
        int bgTop = (panelA << 24) | 0x07111D;
        int bgBottom = (panelA << 24) | 0x0B1A2A;
        int x0 = (int) Math.round(localOrbX - (localOrbX - 0) * reveal);
        int x1 = (int) Math.round(localOrbX + (PANEL_WIDTH - localOrbX) * reveal);
        if (!veilBackground) {
            graphics.fillGradient(x0, 0, x1, PANEL_HEIGHT, bgTop, bgBottom);
        }
        if (reveal > 0.08F && !veilBackground) {
            graphics.fill(0, 0, PANEL_WIDTH, 2, accent);
            graphics.fill(0, PANEL_HEIGHT - 2, PANEL_WIDTH, PANEL_HEIGHT, rgba(color.rgb, (int) (150.0F * opacity)));
            graphics.fill(0, 0, 2, PANEL_HEIGHT, rgba(color.rgb, (int) (120.0F * opacity)));
            graphics.fill(PANEL_WIDTH - 2, 0, PANEL_WIDTH, PANEL_HEIGHT, rgba(color.rgb, (int) (120.0F * opacity)));
            graphics.fill(95, 17, 97, PANEL_HEIGHT - 17, rgba(color.rgb, (int) (90.0F * opacity)));
            graphics.fill(PANEL_WIDTH - 71, 12, PANEL_WIDTH - 69, PANEL_HEIGHT - 12, rgba(color.rgb, (int) (75.0F * opacity)));
            int scan = (int) (((elapsed - 0.9D) * 72.0D) % PANEL_WIDTH);
            graphics.fill(scan, 4, Math.min(PANEL_WIDTH, scan + 28), PANEL_HEIGHT - 4, rgba(color.rgb, (int) (16.0F * opacity)));
            for (int i = 0; i < 5; i++) {
                int y = 16 + i * 18;
                graphics.fill(106, y, PANEL_WIDTH - 16, y + 1, rgba(color.rgb, (int) (18.0F * opacity)));
            }
        }
        if (!veilBackground) {
            renderOrbSocket(graphics, localOrbX, localOrbY, orbRadius, color.rgb, opacity, elapsed);
        }
        if (reveal > 0.35F) {
            ResourceLocation texture = playerSkin(client, active.playerId());
            renderHead(graphics, texture, 24, 22, 58);
            int textCenter = 214;
            graphics.drawCenteredString(client.font, "LLAVE CONSEGUIDA", textCenter, 14, rgba(brighten(color.rgb, 0.7D), (int) (255.0F * opacity)));
            graphics.drawCenteredString(client.font, active.playerName(), textCenter, 36, rgba(0xFFFFFF, (int) (255.0F * opacity)));
            graphics.drawCenteredString(client.font, "CONSIGUIÓ LA", textCenter, 56, rgba(0xB5CFDE, (int) (255.0F * opacity)));
            graphics.drawCenteredString(client.font, "LLAVE " + color.display.toUpperCase(Locale.ROOT), textCenter, 74, accent);
            graphics.fill(106, 93, PANEL_WIDTH - 16, 95, rgba(color.rgb, (int) (98.0F * opacity)));
        }
        graphics.pose().popPose();
    }

    private static void renderOrbSocket(GuiGraphics graphics, int cx, int cy, int orbRadius, int rgb, float opacity, double elapsed) {
        int frame = rgba(brighten(rgb, 0.56D), (int) (200.0F * opacity));
        int frameSoft = rgba(rgb, (int) (85.0F * opacity));
        int base = rgba(darken(rgb, 0.8D), (int) (170.0F * opacity));
        int orbBase = rgba(brighten(rgb, 0.18D), (int) (115.0F * opacity));
        int orbBright = rgba(brighten(rgb, 0.72D), (int) (165.0F * opacity));
        drawDisc(graphics, cx, cy, orbRadius + 10, frameSoft);
        drawRing(graphics, cx, cy, orbRadius + 15, 3, frame);
        drawRing(graphics, cx, cy, orbRadius + 6, 2, rgba(brighten(rgb, 0.32D), (int) (120.0F * opacity)));
        graphics.fill(15, 14, 88, 92, base);
        graphics.fill(18, 17, 85, 89, 0xFF06101A);
        float pulse = 0.92F + (float) (Math.sin(elapsed * 7.0D) * 0.08D);
        drawDisc(graphics, cx, cy, Math.max(6, Math.round(orbRadius * 0.92F * pulse)), orbBase);
        drawDisc(graphics, cx, cy, Math.max(3, Math.round(orbRadius * 0.54F * pulse)), orbBright);
        for (int i = 0; i < 8; i++) {
            double angle = elapsed * 0.7D + i * 0.78539816339D;
            int x = (int) Math.round(cx + Math.cos(angle) * (orbRadius + 6));
            int y = (int) Math.round(cy + Math.sin(angle) * (orbRadius + 6));
            drawDisc(graphics, x, y, 2, orbBright);
        }
    }

    private static ResourceLocation playerSkin(Minecraft client, UUID playerId) {
        if (client.getConnection() != null) {
            PlayerInfo info = client.getConnection().getPlayerInfo(playerId);
            if (info != null) {
                return info.getSkin().texture();
            }
        }
        return DefaultPlayerSkin.get(playerId).texture();
    }

    private static void renderHead(GuiGraphics graphics, ResourceLocation texture, int x, int y, int size) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        float scale = size / 8.0F;
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(texture, 0, 0, 8.0F, 8.0F, 8, 8, 64, 64);
        graphics.blit(texture, 0, 0, 40.0F, 8.0F, 8, 8, 64, 64);
        graphics.pose().popPose();
    }

    private static void drawDisc(GuiGraphics graphics, int cx, int cy, int radius, int argb) {
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

    private static void drawRing(GuiGraphics graphics, int cx, int cy, int radius, int thickness, int argb) {
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

    private static void drawStreak(GuiGraphics graphics, int x0, int y0, int x1, int y1, int argb) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps <= 0) {
            return;
        }
        for (int i = 0; i <= steps; i += 2) {
            float t = i / (float) steps;
            int x = Math.round(x0 + dx * t);
            int y = Math.round(y0 + dy * t);
            drawDisc(graphics, x, y, 1, argb);
        }
    }

    private static int rgba(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static int darken(int rgb, double amount) {
        return channel(rgb, 1.0D - amount, 0.0D);
    }

    private static int brighten(int rgb, double amount) {
        return channel(rgb, 1.0D - amount, 255.0D * amount);
    }

    private static int channel(int rgb, double multiplier, double extra) {
        int r = Mth.clamp((int) (((rgb >>> 16) & 0xFF) * multiplier + extra), 0, 255);
        int g = Mth.clamp((int) (((rgb >>> 8) & 0xFF) * multiplier + extra), 0, 255);
        int b = Mth.clamp((int) ((rgb & 0xFF) * multiplier + extra), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private static double ease(double value) {
        double t = Mth.clamp(value, 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }

    private static KeyColor colorOf(String id) {
        for (KeyColor value : KeyColor.values()) {
            if (value.id.equals(id)) {
                return value;
            }
        }
        return KeyColor.BLUE;
    }
}
