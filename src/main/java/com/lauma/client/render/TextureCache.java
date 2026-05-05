package com.lauma.client.render;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class TextureCache {
    private final Map<String, Identifier> cache = new HashMap<>();

    public void put(String key, Identifier id) { cache.put(key, id); }
    public Identifier get(String key) { return cache.get(key); }
    public boolean contains(String key) { return cache.containsKey(key); }
    public void clear() { cache.clear(); }
}