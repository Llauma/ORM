package com.lauma.config;

import com.google.gson.JsonObject;

public class OverrideEntry {
    public String item;
    public int customModelData = -1;
    public JsonObject nbtCondition;
    public String name;     // optional: match against ItemStack.getName().getString()
    public String target;
    public String texture;

    public boolean hasCustomModelData() { return customModelData >= 0; }
    public boolean hasNbtCondition() { return nbtCondition != null; }
    public boolean hasName() { return name != null && !name.isEmpty(); }
    public boolean hasTarget() { return target != null && !target.isEmpty(); }

    public boolean isPerInstance() { return hasNbtCondition() || hasName(); }
}