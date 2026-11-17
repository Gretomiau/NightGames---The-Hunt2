package dev.greto.nightminigames.mixin;

import dev.greto.nightminigames.key.KeyReturnSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerKeyDeathMixin {
    @Inject(method = "dropEquipment", at = @At("HEAD"))
    private void nightminigames$returnKeys(CallbackInfo callback) {
        if ((Object) this instanceof ServerPlayer player) KeyReturnSystem.onDeath(player);
    }
}
