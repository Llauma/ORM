package com.lauma.client.resource;

import com.lauma.OverrideResourceManager;
import com.lauma.config.ORMConfig;
import com.lauma.config.ORMConfigManager;
import com.lauma.config.OverrideEntry;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.ResourceMetadataSerializer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ORMResourcePack implements ResourcePack {
    private final Map<Identifier, Path> overrides;

    public ORMResourcePack(ORMConfig config) {
        this(config, List.of());
    }

    public ORMResourcePack(ORMConfig config, List<ResourcePack> existingPacks) {
        this.overrides = build(config, existingPacks);
    }

    private static Map<Identifier, Path> build(ORMConfig config, List<ResourcePack> existingPacks) {
        Map<Identifier, Path> map = new LinkedHashMap<>();
        ModelTextureResolver resolver = new ModelTextureResolver(existingPacks);

        for (OverrideEntry entry : config.overrides) {
            Identifier textureId = resolveTextureId(entry, resolver);
            if (textureId == null) continue;

            String[] parts = entry.texture.split(":", 2);
            String filePath = parts.length == 2 ? parts[1] : parts[0];
            Path diskFile = ORMConfigManager.TEXTURE_DIR.resolve(filePath + ".png");

            if (Files.exists(diskFile)) {
                map.put(textureId, diskFile);
                OverrideResourceManager.LOGGER.info("ORM: {} → {}", textureId, diskFile.getFileName());
            } else {
                OverrideResourceManager.LOGGER.warn("ORM: texture not found: {}", diskFile);
            }
        }
        return map;
    }

    private static Identifier resolveTextureId(OverrideEntry entry, ModelTextureResolver resolver) {
        // 1) explicit target wins: "ei:item/regeneration_stick" -> "ei:textures/item/regeneration_stick.png"
        if (entry.hasTarget()) {
            Identifier t = Identifier.tryParse(entry.target);
            if (t != null) {
                return Identifier.of(t.getNamespace(), "textures/" + t.getPath() + ".png");
            }
            OverrideResourceManager.LOGGER.warn("ORM: invalid target id: {}", entry.target);
        }

        // 2) auto-resolve via the custom-model-data override chain
        if (entry.hasCustomModelData()) {
            Identifier resolved = resolver.resolve(entry.item, entry.customModelData);
            if (resolved != null) {
                OverrideResourceManager.LOGGER.info(
                    "ORM: auto-resolved {}#{} -> {}",
                    entry.item, entry.customModelData, resolved
                );
                return resolved;
            }
            OverrideResourceManager.LOGGER.warn(
                "ORM: cannot auto-resolve target for {}#{} - falling back to vanilla item texture",
                entry.item, entry.customModelData
            );
        }

        // 3) fallback: replace the vanilla item texture (original behaviour)
        Identifier itemId = Identifier.tryParse(entry.item);
        if (itemId == null) return null;
        return Identifier.of(
            itemId.getNamespace(),
            "textures/item/" + itemId.getPath() + ".png"
        );
    }

    @Override
    public @Nullable InputSupplier<InputStream> openRoot(String... segments) {
        return null;
    }

    @Override
    public @Nullable InputSupplier<InputStream> open(ResourceType type, Identifier id) {
        if (type != ResourceType.CLIENT_RESOURCES) return null;
        Path file = overrides.get(id);
        if (file == null) return null;
        return () -> Files.newInputStream(file);
    }

    @Override
    public void findResources(ResourceType type, String namespace, String prefix, ResultConsumer consumer) {
        if (type != ResourceType.CLIENT_RESOURCES) return;
        overrides.forEach((id, path) -> {
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(prefix)) {
                consumer.accept(id, () -> Files.newInputStream(path));
            }
        });
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        if (type != ResourceType.CLIENT_RESOURCES) return Set.of();
        return overrides.keySet().stream()
            .map(Identifier::getNamespace)
            .collect(Collectors.toSet());
    }

    @Override
    public @Nullable <T> T parseMetadata(ResourceMetadataSerializer<T> serializer) throws IOException {
        return null;
    }

    @Override
    public ResourcePackInfo getInfo() {
        return new ResourcePackInfo(
            "orm_override_pack",
            Text.literal("ORM Overrides"),
            ResourcePackSource.BUILTIN,
            Optional.empty()
        );
    }

    @Override
    public void close() {}
}