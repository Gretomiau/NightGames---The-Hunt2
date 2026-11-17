package dev.greto.nightminigames.key;

import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.persistence.WorldStructureJson;
import dev.greto.nightminigames.team.TeamBinding;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.UUID;

/** Origin travels with the actual key. No per-tick scan or permanent chunk tickets. */
public final class KeyReturnSystem {
    private static final String ORIGIN = "NightminigamesKeyOrigin";

    private KeyReturnSystem() {}

    public static void bind(ItemStack key, ServerLevel level, BlockPos pos, KeySpawnBlockEntity spawner) {
        String token = UUID.randomUUID().toString();
        spawner.setPickupToken(token);
        CompoundTag origin = new CompoundTag();
        origin.putString("dimension", level.dimension().location().toString());
        origin.putLong("position", pos.asLong());
        origin.putString("token", token);
        origin.putString("team", spawner.ownerTeam());
        origin.putLong("round", spawner.roundGeneration());
        origin.putLong("generation", spawner.spawnerGeneration());
        CustomData data = key.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
        tag.put(ORIGIN, origin);
        key.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Called by vanilla's death equipment path, before keepInventory and dropping. */
    public static void onDeath(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (returnKey(player, stack)) player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        if (returnKey(player, player.containerMenu.getCarried())) {
            player.containerMenu.setCarried(ItemStack.EMPTY);
        }
        player.getInventory().setChanged();
    }

    private static KeyColor color(ItemStack key) {
        for (KeyColor color : KeyColor.values()) if (key.is(color.keyItem())) return color;
        return null;
    }

    private static boolean returnKey(ServerPlayer player, ItemStack key) {
        KeyColor color = color(key);
        if (color == null) return false;
        CustomData custom = key.get(DataComponents.CUSTOM_DATA);
        if (custom == null || !custom.copyTag().contains(ORIGIN, 10)) return false;
        CompoundTag origin = custom.copyTag().getCompound(ORIGIN);
        String dimension = origin.getString("dimension");
        BlockPos pos = BlockPos.of(origin.getLong("position"));
        String token = origin.getString("token");
        if (token.isEmpty()) return false;
        boolean restored = false;
        try {
            ServerLevel level = null;
            for (ServerLevel candidate : player.server.getAllLevels()) {
                if (candidate.dimension().location().toString().equals(dimension)) {
                    level = candidate;
                    break;
                }
            }
            if (level == null) {
                warn(player, "no existe la dimensión de origen " + dimension);
                return false;
            }
            // Load only on death, using vanilla's normal loading, with no forced ticket.
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof KeySpawnBlock block) || block.color != color
                || !(level.getBlockEntity(pos) instanceof KeySpawnBlockEntity spawner)) {
                warn(player, "ya no existe su spawner en " + dimension + " " + pos.toShortString());
                return false;
            }
            // Old-round keys and old pickup copies cannot reactivate a new pickup.
            if (!token.equals(spawner.pickupToken())
                || origin.getLong("round") != spawner.roundGeneration()
                || origin.getLong("generation") != spawner.spawnerGeneration()
                || !origin.getString("team").equals(spawner.ownerTeam())) return true;
            if (state.getValue(KeySpawnBlock.COLLECTED)) {
                if (!level.setBlock(pos, state.setValue(KeySpawnBlock.COLLECTED, false), 3)) {
                    warn(player, "no se pudo reactivar el spawner " + pos.toShortString());
                    return false;
                }
                restored = true;
                WorldStructureJson.markBlockStateChanged(level, pos);
            }
            restored = true;
            spawner.setPickupToken("");
            String team = spawner.ownerTeam();
            String message = "Llave " + color.display + " devuelta a su spawner porque murió " + player.getGameProfile().getName() + ".";
            for (ServerPlayer recipient : player.server.getPlayerList().getPlayers()) {
                if (recipient.hasPermissions(2) || !team.isBlank() && TeamBinding.canInteractWithoutOp(recipient, team)) {
                    recipient.sendSystemMessage(KeyMessages.message(message, ChatFormatting.YELLOW));
                }
            }
            NightminigamesMod.LOGGER.info("[Nightminigames] {} Origen: {} {}", message, dimension, pos.toShortString());
            return true;
        } catch (RuntimeException exception) {
            NightminigamesMod.LOGGER.error("[Nightminigames] No se pudo devolver la llave de {} a {} {}", player.getGameProfile().getName(), dimension, pos, exception);
            if (restored) return true; // Never leave the key behind after reactivating its source.
            warn(player, "falló la devolución; consulta el log del servidor");
            return false;
        }
    }

    private static void warn(ServerPlayer player, String reason) {
        String message = "No se devolvió una llave de " + player.getGameProfile().getName() + ": " + reason + ". Se conserva el comportamiento normal de caída para no borrarla.";
        NightminigamesMod.LOGGER.warn("[Nightminigames] {}", message);
        for (ServerPlayer recipient : player.server.getPlayerList().getPlayers()) {
            if (recipient.hasPermissions(2)) recipient.sendSystemMessage(KeyMessages.message(message, ChatFormatting.RED));
        }
    }
}
