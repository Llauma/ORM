package com.lauma.config;

import com.google.gson.*;
import com.lauma.OverrideResourceManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ORMConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("orm");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("overrides.json");
    public static final Path TEXTURE_DIR = CONFIG_DIR.resolve("textures");

    public static ORMConfig load() {
        ORMConfig config = new ORMConfig();
        if (!Files.exists(CONFIG_FILE)) {
            save(config);
            return config;
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(CONFIG_FILE.toFile()), StandardCharsets.UTF_8)) {
            JsonArray array = GSON.fromJson(reader, JsonArray.class);
            if (array == null) return config;
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                OverrideEntry entry = new OverrideEntry();
                entry.item = obj.get("item").getAsString();
                if (obj.has("custom_model_data")) entry.customModelData = obj.get("custom_model_data").getAsInt();
                if (obj.has("nbt")) entry.nbtCondition = obj.get("nbt").getAsJsonObject();
                if (obj.has("target")) entry.target = obj.get("target").getAsString();
                entry.texture = obj.get("texture").getAsString();
                config.overrides.add(entry);
            }
        } catch (Exception e) {
            OverrideResourceManager.LOGGER.error("Failed to load ORM config", e);
        }
        return config;
    }

    public static void save(ORMConfig config) {
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.createDirectories(TEXTURE_DIR);
            JsonArray array = new JsonArray();
            for (OverrideEntry entry : config.overrides) {
                JsonObject obj = new JsonObject();
                obj.addProperty("item", entry.item);
                if (entry.hasCustomModelData()) obj.addProperty("custom_model_data", entry.customModelData);
                if (entry.hasNbtCondition()) obj.add("nbt", entry.nbtCondition);
                if (entry.hasTarget()) obj.addProperty("target", entry.target);
                obj.addProperty("texture", entry.texture);
                array.add(obj);
            }
            try (Writer w = new OutputStreamWriter(new FileOutputStream(CONFIG_FILE.toFile()), StandardCharsets.UTF_8)) {
                GSON.toJson(array, w);
            }
        } catch (Exception e) {
            OverrideResourceManager.LOGGER.error("Failed to save ORM config", e);
        }
    }
}