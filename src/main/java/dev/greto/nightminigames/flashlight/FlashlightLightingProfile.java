package dev.greto.nightminigames.flashlight;

public record FlashlightLightingProfile(
    float rangeBlocks,
    float coreBrightness,
    float spillBrightness,
    float coreAngleDegrees,
    float spillAngleDegrees,
    double coreSize,
    double spillSize,
    float spillRangeMultiplier
) {
    public static final FlashlightLightingProfile DEFAULT = new FlashlightLightingProfile(
        48.0F,
        5.0F,
        1.65F,
        28.0F,
        65.0F,
        0.24D,
        0.90D,
        0.90F
    );
}
