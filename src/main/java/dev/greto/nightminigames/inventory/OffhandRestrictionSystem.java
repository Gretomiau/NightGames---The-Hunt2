package dev.greto.nightminigames.inventory;

import dev.greto.nightminigames.NightminigamesMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class OffhandRestrictionSystem {
    private static boolean initialized;

    private OffhandRestrictionSystem() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                validate(player);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> server.execute(() -> validate(handler.player)));
    }

    public static boolean isRestricted(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(NightminigamesMod.MOD_ID);
    }

    private static void validate(ServerPlayer player) {
        ItemStack offhand = player.getOffhandItem();
        if (!isRestricted(offhand)) {
            return;
        }
        ItemStack recovered = offhand.copy();
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        player.getInventory().placeItemBackInInventory(recovered);
        player.containerMenu.broadcastFullState();
    }
}
