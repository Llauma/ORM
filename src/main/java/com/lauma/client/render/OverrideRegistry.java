package com.lauma.client.render;

import com.lauma.config.OverrideEntry;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OverrideRegistry {
    public static final OverrideRegistry INSTANCE = new OverrideRegistry();
    private final Map<String, Identifier> spriteIdByFingerprint = new LinkedHashMap<>();
    private OverrideRegistry() {}

    public synchronized void clear() { spriteIdByFingerprint.clear(); }

    public synchronized void register(OverrideEntry entry) {
        if (!entry.isPerInstance()) return;
        String fp = OverrideFingerprint.of(entry);
        spriteIdByFingerprint.put(fp, Identifier.of("minecraft", "orm_overrides/" + fp));
    }

    public synchronized Identifier getSpriteId(OverrideEntry entry) {
        if (!entry.isPerInstance()) return null;
        return spriteIdByFingerprint.get(OverrideFingerprint.of(entry));
    }

    public synchronized Map<String, Identifier> snapshot() { return Map.copyOf(spriteIdByFingerprint); }
}