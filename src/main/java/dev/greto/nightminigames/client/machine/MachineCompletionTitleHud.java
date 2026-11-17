package dev.greto.nightminigames.client.machine;

import dev.greto.nightminigames.client.VeilHudPainter;
import dev.greto.nightminigames.client.HudScale;
import dev.greto.nightminigames.client.VeilHudShader;
import dev.greto.nightminigames.machine.MachineCompletionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Locale;

public final class MachineCompletionTitleHud {
    private static final double DURATION_SECONDS = 6.8D;
    private static final int PANEL_WIDTH = 492;
    private static final int PANEL_HEIGHT = 112;
    private static final ArrayDeque<MachineCompletionPayload> QUEUE = new ArrayDeque<>();
    private static MachineCompletionPayload active;
    private static long startedAt;
    private static long closingAt;
    private static boolean initialized;

    private MachineCompletionTitleHud() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientPlayNetworking.registerGlobalReceiver(MachineCompletionPayload.TYPE, (payload, context) -> context.client().execute(() -> {
            if (payload.visible()) {
                if (!payload.teamName().isBlank()) {
                    active = payload;
                    startedAt = System.nanoTime();
                    closingAt = 0L;
                    QUEUE.clear();
                } else if (QUEUE.size() < 5) {
                    QUEUE.addLast(payload);
                }
            } else if (active != null && active.teamName().equals(payload.teamName())) {
                closingAt = System.nanoTime();
            }
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        HudRenderCallback.EVENT.register(MachineCompletionTitleHud::render);
    }

    private static void clear() {
        active = null;
        QUEUE.clear();
        startedAt = 0L;
        closingAt = 0L;
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            clear();
            return;
        }
        long now = System.nanoTime();
        if (active != null && (active.teamName().isBlank() && (now - startedAt) * 1.0E-9D >= DURATION_SECONDS
            || closingAt != 0L && (now - closingAt) * 1.0E-9D >= 0.55D)) {
            active = null;
            closingAt = 0L;
        }
        if (active == null && !QUEUE.isEmpty()) {
            active = QUEUE.removeFirst();
            startedAt = now;
            closingAt = 0L;
        }
        if (active == null || client.options.hideGui || client.screen != null) {
            return;
        }
        double elapsed = (now - startedAt) * 1.0E-9D;
        float appear = VeilHudPainter.ease((float) (elapsed / 0.50D));
        float disappear = closingAt != 0L
            ? 1.0F - VeilHudPainter.ease((float) ((now - closingAt) * 1.0E-9D / 0.55D))
            : active.teamName().isBlank()
                ? 1.0F - VeilHudPainter.ease((float) ((elapsed - (DURATION_SECONDS - 0.95D)) / 0.95D))
                : 1.0F;
        float opacity = Mth.clamp(Math.min(appear, disappear), 0.0F, 1.0F);
        if (opacity <= 0.0F) {
            return;
        }
        int sw = graphics.guiWidth();
        int sh = graphics.guiHeight();
        float fit = HudScale.fit(graphics, PANEL_WIDTH, PANEL_HEIGHT, 18.0F, 36.0F);
        float bounce = 0.94F + 0.06F * appear;
        float scale = fit * bounce;
        float x = (sw - PANEL_WIDTH * scale) / 2.0F;
        float y = Math.max(HudScale.pixels(5.0F), (sh - PANEL_HEIGHT * scale) * 0.32F + (1.0F - appear) * 18.0F * fit);
        double seconds = System.nanoTime() * 1.0E-9D;
        int color = active.teamColor() & 0xFFFFFF;
        if (!VeilHudShader.draw(graphics, x, y, PANEL_WIDTH * scale, PANEL_HEIGHT * scale, color, opacity, 1.0F, 2, seconds)) {
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            VeilHudPainter.drawPanel(graphics, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, color, opacity, seconds);
            graphics.pose().popPose();
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        int center = PANEL_WIDTH / 2;
        if (!active.teamName().isBlank()) {
            graphics.drawCenteredString(client.font, "EQUIPO " + active.teamName().toUpperCase(Locale.ROOT), center, 9, VeilHudPainter.rgba(color, Math.round(opacity * 255.0F)));
        } else {
            graphics.drawCenteredString(client.font, "NIGHTMINIGAMES", center, 9, VeilHudPainter.rgba(0x8AE6C6, Math.round(opacity * 220.0F)));
        }
        graphics.pose().pushPose();
        graphics.pose().translate(center, 31, 0.0F);
        graphics.pose().scale(1.75F, 1.75F, 1.0F);
        graphics.drawCenteredString(client.font, "¡MÁQUINA COMPLETADA!", 0, 0, VeilHudPainter.rgba(0xB7FFDA, Math.round(opacity * 255.0F)));
        graphics.pose().popPose();
        graphics.drawCenteredString(client.font, "¡Completaste! Ahora todos los jugadores", center, 67, VeilHudPainter.rgba(0xFFFFFF, Math.round(opacity * 255.0F)));
        graphics.drawCenteredString(client.font, "diríjanse a la puerta para completar el minijuego.", center, 82, VeilHudPainter.rgba(0xD9F9E9, Math.round(opacity * 255.0F)));
        int lineStart = 30;
        int lineEnd = PANEL_WIDTH - 30;
        int segment = Math.round((lineEnd - lineStart) * Math.min(1.0F, (float) (elapsed / 1.7D)));
        graphics.fill(lineStart, 103, lineStart + segment, 106, VeilHudPainter.rgba(color, Math.round(opacity * 230.0F)));
        graphics.pose().popPose();
    }
}
