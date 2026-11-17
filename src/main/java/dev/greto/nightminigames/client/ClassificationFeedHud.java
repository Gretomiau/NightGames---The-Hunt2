package dev.greto.nightminigames.client;

import dev.greto.nightminigames.feed.ClassificationFeedPayload;
import dev.greto.nightminigames.feed.HudFeedPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public final class ClassificationFeedHud {
    private static final int MAX_HISTORY = 20;
    private static final int TEXT_WIDTH = 356;
    private static final int ROW_HEIGHT = 13;
    private static final List<HudFeedPayload.Entry> HISTORY = new ArrayList<>();
    private static int visibleLimit = 5;
    private static boolean initialized;

    private ClassificationFeedHud() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientPlayNetworking.registerGlobalReceiver(ClassificationFeedPayload.TYPE, (payload, context) -> context.client().execute(() -> apply(payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        HudRenderCallback.EVENT.register(ClassificationFeedHud::render);
    }

    private static void reset() {
        HISTORY.clear();
        visibleLimit = 5;
    }

    private static void apply(ClassificationFeedPayload payload) {
        if (payload.replace()) {
            HISTORY.clear();
        }
        visibleLimit = Math.clamp(payload.visibleLimit(), 1, 8);
        for (HudFeedPayload.Entry entry : payload.entries()) {
            if (HISTORY.size() >= MAX_HISTORY) {
                HISTORY.remove(0);
            }
            HISTORY.add(entry);
        }
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || client.options.hideGui || client.screen != null || HISTORY.isEmpty()) {
            return;
        }
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        if (screenWidth < 48 || screenHeight < 48) {
            return;
        }
        float scale = HudScale.fit(graphics, TEXT_WIDTH, ROW_HEIGHT * 8, 14.0F, 30.0F);
        float y = GlobalEventFeedHud.bottomEdge(graphics) + HudScale.pixels(22.0F);
        int available = Math.max(0, (int) ((screenHeight - y - HudScale.pixels(45.0F)) / (ROW_HEIGHT * scale)));
        int visible = Math.min(HISTORY.size(), Math.min(visibleLimit, available));
        if (visible == 0) {
            return;
        }
        int from = HISTORY.size() - visible;
        graphics.pose().pushPose();
        graphics.pose().translate(HudScale.pixels(16.0F), y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        Font font = client.font;
        for (int index = from; index < HISTORY.size(); index++) {
            HudFeedPayload.Entry entry = HISTORY.get(index);
            int row = (index - from) * ROW_HEIGHT;
            int next = drawPart(graphics, font, entry.subject(), 0, row, entry.rgb());
            next = drawPart(graphics, font, entry.verb(), next, row, 0xF2F8FC);
            drawPart(graphics, font, entry.highlight(), next, row, 0xA5EAF3);
        }
        graphics.pose().popPose();
    }

    private static int drawPart(GuiGraphics graphics, Font font, String text, int x, int y, int rgb) {
        if (x >= TEXT_WIDTH) {
            return x;
        }
        String shown = font.plainSubstrByWidth(text, TEXT_WIDTH - x);
        graphics.drawString(font, shown, x, y, 0xFF000000 | (rgb & 0xFFFFFF), true);
        return x + font.width(shown);
    }
}
