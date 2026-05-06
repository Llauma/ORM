package com.lauma.modmenu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lauma.config.ORMConfig;
import com.lauma.config.ORMConfigManager;
import com.lauma.config.OverrideEntry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ORMConfigScreen {

    record EntryFields(
        AbstractConfigListEntry<String> item,
        AbstractConfigListEntry<Integer> cmd,
        AbstractConfigListEntry<String> name,
        AbstractConfigListEntry<String> target,
        AbstractConfigListEntry<String> texture,
        AbstractConfigListEntry<String> model,
        AbstractConfigListEntry<String> nbt
    ) {
        void applyTo(OverrideEntry entry) {
            entry.item = item.getValue();
            entry.customModelData = cmd.getValue();
            entry.name = emptyToNull(name.getValue());
            entry.target = emptyToNull(target.getValue());
            entry.texture = emptyToNull(texture.getValue());
            entry.model = emptyToNull(model.getValue());
            entry.nbtCondition = parseNbt(nbt.getValue());
        }
    }

    public static Screen create(Screen parent) {
        return create(parent, new ArrayList<>(ORMConfigManager.load().overrides));
    }

    private static Screen create(Screen parent, List<OverrideEntry> entries) {
        List<EntryFields> fieldRefs = new ArrayList<>();

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.literal("ORM Overrides"));

        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory cat = builder.getOrCreateCategory(Text.literal("Overrides"));

        cat.addEntry(new ButtonEntry(Text.literal("+ Add Entry"), () -> {
            sync(fieldRefs, entries);
            entries.add(new OverrideEntry());
            MinecraftClient.getInstance().setScreen(create(parent, entries));
        }));

        for (int i = 0; i < entries.size(); i++) {
            OverrideEntry entry = entries.get(i);
            final int idx = i;

            AbstractConfigListEntry<String> itemField = eb
                .startStrField(Text.literal("Item"), orEmpty(entry.item))
                .setDefaultValue("")
                .setSaveConsumer(v -> entry.item = v)
                .build();

            AbstractConfigListEntry<Integer> cmdField = eb
                .startIntField(Text.literal("Custom Model Data (-1 = off)"), entry.customModelData)
                .setDefaultValue(-1)
                .setSaveConsumer(v -> entry.customModelData = v)
                .build();

            AbstractConfigListEntry<String> nameField = eb
                .startStrField(Text.literal("Name (match)"), orEmpty(entry.name))
                .setDefaultValue("")
                .setSaveConsumer(v -> entry.name = emptyToNull(v))
                .build();

            AbstractConfigListEntry<String> targetField = eb
                .startStrField(Text.literal("Target texture ID"), orEmpty(entry.target))
                .setDefaultValue("")
                .setSaveConsumer(v -> entry.target = emptyToNull(v))
                .build();

            AbstractConfigListEntry<String> textureField = eb
                .startStrField(Text.literal("Texture (orm:<basename>)"), orEmpty(entry.texture))
                .setDefaultValue("")
                .setSaveConsumer(v -> entry.texture = emptyToNull(v))
                .build();

            AbstractConfigListEntry<String> modelField = eb
                .startStrField(Text.literal("Model (orm:<basename>)"), orEmpty(entry.model))
                .setDefaultValue("")
                .setSaveConsumer(v -> entry.model = emptyToNull(v))
                .build();

            AbstractConfigListEntry<String> nbtField = eb
                .startStrField(Text.literal("NBT condition (JSON)"), nbtToString(entry.nbtCondition))
                .setDefaultValue("")
                .setSaveConsumer(v -> entry.nbtCondition = parseNbt(v))
                .build();

            fieldRefs.add(new EntryFields(itemField, cmdField, nameField, targetField, textureField, modelField, nbtField));

            SubCategoryBuilder sub = eb.startSubCategory(
                Text.literal("#" + (i + 1) + ": " + orEmpty(entry.item))
            );
            sub.add(itemField);
            sub.add(cmdField);
            sub.add(nameField);
            sub.add(targetField);
            sub.add(textureField);
            sub.add(modelField);
            sub.add(nbtField);
            sub.add(new ButtonEntry(Text.literal("Remove Entry"), () -> {
                sync(fieldRefs, entries);
                entries.remove(idx);
                MinecraftClient.getInstance().setScreen(create(parent, entries));
            }));

            cat.addEntry(sub.build());
        }

        builder.setSavingRunnable(() -> {
            ORMConfig config = new ORMConfig();
            config.overrides = new ArrayList<>(entries);
            ORMConfigManager.save(config);
            SystemToast.add(
                MinecraftClient.getInstance().getToastManager(),
                SystemToast.Type.WORLD_BACKUP,
                Text.literal("ORM Config Saved"),
                Text.literal("Restart or press F3+T to apply changes")
            );
        });

        return builder.build();
    }

    private static void sync(List<EntryFields> refs, List<OverrideEntry> entries) {
        for (int i = 0; i < Math.min(refs.size(), entries.size()); i++) {
            refs.get(i).applyTo(entries.get(i));
        }
    }

    private static String orEmpty(String s) {
        return s != null ? s : "";
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    private static String nbtToString(JsonObject obj) {
        return obj != null ? obj.toString() : "";
    }

    private static JsonObject parseNbt(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try {
            return JsonParser.parseString(s).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }
}
