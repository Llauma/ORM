package com.lauma.client.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
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
 * <p>Supports both the 1.21.4+ item-definition format
 * ({@code assets/<ns>/items/<name>.json} with {@code model.type} =
 * {@code model}/{@code range_dispatch}/{@code select}/{@code condition})
 * and the legacy {@code overrides} array inside
 * {@code assets/<ns>/models/item/<name>.json}.
 *
 * <p>Once a custom model is identified, walks parent chains and tries
 * {@code layer0}, then {@code "0"}, then any first textual texture entry
 * to support both flat ({@code item/generated}) and 3D Blockbench models.
 */
public class ModelTextureResolver {
    private final List<ResourcePack> packs;
    private final Map<Identifier, JsonObject> jsonCache = new HashMap<>();

    public ModelTextureResolver(List<ResourcePack> packs) {
        this.packs = packs;
    }

    /**
     * Returns the full texture identifier
     * (e.g. {@code "minecraft:textures/prisonevo/wands/regeneration.png"})
     * referenced by the custom model bound to {@code itemRef + cmd},
     * or {@code null} if it cannot be resolved.
     */
    public Identifier resolve(String itemRef, int cmd) {
        Identifier itemId = Identifier.tryParse(itemRef);
        if (itemId == null) return null;

        String customModelRef = resolveModelRef(itemId, cmd);
        if (customModelRef == null) return null;

        return findTexture(customModelRef, new HashSet<>());
    }

    private String resolveModelRef(Identifier itemId, int cmd) {
        // 1.21.4+ item-definition file
        Identifier itemDefId = Identifier.of(
                itemId.getNamespace(),
                "items/" + itemId.getPath() + ".json"
        );
        JsonObject itemDef = readJson(itemDefId);
        if (itemDef != null && itemDef.has("model") && itemDef.get("model").isJsonObject()) {
            String ref = walkSelector(itemDef.getAsJsonObject("model"), cmd);
            if (ref != null) return ref;
        }

        // Legacy overrides array
        Identifier baseModelId = Identifier.of(
                itemId.getNamespace(),
                "models/item/" + itemId.getPath() + ".json"
        );
        JsonObject base = readJson(baseModelId);
        if (base != null && base.has("overrides") && base.get("overrides").isJsonArray()) {
            for (JsonElement el : base.getAsJsonArray("overrides")) {
                if (!el.isJsonObject()) continue;
                JsonObject ov = el.getAsJsonObject();
                if (!ov.has("predicate") || !ov.has("model")) continue;
                JsonObject pred = ov.getAsJsonObject("predicate");
                if (pred.has("custom_model_data")
                        && pred.get("custom_model_data").getAsInt() == cmd) {
                    return ov.get("model").getAsString();
                }
            }
        }

        return null;
    }

    private String walkSelector(JsonObject node, int cmd) {
        if (node == null || !node.has("type")) return null;
        String type = stripMinecraftPrefix(asString(node.get("type")));
        if (type == null) return null;

        switch (type) {
            case "model":
                return node.has("model") ? asString(node.get("model")) : null;

            case "range_dispatch":
                if (isCmdProperty(node)) {
                    String r = matchRangeDispatch(node, cmd);
                    if (r != null) return r;
                }
                return walkFallback(node, cmd);

            case "select":
                if (isCmdProperty(node)) {
                    String r = matchSelect(node, cmd);
                    if (r != null) return r;
                }
                return walkFallback(node, cmd);

            case "condition": {
                String r = walkBranch(node, "on_false", cmd);
                if (r != null) return r;
                return walkBranch(node, "on_true", cmd);
            }

            default:
                return walkFallback(node, cmd);
        }
    }

    private String matchRangeDispatch(JsonObject node, int cmd) {
        if (!node.has("entries") || !node.get("entries").isJsonArray()) return null;
        JsonArray entries = node.getAsJsonArray("entries");
        JsonObject best = null;
        double bestThreshold = -Double.MAX_VALUE;
        for (JsonElement el : entries) {
            if (!el.isJsonObject()) continue;
            JsonObject entry = el.getAsJsonObject();
            if (!entry.has("threshold") || !entry.has("model")) continue;
            double threshold = entry.get("threshold").getAsDouble();
            if (threshold <= cmd && threshold > bestThreshold) {
                best = entry;
                bestThreshold = threshold;
            }
        }
        if (best == null || !best.get("model").isJsonObject()) return null;
        return walkSelector(best.getAsJsonObject("model"), cmd);
    }

