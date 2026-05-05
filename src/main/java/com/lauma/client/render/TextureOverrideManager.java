package com.lauma.client.render;

import com.lauma.OverrideResourceManager;
import com.lauma.client.matcher.MatchContext;
import com.lauma.client.matcher.MatchPriorityResolver;
import com.lauma.config.ORMConfig;
import com.lauma.config.ORMConfigManager;
import com.lauma.config.OverrideEntry;
import com.lauma.nbt.NbtExtractor;
import com.lauma.nbt.NbtFingerprintResolver;
import com.lauma.util.ItemStackUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TextureOverrideManager {
    public static final TextureOverrideManager INSTANCE = new TextureOverrideManager();

    private ORMConfig config = new ORMConfig();
    private final TextureCache cache = new TextureCache();
    private final Set<String> diagnosedItems = new HashSet<>();

    public void reload() {
        cache.clear();
        config = ORMConfigManager.load();
        OverrideResourceManager.LOGGER.info("ORM: config reloaded, {} entries", config.overrides.size());
    }

    public Identifier resolveTexture(ItemStack stack) {
        Optional<OverrideEntry> match = resolveEntry(stack);
        if (match.isEmpty()) return null;

        String texturePath = match.get().texture;
        int cmd = NbtExtractor.getCustomModelData(stack);
        NbtCompound nbt = NbtExtractor.getCustomNbt(stack);
        String cacheKey = NbtFingerprintResolver.resolve(cmd, nbt) + "|" + texturePath;
        if (cache.contains(cacheKey)) return cache.get(cacheKey);

        Identifier id = SpriteLoader.loadAndRegister(texturePath);
        cache.put(cacheKey, id);
        return id;
    }

    public Optional<OverrideEntry> resolveEntry(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        String itemId = ItemStackUtils.getItemId(stack);
        int cmd = NbtExtractor.getCustomModelData(stack);
        NbtCompound nbt = NbtExtractor.getCustomNbt(stack);
        String displayName = ItemStackUtils.getDisplayName(stack);

        MatchContext ctx = new MatchContext(itemId, cmd, nbt, displayName);
        Optional<OverrideEntry> result = MatchPriorityResolver.resolve(ctx, config.overrides);

        if (result.isEmpty()) {
            boolean hasEntryForThisItem = false;
            for (OverrideEntry e : config.overrides) {
                if (itemId.equals(e.item)) { hasEntryForThisItem = true; break; }
            }
            if (hasEntryForThisItem) {
                synchronized (diagnosedItems) {
                    if (diagnosedItems.add(itemId)) {
                        OverrideResourceManager.LOGGER.info(
                                "ORM[diagnose] {} no match. ctx_cmd={}, ctx_name={}, ctx_nbt={}",
                                itemId, cmd, displayName, (nbt == null ? "null" : nbt.toString())
                        );
                        for (OverrideEntry e : config.overrides) {
                            if (!itemId.equals(e.item)) continue;
                            OverrideResourceManager.LOGGER.info(
                                    "ORM[diagnose]   entry: cmd={}, name={}, nbtCondition={}",
                                    e.customModelData, e.name,
                                    (e.nbtCondition == null ? "null" : e.nbtCondition.toString())
                            );
                        }
                    }
                }
            }
        }
        return result;
    }

    public ORMConfig getConfig() { return config; }
}