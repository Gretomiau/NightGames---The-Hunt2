package dev.greto.nightminigames.region;

import net.minecraft.server.level.ServerPlayer;
import dev.greto.nightminigames.team.TeamBinding;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class RegionSelectionWandItem extends Item {
    private final RegionKind region;

    public RegionSelectionWandItem(RegionKind region) {
        super(new Item.Properties().stacksTo(1));
        this.region = region;
    }

    public RegionKind region() {
        return region;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide() && context.getPlayer() instanceof ServerPlayer player) {
            RegionSelectionSystem.select(player, TeamBinding.of(context.getItemInHand()), region, false, context.getClickedPos());
        }
        return InteractionResult.SUCCESS;
    }
}
