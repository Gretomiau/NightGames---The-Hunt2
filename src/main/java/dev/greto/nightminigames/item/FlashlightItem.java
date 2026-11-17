package dev.greto.nightminigames.item;

import dev.greto.nightminigames.flashlight.FlashlightData;
import dev.greto.nightminigames.flashlight.FlashlightSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class FlashlightItem extends Item {
    public static final int CHARGE_TICKS = 100;
    public static final int USE_DURATION_TICKS = 72000;

    public FlashlightItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            boolean alreadyEnabled = FlashlightData.isEnabled(stack);
            FlashlightSystem.beginUse(serverPlayer, hand, stack, alreadyEnabled);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        return use(context.getLevel(), player, context.getHand()).getResult();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION_TICKS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingTicks) {
        if (level.isClientSide() || !(living instanceof ServerPlayer player)) {
            return;
        }
        int elapsed = USE_DURATION_TICKS - remainingTicks;
        if (elapsed >= CHARGE_TICKS && !FlashlightData.isEnabled(stack)) {
            FlashlightSystem.completeCharge(player, stack);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int remainingTicks) {
        if (!level.isClientSide() && living instanceof ServerPlayer player) {
            FlashlightSystem.endUse(player);
        }
    }
}
