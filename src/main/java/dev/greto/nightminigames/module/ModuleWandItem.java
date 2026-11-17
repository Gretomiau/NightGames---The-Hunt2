package dev.greto.nightminigames.module;

import dev.greto.nightminigames.key.KeyMessages;
import dev.greto.nightminigames.team.TeamBinding;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.Locale;

public final class ModuleWandItem extends Item {
    private static final String TEAM_TAG = "NightminigamesModuleTeam";

    public ModuleWandItem() {
        super(new Item.Properties().stacksTo(1));
    }

    public static String selectedTeam(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return ModuleSystem.TEAMS.getFirst();
        }
        String selected = data.copyTag().getString(TEAM_TAG);
        String locked = TeamBinding.of(stack);
        return TeamBinding.valid(locked) ? locked : ModuleSystem.TEAMS.contains(selected) ? selected : ModuleSystem.TEAMS.getFirst();
    }

    public static void selectTeam(ItemStack stack, String team) {
        if (!ModuleSystem.TEAMS.contains(team)) {
            return;
        }
        CustomData current = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = current == null ? new CompoundTag() : current.copyTag();
        tag.putString(TEAM_TAG, team);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Varita de módulos · " + team.toUpperCase(Locale.ROOT)).withStyle(teamColor(team)));
    }

    private static ChatFormatting teamColor(String team) {
        return switch (team) {
            case "rojo" -> ChatFormatting.RED;
            case "azul" -> ChatFormatting.BLUE;
            case "verde" -> ChatFormatting.GREEN;
            case "amarillo" -> ChatFormatting.YELLOW;
            case "naranja" -> ChatFormatting.GOLD;
            case "morado" -> ChatFormatting.DARK_PURPLE;
            case "rosa" -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.AQUA;
        };
    }

    private static void nextTeam(ServerPlayer player, ItemStack stack) {
        if (TeamBinding.valid(TeamBinding.of(stack))) {
            dev.greto.nightminigames.admin.AdminMessages.send(player, KeyMessages.message("Esta varita está vinculada al equipo " + TeamBinding.of(stack) + ".", teamColor(TeamBinding.of(stack))));
            return;
        }
        if (!player.hasPermissions(2)) {
            return;
        }
        int index = ModuleSystem.TEAMS.indexOf(selectedTeam(stack));
        String next = ModuleSystem.TEAMS.get((index + 1) % ModuleSystem.TEAMS.size());
        selectTeam(stack, next);
        dev.greto.nightminigames.admin.AdminMessages.send(player, KeyMessages.message("Varita de módulos seleccionada: " + next.toUpperCase(Locale.ROOT) + ". Clic derecho en el suelo para guardar el destino.", teamColor(next)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (!serverPlayer.hasPermissions(2)) {
                dev.greto.nightminigames.admin.AdminMessages.send(serverPlayer, KeyMessages.message("Solo los operadores pueden usar esta varita.", ChatFormatting.RED));
            } else if (player.isShiftKeyDown()) {
                nextTeam(serverPlayer, stack);
            } else {
                dev.greto.nightminigames.admin.AdminMessages.send(serverPlayer, KeyMessages.message("Equipo: " + selectedTeam(stack) + ". Clic derecho sobre el suelo para guardar. Shift + clic derecho para cambiar equipo.", ChatFormatting.AQUA));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide() && context.getPlayer() instanceof ServerPlayer player) {
            if (!player.hasPermissions(2)) {
                dev.greto.nightminigames.admin.AdminMessages.send(player, KeyMessages.message("Solo los operadores pueden configurar módulos.", ChatFormatting.RED));
            } else if (player.isShiftKeyDown()) {
                nextTeam(player, context.getItemInHand());
            } else {
                BlockPos stand = context.getClickedPos().relative(context.getClickedFace());
                ModuleSystem.markModulePoint(player, selectedTeam(context.getItemInHand()), stand);
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }
}
