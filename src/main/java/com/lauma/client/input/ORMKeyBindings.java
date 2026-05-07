package com.lauma.client.input;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class ORMKeyBindings {
    public static KeyBinding ADD_ITEM;

    private ORMKeyBindings() {}

    public static void register() {
        ADD_ITEM = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.orm.add_item",
            InputUtil.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            "category.orm"
        ));
    }
}