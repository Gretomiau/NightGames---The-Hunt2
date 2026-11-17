package dev.greto.nightminigames.client.mixin;

import dev.greto.nightminigames.client.environment.EnvironmentClientSystem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void nightminigames$sky(Vec3 cameraPos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (EnvironmentClientSystem.isDark()) {
            cir.setReturnValue(new Vec3(0.008D, 0.012D, 0.020D));
        }
    }

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void nightminigames$clouds(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (EnvironmentClientSystem.isDark()) {
            cir.setReturnValue(new Vec3(0.018D, 0.021D, 0.029D));
        }
    }
}
