package com.lauma.config;

import com.google.gson.JsonObject;

public class OverrideEntry {
    public String item;
    public int customModelData = -1;
    public JsonObject nbtCondition;
    public String name;     // optional: match against ItemStack.getName().getString()
    public String target;   // optional: explicit texture ID from the custom model (e.g. "ei:item/regen_stick")
    public String texture;  // optional: texture id (orm:<basename>) -> config/orm/textures/<basename>.png
    public String model;    // optional: model id (orm:<basename>) -> config/orm/models/<basename>.json

    public boolean hasCustomModelData() { return customModelData >= 0; }
    public boolean hasNbtCondition() { return nbtCondition != null; }
    public boolean hasName() { return name != null && !name.isEmpty(); }
    public boolean hasTarget() { return target != null && !target.isEmpty(); }
    public boolean hasTexture() { return texture != null && !texture.isEmpty(); }
    public boolean hasModel() { return model != null && !model.isEmpty(); }

    public boolean isPerInstance() { return hasNbtCondition() || hasName() || hasModel(); }
}