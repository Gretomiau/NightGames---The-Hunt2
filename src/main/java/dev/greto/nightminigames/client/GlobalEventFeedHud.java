package dev.greto.nightminigames.client;

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

public final class GlobalEventFeedHud {
    private static final int MAX_HISTORY = 120;
    private static final int TEXT_WIDTH = 356;
    private static final int MAX_VISIBLE_ROWS = 7;
    private static final int ROW_HEIGHT = 13;
    private static final List<HudFeedPayload.Entry> HISTORY = new ArrayList<>();
    private static boolean initialized;

    private GlobalEventFeedHud() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientPlayNetworking.registerGlobalReceiver(HudFeedPayload.TYPE, (payload, context) -> context.client().execute(() -> apply(payload)));
        HudRenderCallback.EVENT.register(GlobalEventFeedHud::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> HISTORY.clear());
    }

    private static void apply(HudFeedPayload payload) {
        if (payload.replace()) {
            HISTORY.clear();
        }
        for (HudFeedPayload.Entry entry : payload.entries()) {
            if (HISTORY.size() >= MAX_HISTORY) {
                HISTORY.remove(0);
            }
            HISTORY.add(normalize(entry));
        }
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || client.options.hideGui || client.screen != null || HISTORY.isEmpty()) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (width < 48 || height < 48) {
            return;
        }
        float scale = HudScale.fit(graphics, TEXT_WIDTH, ROW_HEIGHT * MAX_VISIBLE_ROWS, 14.0F, 30.0F);
        int visible = visibleRows(width, height, scale);
        int from = HISTORY.size() - visible;
        float x = HudScale.pixels(16.0F);
        float y = HudScale.pixels(16.0F);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        Font font = client.font;
        for (int i = from; i < HISTORY.size(); i++) {
            HudFeedPayload.Entry entry = HISTORY.get(i);
            int row = (i - from) * ROW_HEIGHT;
            int next = drawPart(graphics, font, entry.subject(), 0, row, 0x55DAF7);
            next = drawPart(graphics, font, entry.verb(), next, row, 0xF2F8FC);
            drawPart(graphics, font, entry.highlight(), next, row, entry.rgb());
        }
        graphics.pose().popPose();
    }

    public static int bottomEdge(GuiGraphics graphics) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float scale = HudScale.fit(graphics, TEXT_WIDTH, ROW_HEIGHT * MAX_VISIBLE_ROWS, 14.0F, 30.0F);
        return Math.round(HudScale.pixels(16.0F) + visibleRows(width, height, scale) * ROW_HEIGHT * scale);
    }

    private static int visibleRows(int width, int height, float scale) {
        int maxRows = Math.max(1, (int) ((height * 0.48F - HudScale.pixels(24.0F)) / (ROW_HEIGHT * scale)));
        return Math.min(HISTORY.size(), Math.min(MAX_VISIBLE_ROWS, maxRows));
    }

    private static HudFeedPayload.Entry normalize(HudFeedPayload.Entry entry) {
        if (!entry.verb().contains("insertó") && !entry.subject().equals("MÁQUINA")) {
            return entry;
        }
        if (entry.subject().equals("MÁQUINA") && entry.highlight().contains("/4 · FALTAN ")) {
            return entry;
        }
        int count = -1;
        int index = entry.subtitle().indexOf("/4");
        if (index > 0) {
            char digit = entry.subtitle().charAt(index - 1);
            if (digit >= '0' && digit <= '4') {
                count = digit - '0';
            }
        }
        if (count < 0) {
            return entry;
        }
        return new HudFeedPayload.Entry("MÁQUINA", " ", count + "/4 · FALTAN " + (4 - count), "", count == 4 ? 0x61E8A0 : 0x5BDEEF);
    }

    private static int drawPart(GuiGraphics graphics, Font font, String text, int x, int y, int rgb) {
        int available = TEXT_WIDTH - x;
        if (available <= 0) {
            return x;
        }
        String visible = font.plainSubstrByWidth(text, available);
        graphics.drawString(font, visible, x, y, 0xFF000000 | (rgb & 0xFFFFFF), true);
        return x + font.width(visible);
    }
}
