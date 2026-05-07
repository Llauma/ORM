package com.lauma.client.resource;

import com.lauma.OverrideResourceManager;
import com.lauma.client.render.OverrideFingerprint;
import com.lauma.client.render.OverrideRegistry;
import com.lauma.config.ORMConfig;
import com.lauma.config.ORMConfigManager;
import com.lauma.config.OverrideEntry;
import com.lauma.util.ChatUtils;
import net.minecraft.client.MinecraftClient;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Resource pack injected at the top of the stack so we can:
 * <ul>
 *   <li>replace vanilla texture files (legacy global override),</li>
 *   <li>provide additional atlas sprites for per-instance entries
 *       (substituted at render time), and</li>
 *   <li>serve user-supplied custom item models / textures from
 *       {@code config/orm/models/*.json} and {@code config/orm/textures/*.png}
 *       at {@code orm:item/<basename>}.</li>
 * </ul>
 */
public class ORMResourcePack implements ResourcePack {
    private static final Identifier ATLAS_JSON_ID = Identifier.of("minecraft", "atlases/blocks.json");
    private static final byte[] ATLAS_JSON_BYTES =
            ("{\"sources\":[{\"type\":\"directory\",\"source\":\"orm_overrides\",\"prefix\":\"orm_overrides/\"}]}")
                    .getBytes(StandardCharsets.UTF_8);

    private final Map<Identifier, Path> globalReplacements;
    private final Map<Identifier, Path> overrideSprites;
    /** Files this pack additionally serves: user JSON models and user item textures
     *  under the {@code orm} namespace ({@code assets/orm/models/item/...},
     *  {@code assets/orm/textures/item/...}). */
    private final Map<Identifier, Path> userAssets;

    public ORMResourcePack(ORMConfig config) {
        this(config, List.of());
    }

    public ORMResourcePack(ORMConfig config, List<ResourcePack> existingPacks) {
        OverrideRegistry.INSTANCE.clear();
        this.globalReplacements = new LinkedHashMap<>();
        this.overrideSprites = new LinkedHashMap<>();
        this.userAssets = new LinkedHashMap<>();
        build(config, existingPacks, this.globalReplacements, this.overrideSprites);
        scanUserAssets(this.userAssets);
    }

    private static void build(
            ORMConfig config,
            List<ResourcePack> existingPacks,
            Map<Identifier, Path> globalReplacements,
            Map<Identifier, Path> overrideSprites
    ) {
        ModelTextureResolver resolver = new ModelTextureResolver(existingPacks);
        MinecraftClient mc = MinecraftClient.getInstance();

        for (OverrideEntry entry : config.overrides) {
            if (entry.isPerInstance()) {
                if (entry.hasTexture()) {
                    Path diskFile = resolveDiskFile(entry);
                    if (diskFile == null || !Files.exists(diskFile)) {
                        OverrideResourceManager.LOGGER.warn("ORM: texture not found: {}", diskFile);
                        if (mc != null) {
                            final String id = entry.item;
                            final boolean hasModel = entry.hasModel();
                            mc.execute(() -> {
                                if (hasModel) ChatUtils.modelNoTextures(id);
                                else ChatUtils.texturesNotFound(id);
                            });
                        }
                    } else {
                        String fp = OverrideFingerprint.of(entry);
                        Identifier spriteFile = Identifier.of(
                                "minecraft",
                                "textures/orm_overrides/" + fp + ".png"
                        );
                        overrideSprites.put(spriteFile, diskFile);
                        OverrideRegistry.INSTANCE.register(entry);
                        OverrideResourceManager.LOGGER.info(
                                "ORM: sprite {} -> {}", spriteFile, diskFile.getFileName()
                        );
                        if (mc != null) {
                            final String id = entry.item;
                            mc.execute(() -> ChatUtils.modelAndTexturesFound(id));
                        }
                    }
                } else if (entry.hasModel()) {
                    if (mc != null) {
                        final String id = entry.item;
                        mc.execute(() -> ChatUtils.modelLoaded(id));
                    }
                }
                continue;
            }

            if (!entry.hasTexture()) continue;
            Path diskFile = resolveDiskFile(entry);
            if (diskFile == null || !Files.exists(diskFile)) {
                OverrideResourceManager.LOGGER.warn("ORM: texture not found: {}", diskFile);
                if (mc != null) {
                    final String id = entry.item;
                    mc.execute(() -> ChatUtils.texturesNotFound(id));
                }
                continue;
            }
            Identifier textureId = resolveTextureId(entry, resolver);
            if (textureId == null) continue;
            globalReplacements.put(textureId, diskFile);
            OverrideResourceManager.LOGGER.info("ORM: {} -> {}", textureId, diskFile.getFileName());
            if (mc != null) {
                final String id = entry.item;
                mc.execute(() -> ChatUtils.modelAndTexturesFound(id));
            }
        }
    }

    /** Scans {@code config/orm/models/} and {@code config/orm/textures/} (recursively) and exposes
     *  their files as resources under the {@code orm} namespace.
     *
     *  <p>The relative path under each root maps directly to the resource path so the layout
     *  mirrors a normal resource pack:
     *  <pre>
     *  config/orm/models/item/foo.json    -> assets/orm/models/item/foo.json
     *  config/orm/textures/item/foo.png   -> assets/orm/textures/item/foo.png
     *  </pre>
     *
     *  <p>For convenience, files placed directly at the root of each dir (no subfolder) are
     *  ALSO exposed at the conventional {@code item/} path so older flat layouts keep working:
     *  <pre>
     *  config/orm/models/foo.json         -> ALSO assets/orm/models/item/foo.json
     *  config/orm/textures/foo.png        -> ALSO assets/orm/textures/item/foo.png
     *  </pre>
     */
    private static void scanUserAssets(Map<Identifier, Path> userAssets) {
        scanRecursive(ORMConfigManager.MODEL_DIR, "models", ".json", userAssets, true);
        scanRecursive(ORMConfigManager.TEXTURE_DIR, "textures", ".png", userAssets, false);
        scanRecursive(ORMConfigManager.TEXTURE_DIR, "textures", ".png.mcmeta", userAssets, false);
    }

    /** Walks {@code root} recursively and registers every file ending in {@code suffix} under
     *  the resource path {@code <topLevel>/<relativePath>}. Files at the root of the dir are
     *  additionally aliased under {@code <topLevel>/item/<filename>} for backward compatibility.
     *
     *  @param logRegistration whether to log each registration (used for models for visibility) */
    private static void scanRecursive(
            Path root,
            String topLevel,
            String suffix,
            Map<Identifier, Path> userAssets,
            boolean logRegistration
    ) {
        if (!Files.isDirectory(root)) return;
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(suffix))
                    .forEach(p -> {
                        Path rel = root.relativize(p);
                        String relStr = rel.toString().replace('\\', '/');

                        Identifier id = Identifier.of("orm", topLevel + "/" + relStr);
                        userAssets.put(id, p);
                        if (logRegistration) {
                            OverrideResourceManager.LOGGER.info("ORM: serving {} -> {}", id, p);
                        }

                        if (!relStr.contains("/")) {
                            String name = p.getFileName().toString();
                            Identifier flatId = Identifier.of("orm", topLevel + "/item/" + name);
                            userAssets.put(flatId, p);
                        }
                    });
        } catch (IOException e) {
            OverrideResourceManager.LOGGER.warn("ORM: failed to scan dir {}", root, e);
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
                    "ORM: cannot auto-resolve target for {}#{} - skipping entry (no vanilla texture fallback)",
                    entry.item, entry.customModelData
            );
            return null;
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
        if (file == null) file = userAssets.get(id);
        if (file == null) return null;
        final Path target = file;
        if (target.getFileName().toString().endsWith(".json")) {
            return () -> new ByteArrayInputStream(patchModelJson(Files.readAllBytes(target)));
        }
        return () -> Files.newInputStream(target);
    }

    private static byte[] patchModelJson(byte[] original) {
        try {
            String json = new String(original, StandardCharsets.UTF_8);
            if (!json.contains("\"#missing\"")) return original;
            json = json.replace("\"#missing\"", "\"#0\"");
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return original;
        }
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
        for (Map.Entry<Identifier, Path> e : userAssets.entrySet()) {
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
        for (Identifier id : userAssets.keySet()) ns.add(id.getNamespace());
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
