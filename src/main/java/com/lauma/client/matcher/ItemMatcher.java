package com.lauma.client.matcher;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lauma.config.OverrideEntry;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.Map;

public class ItemMatcher {
    public static boolean matches(MatchContext ctx, OverrideEntry entry) {
        if (!ctx.itemId().equals(entry.item)) return false;
        if (entry.hasCustomModelData() && ctx.customModelData() != entry.customModelData) return false;
        if (entry.hasName() && !entry.name.equals(ctx.displayName())) return false;
        if (entry.hasNbtCondition() && !nbtPartialMatch(ctx.nbt(), entry.nbtCondition)) return false;
        return true;
    }

    private static boolean nbtPartialMatch(NbtCompound nbt, JsonObject condition) {
        if (nbt == null || nbt.isEmpty()) return false;
        for (Map.Entry<String, JsonElement> e : condition.entrySet()) {
            String key = e.getKey();
            JsonElement expected = e.getValue();
            if (!nbt.contains(key)) return false;
            NbtElement actual = nbt.get(key);
            if (expected.isJsonObject() && actual instanceof NbtCompound nested) {
                if (!nbtPartialMatch(nested, expected.getAsJsonObject())) return false;
            } else if (expected.isJsonPrimitive()) {
                if (expected.getAsJsonPrimitive().isNumber()) {
                    if (!(actual instanceof AbstractNbtNumber num)) return false;
                    if (Math.abs(num.doubleValue() - expected.getAsDouble()) > 0.00001) return false;
                } else if (expected.getAsJsonPrimitive().isBoolean()) {
                    if (!(actual instanceof AbstractNbtNumber num)) return false;
                    if ((num.byteValue() != 0) != expected.getAsBoolean()) return false;
                } else {
                    if (!actual.asString().equals(expected.getAsString())) return false;
                }
            }
        }
        return true;
    }
}