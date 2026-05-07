package com.lauma.mixin;

import com.lauma.OverrideResourceManager;
import com.lauma.client.render.OverrideBakedModel;
import com.lauma.client.render.OverrideRegistry;
import com.lauma.client.render.RenderContext;
import com.lauma.client.render.TextureOverrideManager;
import com.lauma.config.OverrideEntry;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;

@Mixin(ItemRenderState.LayerRenderState.class)
public abstract class MixinLayerRenderState {

    @ModifyVariable(
            method = "setModel(Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/client/render/RenderLayer;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1
    )
    private BakedModel orm$wrapModel(BakedModel original) {
        if (!RenderContext.isOverrideMode()) return original;
        ItemStack stack = RenderContext.current();
        if (stack == null || stack.isEmpty() || original == null) return original;

        Optional<OverrideEntry> matched = TextureOverrideManager.INSTANCE.resolveEntry(stack);
        if (matched.isEmpty()) return original;

        OverrideEntry entry = matched.get();
        if (!entry.isPerInstance()) return original;

        if (entry.hasModel()) {
            BakedModel custom = lookupCustomModel(entry.model);
            if (custom != null) return custom;
        }

        if (!entry.hasTexture()) return original;
        Identifier spriteId = OverrideRegistry.INSTANCE.getSpriteId(entry);
        if (spriteId == null) return original;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getBakedModelManager() == null) return original;

        SpriteAtlasTexture atlas = mc.getBakedModelManager()
                .getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        if (atlas == null) return original;

        Sprite sprite = atlas.getSprite(spriteId);
        if (sprite == null
                || MissingSprite.getMissingSpriteId().equals(sprite.getContents().getId())) {
            OverrideResourceManager.LOGGER.warn(
                    "ORM: override sprite {} not stitched into atlas (sprite={})",
                    spriteId, sprite
            );
            return original;
        }
        return new OverrideBakedModel(original, sprite);
    }

    /** Resolves a user model id to a baked model via {@link FabricBakedModelManager#getModel(Identifier)}.
     *
     *  <p>Path resolution:
     *  <ul>
     *    <li>{@code orm:foo}              -> {@code orm:item/foo}      (legacy: prepend item/)</li>
     *    <li>{@code orm:item/foo}         -> {@code orm:item/foo}      (already qualified)</li>
     *    <li>{@code orm:weapons/foo}      -> {@code orm:weapons/foo}   (subfolder, leave as-is)</li>
     *  </ul>
     */
    private static BakedModel lookupCustomModel(String modelStr) {
        Identifier raw = Identifier.tryParse(modelStr);
        if (raw == null) return null;
        Identifier id = raw.getPath().contains("/")
                ? raw
                : Identifier.of(raw.getNamespace(), "item/" + raw.getPath());
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return null;
        BakedModelManager mgr = mc.getBakedModelManager();
        if (mgr == null) return null;
        try {
            return ((FabricBakedModelManager) mgr).getModel(id);
        } catch (Throwable t) {
            OverrideResourceManager.LOGGER.warn("ORM: lookup failed for {}: {}", id, t.toString());
            return null;
        }
    }
}
