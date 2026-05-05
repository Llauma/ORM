package com.lauma.nbt;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.List;

public class NbtExtractor {
    /**
     * Returns -1 if no CMD set.
     * CustomModelDataComponent.floats() maps old integer CMD to first float entry.
     */
    public static int getCustomModelData(ItemStack stack) {
        CustomModelDataComponent cmd = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (cmd == null) return -1;
        List<Float> floats = cmd.floats();
        return floats.isEmpty() ? 0 : floats.get(0).intValue();
    }

    public static NbtCompound getCustomNbt(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        return nbtComponent != null ? nbtComponent.copyNbt() : null;
    }
}