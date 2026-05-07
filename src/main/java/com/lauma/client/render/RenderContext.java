package com.lauma.client.render;

import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;

import java.util.Set;

public final class RenderContext {
    private static final Set<ModelTransformationMode> OVERRIDE_MODES = Set.of(
            ModelTransformationMode.FIRST_PERSON_RIGHT_HAND,
            ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
            ModelTransformationMode.THIRD_PERSON_RIGHT_HAND,
            ModelTransformationMode.THIRD_PERSON_LEFT_HAND,
            ModelTransformationMode.GUI,
            ModelTransformationMode.HEAD,
            ModelTransformationMode.FIXED,
            ModelTransformationMode.GROUND
    );

    private static final ThreadLocal<ItemStack> CURRENT_STACK = new ThreadLocal<>();
    private static final ThreadLocal<ModelTransformationMode> CURRENT_MODE = new ThreadLocal<>();

    private RenderContext() {}

    public static void push(ItemStack stack, ModelTransformationMode mode) {
        CURRENT_STACK.set(stack);
        CURRENT_MODE.set(mode);
    }

    public static ItemStack current() { return CURRENT_STACK.get(); }

    public static boolean isOverrideMode() {
        ModelTransformationMode mode = CURRENT_MODE.get();
        return mode != null && OVERRIDE_MODES.contains(mode);
    }

    public static void pop() {
        CURRENT_STACK.remove();
        CURRENT_MODE.remove();
    }
}