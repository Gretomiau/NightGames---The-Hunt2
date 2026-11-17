package dev.greto.nightminigames.client;

import dev.greto.nightminigames.flashlight.FlashlightData;
import dev.greto.nightminigames.item.FlashlightItem;
import dev.greto.nightminigames.registry.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class FlashlightChargeHud {
    private static final int CYAN = 0x59F5FF;
    private static final int PALE = 0xD5FBFF;
    private static boolean initialized;
    private static int lastUseTicks;
    private static boolean blockedUntilRelease;
    private static long completedAtNanos;

    private FlashlightChargeHud() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        HudRenderCallback.EVENT.register(FlashlightChargeHud::render);
    }

    public static void onActivated() {
        completedAtNanos = System.nanoTime();
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.screen != null || client.options.hideGui) {
            lastUseTicks = 0;
            blockedUntilRelease = false;
            return;
        }
        boolean success = System.nanoTime() - completedAtNanos < 600_000_000L;
        int usedTicks = 0;
        if (client.player.isUsingItem()) {
            ItemStack held = client.player.getUseItem();
            if (held.is(ModItems.FLASHLIGHT)) {
                usedTicks = client.player.getTicksUsingItem();
                if (usedTicks < lastUseTicks) {
                    blockedUntilRelease = false;
                }
                if (usedTicks <= 1 && FlashlightData.isEnabled(held)) {
                    blockedUntilRelease = true;
                }
                lastUseTicks = usedTicks;
                if (!success && (FlashlightData.isEnabled(held) || blockedUntilRelease)) {
                    return;
                }
            } else if (!success) {
                lastUseTicks = 0;
                blockedUntilRelease = false;
                return;
            }
        } else if (!success) {
            lastUseTicks = 0;
            blockedUntilRelease = false;
            return;
        }
        float delta = deltaTracker.getGameTimeDeltaPartialTick(true);
        float progress = success ? 1.0F : Mth.clamp((usedTicks + delta) / FlashlightItem.CHARGE_TICKS, 0.0F, 1.0F);
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int panelWidth = 254;
        int panelHeight = 63;
        float scale = HudScale.fit(graphics, panelWidth, panelHeight, 18.0F, 190.0F);
        float panelX = (width - panelWidth * scale) / 2.0F;
        float panelY = Math.max(HudScale.pixels(8.0F), height - HudScale.reference(84.0F) - panelHeight * scale);
        double time = System.nanoTime() * 1.0E-9D;
        boolean veilBackground = VeilHudShader.draw(graphics, panelX, panelY, panelWidth * scale, panelHeight * scale, CYAN, 1.0F, progress, 0, time);
        graphics.pose().pushPose();
        graphics.pose().translate(panelX, panelY, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        if (!veilBackground) {
            VeilHudPainter.drawPanel(graphics, 0, 0, panelWidth, panelHeight, CYAN, 1.0F, time);
        }
        if (!veilBackground) {
            VeilHudPainter.drawEnergyOrb(graphics, 27, 31, 10, CYAN, 1.0F, time);
            VeilHudPainter.drawDropletBurst(graphics, 27, 31, 12, CYAN, 0.75F, time);
        }
        String title = success ? "LINTERNA ACTIVADA" : usedTicks >= FlashlightItem.CHARGE_TICKS ? "VALIDANDO CARGA" : "INICIANDO LINTERNA";
        graphics.drawString(client.font, title, 49, 10, VeilHudPainter.rgba(PALE, 255), false);
        VeilHudPainter.drawSegmentBar(graphics, 18, 36, panelWidth - 36, 11, progress, CYAN, 1.0F, time);
        graphics.drawCenteredString(client.font, success ? "100%  LISTO" : Math.round(progress * 100.0F) + "%", panelWidth / 2, 50, VeilHudPainter.rgba(0xBDF9FF, 255));
        graphics.drawString(client.font, "POWER", 18, 27, VeilHudPainter.rgba(0x6EEBFF, 145), false);
        graphics.drawString(client.font, "HOLD", panelWidth - 42, 27, VeilHudPainter.rgba(0x6EEBFF, 145), false);
        graphics.pose().popPose();
    }
}
