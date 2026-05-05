package com.lauma.client.matcher;

import net.minecraft.nbt.NbtCompound;

public record MatchContext(String itemId, int customModelData, NbtCompound nbt) {
    public boolean hasCustomModelData() { return customModelData >= 0; }
    public boolean hasNbt() { return nbt != null && !nbt.isEmpty(); }
}