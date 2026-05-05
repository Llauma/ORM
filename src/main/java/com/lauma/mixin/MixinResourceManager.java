package com.lauma.mixin;

import com.lauma.client.render.TextureOverrideManager;
import com.lauma.client.resource.ORMResourcePack;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ReloadableResourceManagerImpl.class)
public class MixinResourceManager {
    @ModifyVariable(
            method = "reload(Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;Ljava/util/List;)Lnet/minecraft/resource/ResourceReload;",
            at = @At("HEAD"),
            argsOnly = true
    )
    private List<ResourcePack> injectOrmPack(List<ResourcePack> packs) {
        List<ResourcePack> modified = new ArrayList<>(packs);
        // Reload config from disk so render-time matcher and ORMResourcePack
        // share the same source of truth.
        TextureOverrideManager.INSTANCE.reload();
        modified.add(new ORMResourcePack(TextureOverrideManager.INSTANCE.getConfig(), packs));
        return modified;
    }
}