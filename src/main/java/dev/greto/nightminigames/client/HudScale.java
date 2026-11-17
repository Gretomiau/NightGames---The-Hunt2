package dev.greto.nightminigames.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class HudScale {
    private static final float DESIGN_PIXEL_SCALE = 2.0F;

    private HudScale() {
    }

    public static float guiScale() {
        return (float) Math.max(1.0D, Minecraft.getInstance().getWindow().getGuiScale());
    }

    public static float pixels(float physicalPixels) {
        return physicalPixels / guiScale();
    }

    public static float reference(float designUnits) {
        return designUnits * DESIGN_PIXEL_SCALE / guiScale();
    }

    public static float fit(GuiGraphics graphics, int designWidth, int designHeight, float horizontalMargin, float verticalMargin) {
        float windowScale = guiScale();
        float physicalWidth = graphics.guiWidth() * windowScale;
        float physicalHeight = graphics.guiHeight() * windowScale;
        float allowedWidth = Math.max(1.0F, physicalWidth - horizontalMargin * 2.0F);
        float allowedHeight = Math.max(1.0F, physicalHeight - verticalMargin * 2.0F);
        float fit = Math.min(1.0F, Math.min(allowedWidth / (designWidth * DESIGN_PIXEL_SCALE), allowedHeight / (designHeight * DESIGN_PIXEL_SCALE)));
        return Math.max(0.01F, DESIGN_PIXEL_SCALE * fit / windowScale);
    }
}
