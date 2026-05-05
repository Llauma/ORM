package com.lauma.client.render;
 
import net.minecraft.item.ItemStack;
 
public final class RenderContext {
    private static final ThreadLocal<ItemStack> CURRENT = new ThreadLocal<>();
    private RenderContext() {}
    public static void push(ItemStack stack) { CURRENT.set(stack); }
    public static ItemStack current() { return CURRENT.get(); }
    public static void pop() { CURRENT.remove(); }
}