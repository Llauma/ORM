package com.lauma.client.resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lauma.OverrideResourceManager;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves the texture identifier referenced by a custom model.
 *
 * <p>Walks the chain {@code item model -> custom_model_data override -> custom model
 * -> textures.layer0} (with parent fallback) by reading model JSONs from the
 * currently loaded resource packs.
 *
 * <p>Only the legacy 1.21.4 {@code "overrides"} format is supported. The new
 * {@code assets/<ns>/items/<name>.json} item-definition format introduced in
 * 1.21.4 has a different schema and is not handled here.
 */
public class ModelTextureResolver {
    private final List<ResourcePack> packs;
    private final Map<Identifier, JsonObject> jsonCache = new HashMap<>();

    public ModelTextureResolver(List<ResourcePack> packs) {
        this.packs = packs;
    }

    /**
     * Returns the full texture identifier
     * (e.g. {@code "ei:textures/item/regeneration_stick.png"}) referenced by the
     * custom model bound to {@code itemRef + cmd}, or {@code null} if it cannot
     * be resolved from the loaded resource packs.
     */
    public Identifier resolve(String itemRef, int cmd) {
        Identifier itemId = Identifier.tryParse(itemRef);
        if (itemId == null) return null;

        Identifier baseModelId = Identifier.of(
            itemId.getNamespace(),
            "models/item/" + itemId.getPath() + ".json"
        );
        JsonObject base = readJson(baseModelId);
        if (base == null || !base.has("overrides")) return null;

        String customModelRef = null;
        for (JsonElement el : base.getAsJsonArray("overrides")) {
            if (!el.isJsonObject()) continue;
            JsonObject ov = el.getAsJsonObject();
            if (!ov.has("predicate") || !ov.has("model")) continue;
            JsonObject pred = ov.getAsJsonObject("predicate");
            if (pred.has("custom_model_data")
                && pred.get("custom_model_data").getAsInt() == cmd) {
                customModelRef = ov.get("model").getAsString();
                break;
            }
        }
        if (customModelRef == null) return null;

        return findLayer0(customModelRef, new HashSet<>());
    }

    private Identifier findLayer0(String modelRef, Set<String> visited) {
        if (!visited.add(modelRef)) return null;
        Identifier id = Identifier.tryParse(modelRef);
        if (id == null) return null;
        Identifier modelFileId = Identifier.of(
            id.getNamespace(),
            "models/" + id.getPath() + ".json"
        );
        JsonObject model = readJson(modelFileId);
        if (model == null) return null;

        if (model.has("textures")) {
            JsonObject tex = model.getAsJsonObject("textures");
            if (tex.has("layer0")) {
                Identifier t = Identifier.tryParse(tex.get("layer0").getAsString());
                if (t != null) {
                    return Identifier.of(
                        t.getNamespace(),
                        "textures/" + t.getPath() + ".png"
                    );
                }
            }
        }
        if (model.has("parent")) {
            return findLayer0(model.get("parent").getAsString(), visited);
        }
        return null;
    }

    private JsonObject readJson(Identifier id) {
        if (jsonCache.containsKey(id)) return jsonCache.get(id);
        for (int i = packs.size() - 1; i >= 0; i--) {
            InputSupplier<InputStream> supplier = packs.get(i)
                .open(ResourceType.CLIENT_RESOURCES, id);
            if (supplier == null) continue;
            try (InputStream in = supplier.get();
                 InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
                jsonCache.put(id, obj);
                return obj;
            } catch (Exception e) {
                OverrideResourceManager.LOGGER.warn("ORM: failed to parse {}: {}", id, e.toString());
            }
        }
        jsonCache.put(id, null);
        return null;
    }
}
