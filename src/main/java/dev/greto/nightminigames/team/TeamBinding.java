package dev.greto.nightminigames.team;

import dev.greto.nightminigames.module.ModuleSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.scores.PlayerTeam;

public final class TeamBinding {
    private static final String TAG = "NightminigamesOwnerTeam";

    private TeamBinding() {
    }

    public static boolean valid(String team) {
        return ModuleSystem.TEAMS.contains(team);
    }

    public static String of(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return "";
        }
        String value = data.copyTag().getString(TAG);
        return valid(value) ? value : "";
    }

    public static void set(ItemStack stack, String team) {
        if (!valid(team)) {
            return;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();
        tag.putString(TAG, team);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static ItemStack label(ItemStack stack, String team) {
        set(stack, team);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(stack.getHoverName().getString() + " · " + team.toUpperCase())
            .withStyle(color(team)));
        return stack;
    }

    public static ChatFormatting color(String team) {
        return switch (team) {
            case "rojo" -> ChatFormatting.RED;
            case "azul" -> ChatFormatting.BLUE;
            case "verde" -> ChatFormatting.GREEN;
            case "amarillo" -> ChatFormatting.YELLOW;
            case "naranja" -> ChatFormatting.GOLD;
            case "morado" -> ChatFormatting.DARK_PURPLE;
            case "rosa" -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.GRAY;
        };
    }

    public static int rgb(String team) {
        return switch (team) {
            case "rojo" -> 0xF25A5A;
            case "azul" -> 0x4C92F5;
            case "verde" -> 0x50CB80;
            case "amarillo" -> 0xE9C644;
            case "naranja" -> 0xEF9944;
            case "morado" -> 0xA477E5;
            case "rosa" -> 0xEE89BE;
            default -> 0x55DDFF;
        };
    }

    public static boolean canInteractWithoutOp(ServerPlayer player, String team) {
        if (team.isBlank()) {
            return true;
        }
        PlayerTeam membership = player.server.getScoreboard().getPlayersTeam(player.getScoreboardName());
        return membership != null && team.equals(membership.getName());
    }

    public static boolean canInteract(ServerPlayer player, String team) {
        if (team.isBlank()) {
            return true;
        }
        if (player.hasPermissions(2)) {
            return true;
        }
        PlayerTeam membership = player.server.getScoreboard().getPlayersTeam(player.getScoreboardName());
        return membership != null && team.equals(membership.getName());
    }

    public static boolean requireKitTeam(ServerPlayer player, ItemStack stack) {
        String team = of(stack);
        if (valid(team)) {
            return true;
        }
        dev.greto.nightminigames.admin.AdminMessages.send(player, dev.greto.nightminigames.key.KeyMessages.message("Usa el kit de un equipo: /nightminigames kit configuracion <equipo>", ChatFormatting.YELLOW));
        return false;
    }
}
