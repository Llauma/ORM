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
        this.overrides = build(config);
    }

    private static Map<Identifier, Path> build(ORMConfig config) {
        Map<Identifier, Path> map = new LinkedHashMap<>();
        for (OverrideEntry entry : config.overrides) {
            Identifier itemId = Identifier.tryParse(entry.item);
            if (itemId == null) continue;

            Identifier textureId = Identifier.of(
                itemId.getNamespace(),
                "textures/item/" + itemId.getPath() + ".png"
            );

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