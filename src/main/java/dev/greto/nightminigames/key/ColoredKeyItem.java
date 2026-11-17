package dev.greto.nightminigames.key;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class ColoredKeyItem extends Item implements GeoItem {
    private final String color;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private GeoRenderProvider renderProvider;

    public ColoredKeyItem(String color) {
        super(new Item.Properties().stacksTo(1));
        this.color = color;
    }

    public String getColor() {
        return color;
    }

    public void setRenderProvider(GeoRenderProvider renderProvider) {
        this.renderProvider = Objects.requireNonNull(renderProvider);
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(Objects.requireNonNull(renderProvider, "El renderizador de la llave no esta registrado en el cliente"));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }
}
