package dev.greto.nightminigames.flashlight;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class FlashlightData {
    private static final String ROOT_TAG = "NightminigamesFlashlight";
    private static final String ENABLED_TAG = "Enabled";

    private FlashlightData() {
    }

    public static boolean isEnabled(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        CompoundTag data = customData.copyTag();
        if (!data.contains(ROOT_TAG, 10)) {
            return false;
        }
        return data.getCompound(ROOT_TAG).getBoolean(ENABLED_TAG);
    }

    public static void setEnabled(ItemStack stack, boolean enabled) {
        CompoundTag data = getData(stack);
        CompoundTag root = data.contains(ROOT_TAG, 10) ? data.getCompound(ROOT_TAG) : new CompoundTag();
        root.putBoolean(ENABLED_TAG, enabled);
        data.put(ROOT_TAG, root);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    private static CompoundTag getData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : new CompoundTag();
    }
}
