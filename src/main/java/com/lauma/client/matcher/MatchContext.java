package com.lauma.client.matcher;

import net.minecraft.nbt.NbtCompound;

public record MatchContext(String itemId, int customModelData, NbtCompound nbt, String displayName) {
    public MatchContext(String itemId, int customModelData, NbtCompound nbt) {
        this(itemId, customModelData, nbt, null);
    }
    public boolean hasCustomModelData() { return customModelData >= 0; }
    public boolean hasNbt() { return nbt != null && !nbt.isEmpty(); }
    public boolean hasDisplayName() { return displayName != null && !displayName.isEmpty(); }
}