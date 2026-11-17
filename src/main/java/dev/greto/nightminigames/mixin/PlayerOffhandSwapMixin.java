package dev.greto.nightminigames.mixin;

import dev.greto.nightminigames.inventory.OffhandRestrictionSystem;
import dev.greto.nightminigames.inventory.DropRestrictionSystem;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PlayerOffhandSwapMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void nightminigames$preventSwap(ServerboundPlayerActionPacket packet, CallbackInfo callback) {
        if (!player.server.isSameThread()) {
            return;
        }
        if ((packet.getAction() == ServerboundPlayerActionPacket.Action.DROP_ITEM
            || packet.getAction() == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS)
            && DropRestrictionSystem.isProtected(player.getMainHandItem())) {
            callback.cancel();
            player.containerMenu.broadcastFullState();
            return;
        }
        if (packet.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND
            && OffhandRestrictionSystem.isRestricted(player.getMainHandItem())) {
            callback.cancel();
            player.containerMenu.broadcastFullState();
        }
    }

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true)
    private void nightminigames$preventCreativeDrop(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo callback) {
        if (!player.server.isSameThread()) {
            return;
        }
        if (packet.slotNum() < 0 && DropRestrictionSystem.isProtected(packet.itemStack())) {
            callback.cancel();
            player.containerMenu.broadcastFullState();
        }
    }
}
