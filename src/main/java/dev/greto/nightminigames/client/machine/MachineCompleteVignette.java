package dev.greto.nightminigames.client.machine;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import dev.greto.nightminigames.client.HudScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class MachineCompleteVignette {
    private static final double DURATION_SECONDS = 3.4D;
    private static long activatedAt;
    private static boolean initialized;

    private MachineCompleteVignette() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> activatedAt = 0L);
        HudRenderCallback.EVENT.register(MachineCompleteVignette::render);
    }

    public static void activate() {
        activatedAt = System.nanoTime();
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (activatedAt == 0L || client.level == null || client.options.hideGui || client.screen != null) {
            return;
        }
        double elapsed = (System.nanoTime() - activatedAt) * 1.0E-9D;
        if (elapsed >= DURATION_SECONDS) {
            activatedAt = 0L;
            return;
        }
        float fadeIn = Mth.clamp((float) (elapsed / 0.34D), 0.0F, 1.0F);
        float fadeOut = Mth.clamp((float) ((DURATION_SECONDS - elapsed) / 0.9D), 0.0F, 1.0F);
        float pulse = 0.92F + (float) Math.sin(elapsed * 7.0D) * 0.08F;
        float opacity = Math.min(fadeIn, fadeOut) * pulse;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int vertical = Math.max(1, Math.min(height / 3, Math.round(HudScale.reference(88.0F))));
        int horizontal = Math.max(1, Math.min(width / 4, Math.round(HudScale.reference(118.0F))));
        for (int i = 0; i < vertical; i += 2) {
            float t = 1.0F - i / (float) vertical;
            float strength = t * t * opacity;
            int alpha = Mth.clamp((int) (95.0F * strength), 0, 255);
            int color = (alpha << 24) | 0x20EE72;
            graphics.fill(0, i, width, Math.min(height, i + 2), color);
            graphics.fill(0, Math.max(0, height - i - 2), width, height - i, color);
        }
        for (int i = 0; i < horizontal; i += 2) {
            float t = 1.0F - i / (float) horizontal;
            float strength = t * t * opacity;
            int alpha = Mth.clamp((int) (98.0F * strength), 0, 255);
            int color = (alpha << 24) | 0x19C95C;
            graphics.fill(i, 0, Math.min(width, i + 2), height, color);
            graphics.fill(Math.max(0, width - i - 2), 0, width - i, height, color);
        }
    }
}
