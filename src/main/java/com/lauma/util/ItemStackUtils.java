package com.lauma.util;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ItemStackUtils {
    public static String getItemId(ItemStack stack) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return id.toString();
    }

    public static String getDisplayName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        try {
            return stack.getName().getString();
        } catch (Throwable t) {
            return null;
        }
    }
}