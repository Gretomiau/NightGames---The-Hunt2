package dev.greto.nightminigames.client.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import dev.greto.nightminigames.client.environment.DarknessMath;
import dev.greto.nightminigames.client.environment.EnvironmentClientSystem;
import dev.greto.nightminigames.client.environment.LightmapTracker;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DynamicTexture.class)
public abstract class DynamicTextureMixin {
    @Shadow private NativeImage pixels;

    @Inject(method = "upload", at = @At("HEAD"))
    private void nightminigames$darken(CallbackInfo ci) {
        if (!EnvironmentClientSystem.isDark() || pixels == null || !LightmapTracker.isLightmap((DynamicTexture) (Object) this)) {
            return;
        }
        int width = Math.min(16, pixels.getWidth());
        int height = Math.min(16, pixels.getHeight());
        for (int block = 0; block < width; block++) {
            for (int sky = 0; sky < height; sky++) {
                pixels.setPixelRGBA(block, sky, DarknessMath.darken(pixels.getPixelRGBA(block, sky), block, sky));
            }
        }
    }
}
