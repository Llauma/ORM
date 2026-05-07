package com.lauma.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class ChatUtils {
    private static final String PREFIX = "§b[ORM]§r ";

    private ChatUtils() {}

    public static void send(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        client.player.sendMessage(Text.literal(PREFIX + message), false);
    }

    public static void itemAdded(String itemId) {
        send("§aAdded override for §e" + itemId + "§a. Edit texture in config/orm/textures/, then press §cF3+T§a to reload.");
    }

    public static void modelAndTexturesFound(String itemId) {
        send("§a[OK] §e" + itemId + "§a: model + textures loaded.");
    }

    public static void modelLoaded(String itemId) {
        send("§a[OK] §e" + itemId + "§a: custom model loaded.");
    }

    public static void modelNoTextures(String itemId) {
        send("§e[WARN] §e" + itemId + "§e: model placed, textures not found.");
    }

    public static void texturesNotFound(String itemId) {
        send("§c[FAIL] §e" + itemId + "§c: textures not found.");
    }
}