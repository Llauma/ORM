package com.lauma.mixin;

import com.lauma.OverrideResourceManager;
import com.lauma.client.render.OverrideBakedModel;
import com.lauma.client.render.OverrideRegistry;
import com.lauma.client.render.RenderContext;
import com.lauma.client.render.TextureOverrideManager;
import com.lauma.config.OverrideEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Mixin(ItemRenderState.LayerRenderState.class)
public abstract class MixinLayerRenderState {

    private static final Set<String> ORM_LOGGED = new HashSet<>();

    private static void ormDebug(String key, String fmt, Object... args) {
        synchronized (ORM_LOGGED) {
            if (!ORM_LOGGED.add(key)) return;
        }
        OverrideResourceManager.LOGGER.info("ORM[debug:" + key + "] " + fmt, args);
    }

    @ModifyVariable(method = "setModel(Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/client/render/RenderLayer;)V", at = @At("HEAD"), argsOnly = true, index = 1)
    private BakedModel orm$wrapModel(BakedModel original) {
        ormDebug("setModel-entered", "setModel intercepted; ctx={}, model={}", RenderContext.current(), original);
        ItemStack stack = RenderContext.current();
        if (stack == null || stack.isEmpty() || original == null) return original;
        ormDebug("setModel-stack", "stack on setModel: item={}", stack.getItem());

        Optional<OverrideEntry> matched = TextureOverrideManager.INSTANCE.resolveEntry(stack);
        if (matched.isEmpty()) {
            ormDebug("no-match-" + stack.getItem(), "no match for {}", stack.getItem());
            return original;
        }
        OverrideEntry entry = matched.get();
        ormDebug("match-" + entry.item + "-" + entry.customModelData,
                "match: item={}, cmd={}, hasNbt={}",
                entry.item, entry.customModelData, entry.hasNbtCondition());
        if (!entry.isPerInstance()) return original;

        Identifier spriteId = OverrideRegistry.INSTANCE.getSpriteId(entry);
        if (spriteId == null) {
            ormDebug("no-sprite-" + entry.item, "no spriteId for entry {}", entry.item);
            return original;
        }
        ormDebug("sprite-id-" + entry.item, "spriteId resolved to {}", spriteId);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getBakedModelManager() == null) return original;

        SpriteAtlasTexture atlas = mc.getBakedModelManager().getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        if (atlas == null) {
            ormDebug("no-atlas", "no BLOCK atlas instance");
            return original;
        }

        Sprite sprite = atlas.getSprite(spriteId);
        if (sprite == null || MissingSprite.getMissingSpriteId().equals(sprite.getContents().getId())) {
            OverrideResourceManager.LOGGER.warn(
                    "ORM: override sprite {} not stitched into atlas (sprite={})",
                    spriteId, sprite
            );
            return original;
        }
        ormDebug("wrap-" + entry.item + "-" + entry.customModelData,
                "wrapping {} with sprite {} (uv {},{} - {},{})",
                original, spriteId, sprite.getMinU(), sprite.getMinV(),
                sprite.getMaxU(), sprite.getMaxV());
        return new OverrideBakedModel(original, sprite);
    }
}