package com.lauma.mixin;

import com.lauma.client.render.RenderContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    private static volatile boolean ormLoggedSimple = false;
    private static volatile boolean ormLoggedLiving = false;

    @Inject(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;I)V", at = @At("HEAD"))
    private void orm$pushSimple(ItemStack stack, ModelTransformationMode m, int l, int o, MatrixStack ms, VertexConsumerProvider vcp, World w, int s, CallbackInfo ci) {
        if (!ormLoggedSimple) {
            ormLoggedSimple = true;
            com.lauma.OverrideResourceManager.LOGGER.info("ORM[debug:itemrenderer-simple] first call, stack={}", stack.getItem());
        }
        RenderContext.push(stack);
    }

    @Inject(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;I)V", at = @At("RETURN"))
    private void orm$popSimple(ItemStack stack, ModelTransformationMode m, int l, int o, MatrixStack ms, VertexConsumerProvider vcp, World w, int s, CallbackInfo ci) {
        RenderContext.pop();
    }

    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V", at = @At("HEAD"))
    private void orm$pushLiving(LivingEntity e, ItemStack stack, ModelTransformationMode m, boolean lh, MatrixStack ms, VertexConsumerProvider vcp, World w, int l, int o, int s, CallbackInfo ci) {
        if (!ormLoggedLiving) {
            ormLoggedLiving = true;
            com.lauma.OverrideResourceManager.LOGGER.info("ORM[debug:itemrenderer-living] first call, stack={}", stack.getItem());
        }
        RenderContext.push(stack);
    }

    @Inject(method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V", at = @At("RETURN"))
    private void orm$popLiving(LivingEntity e, ItemStack stack, ModelTransformationMode m, boolean lh, MatrixStack ms, VertexConsumerProvider vcp, World w, int l, int o, int s, CallbackInfo ci) {
        RenderContext.pop();
    }
}