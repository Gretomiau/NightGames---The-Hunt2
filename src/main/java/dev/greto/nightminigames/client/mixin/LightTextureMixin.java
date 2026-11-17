package dev.greto.nightminigames.client.mixin;

import dev.greto.nightminigames.client.environment.EnvironmentClientSystem;
import dev.greto.nightminigames.client.environment.LightmapTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    @Shadow @Final private DynamicTexture lightTexture;
    @Shadow private boolean updateLightTexture;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nightminigames$track(GameRenderer renderer, Minecraft client, CallbackInfo ci) {
        LightmapTracker.mark(lightTexture);
    }

    @Inject(method = "updateLightTexture", at = @At("HEAD"))
    private void nightminigames$refresh(float partialTick, CallbackInfo ci) {
        if (EnvironmentClientSystem.consumeRefresh()) {
            updateLightTexture = true;
        }
    }
}
