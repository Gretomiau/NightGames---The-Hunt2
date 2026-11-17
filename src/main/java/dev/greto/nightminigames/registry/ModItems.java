package dev.greto.nightminigames.registry;

import dev.greto.nightminigames.NightminigamesMod;
import dev.greto.nightminigames.module.MeetingWandItem;
import dev.greto.nightminigames.module.ModuleWandItem;
import dev.greto.nightminigames.item.FlashlightItem;
import dev.greto.nightminigames.flashlight.FlashlightSpawnerWandItem;
import dev.greto.nightminigames.key.ColoredKeyItem;
import dev.greto.nightminigames.key.KeySpawnerWandItem;
import dev.greto.nightminigames.key.KeyColor;
import dev.greto.nightminigames.region.RegionKind;
import dev.greto.nightminigames.region.RegionSelectionWandItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

public final class ModItems {
    public static final Item FLASHLIGHT = Registry.register(
        BuiltInRegistries.ITEM,
        NightminigamesMod.id("flashlight"),
        new FlashlightItem(new Item.Properties().durability(480).stacksTo(1))
    );

    public static final Item WAND_FLASHLIGHT = Registry.register(
        BuiltInRegistries.ITEM, NightminigamesMod.id("wand_flashlight"), new FlashlightSpawnerWandItem()
    );

    public static final ColoredKeyItem KEY_BLUE = registerKey("blue");
    public static final ColoredKeyItem KEY_YELLOW = registerKey("yellow");
    public static final ColoredKeyItem KEY_RED = registerKey("red");
    public static final ColoredKeyItem KEY_GREEN = registerKey("green");

    public static final Item WAND_BLUE = registerWand(KeyColor.BLUE);
    public static final Item WAND_YELLOW = registerWand(KeyColor.YELLOW);
    public static final Item WAND_RED = registerWand(KeyColor.RED);
    public static final Item WAND_GREEN = registerWand(KeyColor.GREEN);

    public static final Item WAND_MODULOS = Registry.register(BuiltInRegistries.ITEM, NightminigamesMod.id("wand_modulos"), new ModuleWandItem());

    public static final Item WAND_REUNION = Registry.register(BuiltInRegistries.ITEM, NightminigamesMod.id("wand_reunion"), new MeetingWandItem());

    public static final Item WAND_REGION_ENTRADA = registerRegionWand(RegionKind.ENTRADA);
    public static final Item WAND_REGION_SALIDA = registerRegionWand(RegionKind.SALIDA);

    public static Item regionWand(RegionKind kind) {
        return kind == RegionKind.ENTRADA ? WAND_REGION_ENTRADA : WAND_REGION_SALIDA;
    }

    private static Item registerRegionWand(RegionKind kind) {
        return Registry.register(BuiltInRegistries.ITEM, NightminigamesMod.id("wand_region_" + kind.id), new RegionSelectionWandItem(kind));
    }

    public static final Item KILL = Registry.register(
        BuiltInRegistries.ITEM, NightminigamesMod.id("kill"),
        new BlockItem(ModBlocks.KILL, new Item.Properties())
    );

    public static final Item MACHINE = Registry.register(
        BuiltInRegistries.ITEM,
        NightminigamesMod.id("machine"),
        new BlockItem(ModBlocks.MACHINE, new Item.Properties().stacksTo(64))
    );

    public static final Item DOOR_ENTRANCE = Registry.register(
        BuiltInRegistries.ITEM, NightminigamesMod.id("door_entrance"),
        new BlockItem(ModBlocks.DOOR_ENTRANCE, new Item.Properties().stacksTo(1))
    );
    public static final Item DOOR_EXIT = Registry.register(
        BuiltInRegistries.ITEM, NightminigamesMod.id("door_exit"),
        new BlockItem(ModBlocks.DOOR_EXIT, new Item.Properties().stacksTo(1))
    );

    private static Item registerWand(KeyColor color) {
        return Registry.register(BuiltInRegistries.ITEM, NightminigamesMod.id("wand_key_" + color.id), new KeySpawnerWandItem(color));
    }

    public static Item wand(KeyColor color) {
        return switch (color) {
            case BLUE -> WAND_BLUE;
            case YELLOW -> WAND_YELLOW;
            case RED -> WAND_RED;
            case GREEN -> WAND_GREEN;
        };
    }

    private static ColoredKeyItem registerKey(String color) {
        return Registry.register(BuiltInRegistries.ITEM, NightminigamesMod.id("key_" + color), new ColoredKeyItem(color));
    }

    private ModItems() {
    }

    public static void initialize() {
    }
}
