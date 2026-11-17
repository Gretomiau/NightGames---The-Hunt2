package dev.greto.nightminigames.inventory;

import dev.greto.nightminigames.registry.ModItems;
import net.minecraft.world.item.ItemStack;

public final class DropRestrictionSystem {
    private DropRestrictionSystem() {
    }

    public static boolean isProtected(ItemStack stack) {
        return !stack.isEmpty() && (
            stack.is(ModItems.FLASHLIGHT)
                || stack.is(ModItems.KEY_BLUE)
                || stack.is(ModItems.KEY_YELLOW)
                || stack.is(ModItems.KEY_RED)
                || stack.is(ModItems.KEY_GREEN)
        );
    }
}
