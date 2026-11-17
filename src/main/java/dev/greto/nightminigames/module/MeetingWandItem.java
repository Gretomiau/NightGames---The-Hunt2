package dev.greto.nightminigames.module;

import net.minecraft.server.level.ServerPlayer;
import dev.greto.nightminigames.team.TeamBinding;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class MeetingWandItem extends Item {
    public MeetingWandItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide() && context.getPlayer() instanceof ServerPlayer player) {
            ModuleSystem.markMeetingPoint(player, context.getClickedPos().above(), TeamBinding.of(context.getItemInHand()));
        }
        return InteractionResult.SUCCESS;
    }
}
