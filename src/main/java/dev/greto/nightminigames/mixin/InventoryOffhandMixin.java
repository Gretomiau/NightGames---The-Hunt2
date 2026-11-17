package dev.greto.nightminigames.mixin;

import dev.greto.nightminigames.inventory.OffhandRestrictionSystem;
import dev.greto.nightminigames.inventory.DropRestrictionSystem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class InventoryOffhandMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void nightminigames$preventOffhand(int slotId, int button, ClickType type, Player player, CallbackInfo callback) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if ((type == ClickType.THROW && slotId >= 0 && slotId < menu.slots.size()
            && DropRestrictionSystem.isProtected(menu.getSlot(slotId).getItem()))
            || (type == ClickType.PICKUP && slotId == -999
            && DropRestrictionSystem.isProtected(menu.getCarried()))) {
            callback.cancel();
            if (!player.level().isClientSide()) {
                menu.broadcastFullState();
            }
            return;
        }
        boolean targetOffhand = menu == player.inventoryMenu && slotId == 45;
        boolean swapToOffhand = type == ClickType.SWAP && button == 40 && slotId >= 0 && slotId < menu.slots.size();
        if (!targetOffhand && !swapToOffhand) {
            return;
        }
        boolean forbidden = false;
        if (targetOffhand) {
            if (type == ClickType.SWAP && button >= 0 && button < 9) {
                forbidden = OffhandRestrictionSystem.isRestricted(player.getInventory().getItem(button));
            } else {
                forbidden = OffhandRestrictionSystem.isRestricted(menu.getCarried());
            }
        } else if (swapToOffhand) {
            forbidden = OffhandRestrictionSystem.isRestricted(menu.getSlot(slotId).getItem());
        }
        if (forbidden) {
            callback.cancel();
            if (!player.level().isClientSide()) {
                menu.broadcastFullState();
            }
        }
    }
}
