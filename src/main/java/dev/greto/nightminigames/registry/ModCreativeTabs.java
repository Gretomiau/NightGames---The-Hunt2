package dev.greto.nightminigames.registry;

import dev.greto.nightminigames.NightminigamesMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {
    public static final ResourceKey<CreativeModeTab> NIGHTMINIGAMES_KEY = ResourceKey.create(
        BuiltInRegistries.CREATIVE_MODE_TAB.key(),
        NightminigamesMod.id("main")
    );

    private static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES_KEY = ResourceKey.create(
        BuiltInRegistries.CREATIVE_MODE_TAB.key(),
        ResourceLocation.fromNamespaceAndPath("minecraft", "tools_and_utilities")
    );

    public static final CreativeModeTab NIGHTMINIGAMES = FabricItemGroup.builder()
        .icon(() -> new ItemStack(ModItems.FLASHLIGHT))
        .title(Component.translatable("itemGroup.nightminigames"))
        .displayItems((context, output) -> {
            output.accept(ModItems.FLASHLIGHT);
            output.accept(ModItems.WAND_FLASHLIGHT);
            output.accept(ModItems.KEY_BLUE);
            output.accept(ModItems.KEY_YELLOW);
            output.accept(ModItems.KEY_RED);
            output.accept(ModItems.KEY_GREEN);
            output.accept(ModItems.WAND_BLUE);
            output.accept(ModItems.WAND_YELLOW);
            output.accept(ModItems.WAND_RED);
            output.accept(ModItems.WAND_GREEN);
            output.accept(ModItems.MACHINE);
            output.accept(ModItems.KILL);
            output.accept(ModItems.WAND_MODULOS);
            output.accept(ModItems.WAND_REGION_ENTRADA);
            output.accept(ModItems.WAND_REGION_SALIDA);
            output.accept(ModItems.DOOR_ENTRANCE);
            output.accept(ModItems.DOOR_EXIT);
        })
        .build();

    private ModCreativeTabs() {
    }

    public static void initialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, NIGHTMINIGAMES_KEY, NIGHTMINIGAMES);
        ItemGroupEvents.modifyEntriesEvent(TOOLS_AND_UTILITIES_KEY)
            .register(entries -> entries.accept(ModItems.FLASHLIGHT));
    }
}
