package com.lauma.mixin;

import com.lauma.client.render.RenderContext;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelManager.class)
public abstract class MixinItemModelManager {

    private static volatile boolean ormLoggedUpdate7 = false;
    private static volatile boolean ormLoggedUpdate6 = false;

    @Inject(
        method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
        at = @At("HEAD")
    )
    private void orm$pushUpdate7(ItemRenderState state, ItemStack stack, ModelTransformationMode mode, boolean leftHanded, World world, LivingEntity entity, int seed, CallbackInfo ci) {
        if (!ormLoggedUpdate7) {
            ormLoggedUpdate7 = true;
            com.lauma.OverrideResourceManager.LOGGER.info(
                "ORM[debug:imm-update7] first call, stack={}, mode={}",
                stack.getItem(), mode
            );
        }
        RenderContext.push(stack);
    }

    @Inject(
        method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
        at = @At("RETURN")
    )
    private void orm$popUpdate7(ItemRenderState state, ItemStack stack, ModelTransformationMode mode, boolean leftHanded, World world, LivingEntity entity, int seed, CallbackInfo ci) {
        RenderContext.pop();
    }

    @Inject(
        method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
        at = @At("HEAD")
    )
    private void orm$pushUpdate6(ItemRenderState state, ItemStack stack, ModelTransformationMode mode, World world, LivingEntity entity, int seed, CallbackInfo ci) {
        if (!ormLoggedUpdate6) {
            ormLoggedUpdate6 = true;
            com.lauma.OverrideResourceManager.LOGGER.info(
                "ORM[debug:imm-update6] first call, stack={}, mode={}",
                stack.getItem(), mode
            );
        }
        RenderContext.push(stack);
    }

    @Inject(
        method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
        at = @At("RETURN")
    )
    private void orm$popUpdate6(ItemRenderState state, ItemStack stack, ModelTransformationMode mode, World world, LivingEntity entity, int seed, CallbackInfo ci) {
        RenderContext.pop();
    }
}