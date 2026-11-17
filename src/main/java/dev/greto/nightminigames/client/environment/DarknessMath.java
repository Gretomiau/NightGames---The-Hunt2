package dev.greto.nightminigames.client.environment;

public final class DarknessMath {
    private DarknessMath() {
    }

    public static int darken(int color, int blockIndex, int skyIndex) {
        if (blockIndex == 15 && skyIndex == 15) {
            return color;
        }
        float block = Math.clamp(blockIndex / 15.0F, 0.0F, 1.0F);
        float sky = Math.clamp(skyIndex / 15.0F, 0.0F, 1.0F);
        float blockSquared = block * block;
        float skySquared = sky * sky;
        float retained = 0.006F + 0.065F * blockSquared * blockSquared + 0.008F * skySquared * skySquared * skySquared;
        int red = Math.min(255, Math.max(0, Math.round((color & 255) * retained)));
        int green = Math.min(255, Math.max(0, Math.round(((color >>> 8) & 255) * retained)));
        int blue = Math.min(255, Math.max(0, Math.round(((color >>> 16) & 255) * retained)));
        return (color & 0xFF000000) | (blue << 16) | (green << 8) | red;
    }
}
