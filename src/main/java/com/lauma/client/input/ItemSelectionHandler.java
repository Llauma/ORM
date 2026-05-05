package com.lauma.client.input;

import com.google.gson.JsonObject;
import com.lauma.OverrideResourceManager;
import com.lauma.client.render.TextureOverrideManager;
import com.lauma.config.ORMConfig;
import com.lauma.config.ORMConfigManager;
import com.lauma.config.OverrideEntry;
import com.lauma.nbt.NbtExtractor;
import com.lauma.util.ItemStackUtils;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.screen.slot.Slot;

import java.lang.reflect.Field;

public class ItemSelectionHandler {
    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            ScreenMouseEvents.afterMouseClick(screen).register((s, mouseX, mouseY, button) -> {
                if (button != 2) return; // middle click only
                if (!(s instanceof HandledScreen<?> hs)) return;
                ItemStack stack = getHoveredStack(hs);
                if (stack == null || stack.isEmpty()) return;
                recordOverride(stack);
            });
        });
    }

    private static ItemStack getHoveredStack(HandledScreen<?> screen) {
        try {
            // Yarn-mapped field: focusedSlot (protected in HandledScreen)
            Field f = HandledScreen.class.getDeclaredField("focusedSlot");
            f.setAccessible(true);
            Object slot = f.get(screen);
            if (slot instanceof Slot s) return s.getStack();
        } catch (Exception ignored) {}
        return null;
    }

    private static void recordOverride(ItemStack stack) {
        String itemId = ItemStackUtils.getItemId(stack);
        int cmd = NbtExtractor.getCustomModelData(stack);
        NbtCompound nbt = NbtExtractor.getCustomNbt(stack);

        OverrideEntry entry = new OverrideEntry();
        entry.item = itemId;
        if (cmd >= 0) entry.customModelData = cmd;
        if (nbt != null && !nbt.isEmpty()) {
            entry.nbtCondition = nbtToJson(nbt);
        }
        // Placeholder texture path — user fills in actual PNG name
        String texName = itemId.replace(":", "/") + (cmd >= 0 ? "_" + cmd : "");
        entry.texture = "orm:" + texName;

        ORMConfig config = TextureOverrideManager.INSTANCE.getConfig();
        config.overrides.add(entry);
        ORMConfigManager.save(config);
        OverrideResourceManager.LOGGER.info("ORM: recorded override for {} → {}", itemId, entry.texture);
    }

    private static JsonObject nbtToJson(NbtCompound nbt) {
        JsonObject obj = new JsonObject();
        for (String key : nbt.getKeys()) {
            NbtElement el = nbt.get(key);
            if (el instanceof NbtString) {
                obj.addProperty(key, el.asString());
            } else if (el instanceof AbstractNbtNumber num) {
                obj.addProperty(key, num.doubleValue());
            } else if (el instanceof NbtCompound nested) {
                obj.add(key, nbtToJson(nested));
            }
            // Arrays/lists skipped for simplicity
        }
        return obj;
    }
}