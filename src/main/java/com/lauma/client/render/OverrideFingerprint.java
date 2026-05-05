package com.lauma.client.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lauma.config.OverrideEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OverrideFingerprint {
    private OverrideFingerprint() {}

    public static String of(OverrideEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(sanitize(entry.item != null ? entry.item : "unknown"));
        if (entry.hasCustomModelData()) sb.append("__cmd_").append(entry.customModelData);
        if (entry.hasNbtCondition()) sb.append("__nbt_").append(canonicalize(entry.nbtCondition));
        return sb.toString();
    }

    private static String canonicalize(JsonObject obj) {
        StringBuilder sb = new StringBuilder();
        appendObject(obj, sb);
        return Integer.toHexString(sb.toString().hashCode() & 0x7fffffff);
    }

    private static void appendObject(JsonObject obj, StringBuilder sb) {
        sb.append('{');
        List<String> keys = new ArrayList<>(obj.keySet());
        Collections.sort(keys);
        boolean first = true;
        for (String k : keys) {
            if (!first) sb.append(',');
            first = false;
            sb.append(sanitize(k)).append(':');
            JsonElement v = obj.get(k);
            if (v.isJsonObject()) appendObject(v.getAsJsonObject(), sb);
            else if (v.isJsonPrimitive()) sb.append(sanitize(v.getAsString()));
            else sb.append(v.toString());
        }
        sb.append('}');
    }

    private static String sanitize(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                || c == '_' || c == '-' || c == '.') out.append(c);
            else out.append('_');
        }
        return out.toString().toLowerCase();
    }
}