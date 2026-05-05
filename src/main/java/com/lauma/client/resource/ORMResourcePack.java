package com.lauma.client.resource;

import com.lauma.OverrideResourceManager;
import com.lauma.client.render.OverrideFingerprint;
import com.lauma.client.render.OverrideRegistry;
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ORMResourcePack implements ResourcePack {
    private static final Identifier ATLAS_JSON_ID = Identifier.of("minecraft", "atlases/blocks.json");
    private static final byte[] ATLAS_JSON_BYTES =
            ("{\"sources\":[{\"type\":\"directory\",\"source\":\"orm_overrides\",\"prefix\":\"orm_overrides/\"}]}")
                    .getBytes(StandardCharsets.UTF_8);

    private final Map<Identifier, Path> globalReplacements;
    private final Map<Identifier, Path> overrideSprites;

    public ORMResourcePack(ORMConfig config) {
        this(config, List.of());
    }

    public ORMResourcePack(ORMConfig config, List<ResourcePack> existingPacks) {
        OverrideRegistry.INSTANCE.clear();
        this.globalReplacements = new LinkedHashMap<>();
        this.overrideSprites = new LinkedHashMap<>();
        build(config, existingPacks, this.globalReplacements, this.overrideSprites);
    }

    private static void build(
            ORMConfig config,
            List<ResourcePack> existingPacks,
            Map<Identifier, Path> globalReplacements,
            Map<Identifier, Path> overrideSprites
    ) {
        ModelTextureResolver resolver = new ModelTextureResolver(existingPacks);

        for (OverrideEntry entry : config.overrides) {
            Path diskFile = resolveDiskFile(entry);
            if (diskFile == null || !Files.exists(diskFile)) {
                OverrideResourceManager.LOGGER.warn("ORM: texture not found: {}", diskFile);
                continue;
            }

            if (entry.hasNbtCondition()) {
                String fp = OverrideFingerprint.of(entry);
                Identifier spriteFile = Identifier.of(
                        "minecraft",
                        "textures/orm_overrides/" + fp + ".png"
                );
                overrideSprites.put(spriteFile, diskFile);
                OverrideRegistry.INSTANCE.register(entry);
                OverrideResourceManager.LOGGER.info("ORM: sprite {} -> {}", spriteFile, diskFile.getFileName());
                continue;
            }

            Identifier textureId = resolveTextureId(entry, resolver);
            if (textureId == null) continue;
            globalReplacements.put(textureId, diskFile);
            OverrideResourceManager.LOGGER.info("ORM: {} -> {}", textureId, diskFile.getFileName());
        }
    }

    private static Path resolveDiskFile(OverrideEntry entry) {
        if (entry.texture == null || entry.texture.isEmpty()) return null;
        String[] parts = entry.texture.split(":", 2);
        String filePath = parts.length == 2 ? parts[1] : parts[0];
        return ORMConfigManager.TEXTURE_DIR.resolve(filePath + ".png");
    }

    private static Identifier resolveTextureId(OverrideEntry entry, ModelTextureResolver resolver) {
        if (entry.hasTarget()) {
            Identifier t = Identifier.tryParse(entry.target);
            if (t != null) {
                return Identifier.of(t.getNamespace(), "textures/" + t.getPath() + ".png");
            }
            OverrideResourceManager.LOGGER.warn("ORM: invalid target id: {}", entry.target);
        }

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
        if (id.equals(ATLAS_JSON_ID) && !overrideSprites.isEmpty()) {
            return () -> new ByteArrayInputStream(ATLAS_JSON_BYTES);
        }
        Path file = globalReplacements.get(id);
        if (file == null) file = overrideSprites.get(id);
        if (file == null) return null;
        final Path target = file;
        return () -> Files.newInputStream(target);
    }

    @Override
    public void findResources(ResourceType type, String namespace, String prefix, ResultConsumer consumer) {
        if (type != ResourceType.CLIENT_RESOURCES) return;
        for (Map.Entry<Identifier, Path> e : globalReplacements.entrySet()) {
            Identifier id = e.getKey();
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(prefix)) {
                consumer.accept(id, () -> Files.newInputStream(e.getValue()));
            }
        }
        for (Map.Entry<Identifier, Path> e : overrideSprites.entrySet()) {
            Identifier id = e.getKey();
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(prefix)) {
                consumer.accept(id, () -> Files.newInputStream(e.getValue()));
            }
        }
        if (!overrideSprites.isEmpty()
                && ATLAS_JSON_ID.getNamespace().equals(namespace)
                && ATLAS_JSON_ID.getPath().startsWith(prefix)) {
            consumer.accept(ATLAS_JSON_ID, () -> new ByteArrayInputStream(ATLAS_JSON_BYTES));
        }
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        if (type != ResourceType.CLIENT_RESOURCES) return Set.of();
        Set<String> ns = new HashSet<>();
        for (Identifier id : globalReplacements.keySet()) ns.add(id.getNamespace());
        for (Identifier id : overrideSprites.keySet()) ns.add(id.getNamespace());
        if (!overrideSprites.isEmpty()) ns.add(ATLAS_JSON_ID.getNamespace());
        return ns;
    }

    @Override
    public @Nullable <T> T parseMetadata(ResourceMetadataSerializer<T> serializer) {
        return null;
    }

    @Override
    public ResourcePackInfo getInfo() {
        return new ResourcePackInfo(
                "orm_overrides",
                Text.of("ORM Overrides"),
                ResourcePackSource.BUILTIN,
                java.util.Optional.empty()
        );
    }

    @Override
    public void close() {}
}