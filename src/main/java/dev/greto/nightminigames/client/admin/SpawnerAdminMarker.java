package dev.greto.nightminigames.client.admin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;

public final class SpawnerAdminMarker {
    private static final int LIGHT = 0x00F000F0;

    private SpawnerAdminMarker() {
    }

    public static void renderSpawner(PoseStack pose, MultiBufferSource buffers, float height, int color, String title, boolean collected, String team) {
        if (!AdminShowClient.creative() || !AdminShowClient.visible(team)) {
            return;
        }
        renderIcon(pose, buffers, height, collected ? 0xAAAAAA : color, collected ? "×" : "◆");
        if (AdminShowClient.isEnabled()) {
            renderText(pose, buffers, height + 0.34F, collected ? 0xAAAAAA : color, title + (collected ? " · AGOTADO" : ""));
        }
    }

    public static void renderWhenShown(PoseStack pose, MultiBufferSource buffers, float height, int color, String text, String team) {
        if (!AdminShowClient.isEnabled() || !AdminShowClient.creative() || !AdminShowClient.visible(team)) {
            return;
        }
        renderIcon(pose, buffers, height, color, "◆");
        renderText(pose, buffers, height + 0.33F, color, text);
    }

    private static void renderIcon(PoseStack pose, MultiBufferSource buffers, float height, int color, String symbol) {
        renderText(pose, buffers, height, color, symbol);
    }

    private static void renderText(PoseStack pose, MultiBufferSource buffers, float height, int color, String text) {
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        pose.pushPose();
        pose.translate(0.5D, height, 0.5D);
        pose.mulPose(client.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(0.024F, -0.024F, 0.024F);
        font.drawInBatch(text, -font.width(text) * 0.5F, 0.0F, (color & 0xFFFFFF) | 0xFF000000, true,
            pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LIGHT);
        pose.popPose();
    }
}
