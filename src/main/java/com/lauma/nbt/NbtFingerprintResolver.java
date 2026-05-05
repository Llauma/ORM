package com.lauma.nbt;

import net.minecraft.nbt.NbtCompound;

public class NbtFingerprintResolver {
    public static String resolve(int customModelData, NbtCompound nbt) {
        StringBuilder sb = new StringBuilder();
        sb.append("cmd=").append(customModelData);
        if (nbt != null && !nbt.isEmpty()) {
            sb.append("|nbt=").append(nbt);
        }
        return sb.toString();
    }
}
