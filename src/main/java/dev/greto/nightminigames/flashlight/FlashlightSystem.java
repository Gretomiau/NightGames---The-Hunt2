package dev.greto.nightminigames.flashlight;

import dev.greto.nightminigames.registry.ModItems;
import dev.greto.nightminigames.registry.ModSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FlashlightSystem {
    private static final Set<UUID> ACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<UUID, Integer> BATTERY_COUNTERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, String> ACTIVE_DIMENSIONS = new ConcurrentHashMap<>();
    private static final Set<UUID> CHARGING_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> OFF_HOLD_PLAYERS = ConcurrentHashMap.newKeySet();
    private static boolean initialized;

    private FlashlightSystem() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        PayloadTypeRegistry.playS2C().register(FlashlightStatePayload.TYPE, FlashlightStatePayload.CODEC);
        FlashlightSettingsSystem.initialize();
        ServerTickEvents.END_SERVER_TICK.register(FlashlightSystem::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendJoinState(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.player.getUUID();
            ACTIVE_PLAYERS.remove(playerId);
            ACTIVE_DIMENSIONS.remove(playerId);
            BATTERY_COUNTERS.remove(playerId);
            CHARGING_PLAYERS.remove(playerId);
            OFF_HOLD_PLAYERS.remove(playerId);
            broadcastState(server, playerId, false);
        });
    }

    public static int rechargeAll(ServerPlayer player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() == ModItems.FLASHLIGHT) {
                stack.setDamageValue(0);
                count++;
            }
        }
        if (count > 0) {
            BATTERY_COUNTERS.remove(player.getUUID());
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        return count;
    }

    public static void beginUse(ServerPlayer player, InteractionHand hand, ItemStack stack, boolean alreadyEnabled) {
        UUID playerId = player.getUUID();
        CHARGING_PLAYERS.remove(playerId);
        OFF_HOLD_PLAYERS.remove(playerId);
        if (alreadyEnabled) {
            OFF_HOLD_PLAYERS.add(playerId);
            toggle(player, hand, stack);
        } else if (stack.getDamageValue() < stack.getMaxDamage()) {
            CHARGING_PLAYERS.add(playerId);
        }
    }

    public static void completeCharge(ServerPlayer player, ItemStack stack) {
        UUID playerId = player.getUUID();
        if (!CHARGING_PLAYERS.remove(playerId) || OFF_HOLD_PLAYERS.contains(playerId)) {
            return;
        }
        if (!player.isUsingItem() || player.getUseItem() != stack || stack.getDamageValue() >= stack.getMaxDamage()) {
            return;
        }
        if (!FlashlightData.isEnabled(stack)) {
            toggle(player, player.getUsedItemHand(), stack);
        }
    }

    public static void endUse(ServerPlayer player) {
        UUID playerId = player.getUUID();
        CHARGING_PLAYERS.remove(playerId);
        OFF_HOLD_PLAYERS.remove(playerId);
    }

    public static void toggle(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != ModItems.FLASHLIGHT) {
            return;
        }
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            FlashlightData.setEnabled(stack, false);
            setActive(player.server, player.getUUID(), false);
            return;
        }
        boolean enabled = !FlashlightData.isEnabled(stack);
        FlashlightData.setEnabled(stack, enabled);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FLASHLIGHT_TOGGLE, SoundSource.MASTER, 0.85F, enabled ? 1.0F : 0.92F);
        if (enabled) {
            ACTIVE_PLAYERS.add(player.getUUID());
            ACTIVE_DIMENSIONS.put(player.getUUID(), player.serverLevel().dimension().location().toString());
            BATTERY_COUNTERS.putIfAbsent(player.getUUID(), 0);
        } else {
            ACTIVE_PLAYERS.remove(player.getUUID());
            ACTIVE_DIMENSIONS.remove(player.getUUID());
            BATTERY_COUNTERS.remove(player.getUUID());
        }
        broadcastState(player.server, player.getUUID(), enabled);
    }

    private static void tick(MinecraftServer server) {
        Set<UUID> seen = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ActiveFlashlight activeFlashlight = findEnabledFlashlight(player);
            UUID playerId = player.getUUID();
            if (!player.isUsingItem() || !player.getUseItem().is(ModItems.FLASHLIGHT)) {
                CHARGING_PLAYERS.remove(playerId);
                OFF_HOLD_PLAYERS.remove(playerId);
            }
            if (activeFlashlight != null) {
                seen.add(playerId);
                String dimension = player.serverLevel().dimension().location().toString();
                String previousDimension = ACTIVE_DIMENSIONS.put(playerId, dimension);
                if (ACTIVE_PLAYERS.add(playerId) || !dimension.equals(previousDimension)) {
                    broadcastState(server, playerId, true);
                }
                int next = BATTERY_COUNTERS.getOrDefault(playerId, 0) + 1;
                if (next >= 20) {
                    next = 0;
                    drainBattery(server, player, activeFlashlight);
                }
                if (ACTIVE_PLAYERS.contains(playerId)) {
                    BATTERY_COUNTERS.put(playerId, next);
                }
            } else {
                BATTERY_COUNTERS.remove(playerId);
                ACTIVE_DIMENSIONS.remove(playerId);
                if (ACTIVE_PLAYERS.remove(playerId)) {
                    broadcastState(server, playerId, false);
                }
            }
        }
        ACTIVE_PLAYERS.removeIf(playerId -> {
            if (seen.contains(playerId)) {
                return false;
            }
            BATTERY_COUNTERS.remove(playerId);
            ACTIVE_DIMENSIONS.remove(playerId);
            broadcastState(server, playerId, false);
            return true;
        });
    }

    private static void drainBattery(MinecraftServer server, ServerPlayer player, ActiveFlashlight activeFlashlight) {
        ItemStack stack = activeFlashlight.stack();
        int newDamage = stack.getDamageValue() + 1;
        if (newDamage >= stack.getMaxDamage()) {
            stack.setDamageValue(stack.getMaxDamage());
            FlashlightData.setEnabled(stack, false);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.FLASHLIGHT_TOGGLE, SoundSource.MASTER, 0.75F, 0.84F);
            ACTIVE_PLAYERS.remove(player.getUUID());
            ACTIVE_DIMENSIONS.remove(player.getUUID());
            BATTERY_COUNTERS.remove(player.getUUID());
            broadcastState(server, player.getUUID(), false);
            return;
        }
        stack.setDamageValue(newDamage);
        if (activeFlashlight.hand() == InteractionHand.MAIN_HAND) {
            player.setItemSlot(EquipmentSlot.MAINHAND, stack);
        } else {
            player.setItemSlot(EquipmentSlot.OFFHAND, stack);
        }
    }

    private static ActiveFlashlight findEnabledFlashlight(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() == ModItems.FLASHLIGHT && FlashlightData.isEnabled(mainHand) && mainHand.getDamageValue() < mainHand.getMaxDamage()) {
            return new ActiveFlashlight(InteractionHand.MAIN_HAND, mainHand);
        }
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() == ModItems.FLASHLIGHT && FlashlightData.isEnabled(offHand) && offHand.getDamageValue() < offHand.getMaxDamage()) {
            return new ActiveFlashlight(InteractionHand.OFF_HAND, offHand);
        }
        return null;
    }

    private static void sendJoinState(ServerPlayer player) {
        for (UUID activePlayer : ACTIVE_PLAYERS) {
            String dimension = ACTIVE_DIMENSIONS.get(activePlayer);
            if (dimension != null) {
                ServerPlayNetworking.send(player, new FlashlightStatePayload(activePlayer, dimension, true));
            }
        }
    }

    private static void broadcastState(MinecraftServer server, UUID playerId, boolean active) {
        String dimension = active ? ACTIVE_DIMENSIONS.getOrDefault(playerId, "") : "";
        FlashlightStatePayload payload = new FlashlightStatePayload(playerId, dimension, active);
        for (ServerPlayer player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static void setActive(MinecraftServer server, UUID playerId, boolean active) {
        if (active) {
            ACTIVE_PLAYERS.add(playerId);
        } else {
            ACTIVE_PLAYERS.remove(playerId);
            ACTIVE_DIMENSIONS.remove(playerId);
            BATTERY_COUNTERS.remove(playerId);
        }
        broadcastState(server, playerId, active);
    }

    private record ActiveFlashlight(InteractionHand hand, ItemStack stack) {
    }
}
