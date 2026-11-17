package dev.greto.nightminigames.client.machine;

import dev.greto.nightminigames.key.KeyColor;
import dev.greto.nightminigames.client.VeilHudShader;
import dev.greto.nightminigames.client.VeilHudPainter;
import dev.greto.nightminigames.client.HudScale;
import dev.greto.nightminigames.machine.MachineProgressPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayDeque;
import java.util.Locale;

public final class MachineProgressHud {
    private static final double NOTICE_SECONDS = 4.4D;
    private static final int PANEL_WIDTH = 290;
    private static final int PANEL_HEIGHT = 78;
    private static final int DETAIL_HEIGHT = 109;
    private static final ArrayDeque<MachineProgressPayload> QUEUE = new ArrayDeque<>();
    private static MachineProgressPayload active;
    private static long startedAt;
    private static boolean initialized;

    private MachineProgressHud() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientPlayNetworking.registerGlobalReceiver(MachineProgressPayload.TYPE, (payload, context) ->
            context.client().execute(() -> {
                if (payload.removed()) {
                    return;
                }
                if (payload.eventColor() >= 0 && payload.eventColor() < 4 && (payload.mask() & 15) == 15) {
                    MachineCompleteVignette.activate();
                }
                if (QUEUE.size() < 8) {
                    QUEUE.addLast(payload);
                }
            })
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
        HudRenderCallback.EVENT.register(MachineProgressHud::render);
    }

    private static void clear() {
        QUEUE.clear();
        active = null;
        startedAt = 0L;
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            clear();
            return;
        }
        long now = System.nanoTime();
        if (active != null && (now - startedAt) / 1_000_000_000.0D >= NOTICE_SECONDS) {
            active = null;
        }
        if (active == null && !QUEUE.isEmpty()) {
            active = QUEUE.removeFirst();
            startedAt = now;
        }
        if (active == null || client.options.hideGui || client.screen != null) {
            return;
        }

        double time = (now - startedAt) / 1_000_000_000.0D;
        float appear = ease(time / 0.24D);
        float disappear = 1.0F - ease((time - (NOTICE_SECONDS - 0.55D)) / 0.55D);
        float opacity = Math.clamp(Math.min(appear, disappear), 0.0F, 1.0F);
        if (opacity <= 0.0F) {
            return;
        }

        boolean statusView = active.eventColor() == -1;
        int panelHeight = statusView ? DETAIL_HEIGHT : PANEL_HEIGHT;
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        float scale = HudScale.fit(graphics, PANEL_WIDTH, panelHeight, 16.0F, 24.0F);
        float displayWidth = PANEL_WIDTH * scale;
        float x = Math.max(HudScale.pixels(8.0F), screenWidth - displayWidth - HudScale.pixels(10.0F)) + (1.0F - appear) * 36.0F * scale;
        float y = Math.max(HudScale.pixels(7.0F), Math.min(HudScale.reference(24.0F), screenHeight * 0.085F));
        int accent = accent(active);
        float progress = Integer.bitCount(active.mask() & 15) / 4.0F;
        boolean veilBackground = VeilHudShader.draw(graphics, x, y, PANEL_WIDTH * scale, panelHeight * scale, accent, opacity, progress, 2, time);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);

        if (!veilBackground) {
            VeilHudPainter.drawPanel(graphics, 0, 0, PANEL_WIDTH, panelHeight, accent, opacity, time);
        }

        String title = title(active);
        graphics.drawString(client.font, title, 13, 10, rgba(accent, (int) (255.0F * opacity)), false);
        graphics.drawString(client.font, detail(active), 13, 27, rgba(0xE8F5FF, (int) (255.0F * opacity)), false);

        if (statusView) {
            for (int i = 0; i < 4; i++) {
                KeyColor key = KeyColor.values()[i];
                boolean inserted = (active.mask() & (1 << i)) != 0;
                int cx = 13 + (i % 2) * 139;
                int cy = 46 + (i / 2) * 17;
                graphics.fill(cx, cy + 2, cx + 5, cy + 7,
                    rgba(inserted ? key.rgb : 0x4F6274, (int) (255.0F * opacity)));
                String name = inserted ? active.inserter(i) : "PENDIENTE";
                if (name.length() > 11) {
                    name = name.substring(0, 10) + "…";
                }
                graphics.drawString(client.font,
                    key.display.toUpperCase(Locale.ROOT) + ": " + name,
                    cx + 8, cy, rgba(inserted ? 0xD5F8FF : 0x7D90A2, (int) (255.0F * opacity)), false);
            }
        } else {
            int left = 13;
            for (int i = 0; i < 4; i++) {
                KeyColor key = KeyColor.values()[i];
                int segmentX = left + i * 32;
                boolean inserted = (active.mask() & (1 << i)) != 0;
                graphics.fill(segmentX, 48, segmentX + 26, 54,
                    rgba(inserted ? key.rgb : 0x354B5F, (int) (255.0F * opacity)));
            }
        }

        int inserted = Integer.bitCount(active.mask() & 15);
        String footer = inserted + "/4 COLOCADAS  |  FALTAN " + (4 - inserted);
        graphics.drawString(client.font, footer, 13, panelHeight - 16,
            rgba(0xE2FAFF, (int) (255.0F * opacity)), false);
        float timeline = Math.clamp((float) (time / NOTICE_SECONDS), 0.0F, 1.0F);
        graphics.fill(0, panelHeight - 2, Math.round(PANEL_WIDTH * (1.0F - timeline)), panelHeight,
            rgba(accent, (int) (255.0F * opacity)));
        graphics.pose().popPose();
    }

    private static String title(MachineProgressPayload payload) {
        if (payload.eventColor() == -2) {
            return "MAQUINA REINICIADA";
        }
        if (payload.eventColor() == -3) {
            return "LLAVE YA COLOCADA";
        }
        if (payload.eventColor() == -1) {
            return "ESTADO DE LA MAQUINA";
        }
        if ((payload.mask() & 15) == 15) {
            return "MAQUINA COMPLETADA";
        }
        if (payload.eventColor() >= 0 && payload.eventColor() < KeyColor.values().length) {
            return "LLAVE " + KeyColor.values()[payload.eventColor()].display.toUpperCase(Locale.ROOT) + " INSERTADA";
        }
        return "PROGRESO DE LA MAQUINA";
    }

    private static String detail(MachineProgressPayload payload) {
        if (payload.eventColor() == -2) {
            return "Las cuatro cerraduras estan disponibles";
        }
        if (payload.eventColor() == -3) {
            return "Esta cerradura ya esta desbloqueada";
        }
        if (payload.eventColor() == -1) {
            return "Llaves colocadas y sus jugadores:";
        }
        if (payload.eventColor() >= 0 && payload.eventColor() < 4) {
            return payload.actor() + " coloco esta llave";
        }
        return "Progreso actualizado";
    }

    private static int accent(MachineProgressPayload payload) {
        if (payload.eventColor() >= 0 && payload.eventColor() < 4) {
            return (payload.mask() & 15) == 15 ? 0x6EF2B0 : KeyColor.values()[payload.eventColor()].rgb;
        }
        if (payload.eventColor() == -3) {
            return 0xFFC65A;
        }
        return 0x53DDED;
    }

    private static float ease(double value) {
        float t = Math.clamp((float) value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static int rgba(int rgb, int alpha) {
        return (Math.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }
}
