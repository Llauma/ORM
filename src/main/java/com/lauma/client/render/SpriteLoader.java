package com.lauma.client.render;

import com.lauma.OverrideResourceManager;
import com.lauma.config.ORMConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class SpriteLoader {
    /**
     * Loads PNG from config/orm/textures/{path}.png and registers it with TextureManager.
     * texturePath format: "namespace:path" or just "path" — namespace is ignored for file lookup.
     * File resolved as: config/orm/textures/wood_1.png for "orm:wood_1" or "wood_1".
     */
    public static Identifier loadAndRegister(String texturePath) {
        String[] parts = texturePath.split(":", 2);
        String namespace = parts.length == 2 ? parts[0] : "orm";
        String path = parts.length == 2 ? parts[1] : parts[0];

        Path filePath = ORMConfigManager.TEXTURE_DIR.resolve(path + ".png");
        if (!Files.exists(filePath)) {
            OverrideResourceManager.LOGGER.warn("ORM texture not found: {}", filePath);
            return null;
        }

        try (InputStream is = new FileInputStream(filePath.toFile())) {
            NativeImage image = NativeImage.read(is);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            Identifier id = Identifier.of("orm", "loaded/" + namespace + "/" + path);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
            return id;
        } catch (Exception e) {
            OverrideResourceManager.LOGGER.error("Failed to load ORM texture: {}", filePath, e);
            return null;
        }
    }
}