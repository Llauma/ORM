package com.lauma.mixin;

import com.lauma.client.resource.ORMResourcePack;
import com.lauma.config.ORMConfigManager;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ReloadableResourceManagerImpl.class)
public class MixinResourceManager {
    // Inject ORM pack as highest-priority pack on every resource reload.
    // Last entry in the list wins, so we add to the end.
    @ModifyVariable(
        method = "reload(Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;Ljava/util/concurrent/CompletableFuture;Ljava/util/List;)Lnet/minecraft/resource/ResourceReload;",
        at = @At("HEAD"),
        argsOnly = true
    )
    private List<ResourcePack> injectOrmPack(List<ResourcePack> packs) {
        List<ResourcePack> modified = new ArrayList<>(packs);
        modified.add(new ORMResourcePack(ORMConfigManager.load()));
        return modified;
    }
}