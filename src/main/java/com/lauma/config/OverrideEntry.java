package com.lauma.config;

import com.google.gson.JsonObject;

public class OverrideEntry {
    public String item;
    public int customModelData = -1;
    public JsonObject nbtCondition;
    public String target;   // optional: explicit texture ID from the custom model (e.g. "ei:item/regen_stick")
    public String texture;

    public boolean hasCustomModelData() { return customModelData >= 0; }
    public boolean hasNbtCondition() { return nbtCondition != null; }
    public boolean hasTarget() { return target != null && !target.isEmpty(); }
}