    private String matchSelect(JsonObject node, int cmd) {
        if (!node.has("cases") || !node.get("cases").isJsonArray()) return null;
        String cmdStr = String.valueOf(cmd);
        for (JsonElement el : node.getAsJsonArray("cases")) {
            if (!el.isJsonObject()) continue;
            JsonObject c = el.getAsJsonObject();
            if (!c.has("when") || !c.has("model")) continue;
            if (matchesWhen(c.get("when"), cmd, cmdStr)
                    && c.get("model").isJsonObject()) {
                String r = walkSelector(c.getAsJsonObject("model"), cmd);
                if (r != null) return r;
            }
        }
        return null;
    }

    private boolean matchesWhen(JsonElement when, int cmd, String cmdStr) {
        if (when.isJsonArray()) {
            for (JsonElement v : when.getAsJsonArray()) {
                if (matchesWhen(v, cmd, cmdStr)) return true;
            }
            return false;
        }
        if (!when.isJsonPrimitive()) return false;
        JsonPrimitive p = when.getAsJsonPrimitive();
        if (p.isNumber()) return p.getAsInt() == cmd;
        if (p.isString()) return p.getAsString().equals(cmdStr);
        return false;
    }

    private String walkFallback(JsonObject node, int cmd) {
        if (!node.has("fallback") || !node.get("fallback").isJsonObject()) return null;
        return walkSelector(node.getAsJsonObject("fallback"), cmd);
    }

    private String walkBranch(JsonObject node, String key, int cmd) {
        if (!node.has(key) || !node.get(key).isJsonObject()) return null;
        return walkSelector(node.getAsJsonObject(key), cmd);
    }

    private boolean isCmdProperty(JsonObject node) {
        if (!node.has("property")) return true;
        String prop = stripMinecraftPrefix(asString(node.get("property")));
        return "custom_model_data".equals(prop);
    }

    private Identifier findTexture(String modelRef, Set<String> visited) {
        if (modelRef == null || !visited.add(modelRef)) return null;
        Identifier id = Identifier.tryParse(modelRef);
        if (id == null) return null;
        Identifier modelFileId = Identifier.of(
                id.getNamespace(),
                "models/" + id.getPath() + ".json"
        );
        JsonObject model = readJson(modelFileId);
        if (model == null) return null;

        if (model.has("textures") && model.get("textures").isJsonObject()) {
            String texRef = pickTextureRef(model.getAsJsonObject("textures"));
            if (texRef != null) {
                Identifier t = Identifier.tryParse(texRef);
                if (t != null) {
                    return Identifier.of(
                            t.getNamespace(),
                            "textures/" + t.getPath() + ".png"
                    );
                }
            }
        }

        if (model.has("parent")) {
            return findTexture(asString(model.get("parent")), visited);
        }
        return null;
    }

    private static String pickTextureRef(JsonObject textures) {
        for (String key : new String[] { "layer0", "0" }) {
            String v = readStringEntry(textures, key);
            if (v != null) return v;
        }
        for (Map.Entry<String, JsonElement> e : textures.entrySet()) {
            String v = primitiveString(e.getValue());
            if (v != null && !v.startsWith("#")) return v;
        }
        return null;
    }

    private static String readStringEntry(JsonObject obj, String key) {
        if (!obj.has(key)) return null;
        String v = primitiveString(obj.get(key));
        return v == null || v.startsWith("#") ? null : v;
    }

    private static String primitiveString(JsonElement el) {
        if (el == null || !el.isJsonPrimitive()) return null;
        JsonPrimitive p = el.getAsJsonPrimitive();
        return p.isString() ? p.getAsString() : null;
    }

    private static String asString(JsonElement el) {
        return el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()
                ? el.getAsString()
                : null;
    }

    private static String stripMinecraftPrefix(String s) {
        if (s == null) return null;
        int i = s.indexOf(':');
        if (i < 0) return s;
        return s.substring(0, i).equals("minecraft") ? s.substring(i + 1) : s;
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
