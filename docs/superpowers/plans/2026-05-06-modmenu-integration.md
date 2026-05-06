# ModMenu Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add ModMenu + Cloth Config GUI to OverrideResourceManager so users can add, edit, and delete override entries in-game.

**Architecture:** Three new classes in `com.lauma.modmenu` package — `ButtonEntry` (Cloth Config custom widget), `ORMModMenuIntegration` (ModMenuApi entrypoint), and `ORMConfigScreen` (screen builder). The screen holds a mutable `List<OverrideEntry>` in memory; before any structural change (Add/Remove), all typed field values are synced back to the list via field references, then the screen is rebuilt. Save writes the list to disk via the existing `ORMConfigManager.save()`.

**Tech Stack:** Fabric 1.21.4, ModMenu 13.x, Cloth Config 17.x, Java 21 records

---

## File Map

| Action | Path | Responsibility |
|--------|------|---------------|
| Modify | `build.gradle` | Add Maven repos + ModMenu/Cloth Config deps |
| Modify | `gradle.properties` | Add `modmenu_version`, `cloth_config_version` |
| Modify | `src/main/resources/fabric.mod.json` | Add `modmenu` entrypoint + `suggests` block |
| Create | `src/main/java/com/lauma/modmenu/ButtonEntry.java` | Custom Cloth Config entry that renders a Minecraft `ButtonWidget` |
| Create | `src/main/java/com/lauma/modmenu/ORMModMenuIntegration.java` | Implements `ModMenuApi`, returns screen factory |
| Create | `src/main/java/com/lauma/modmenu/ORMConfigScreen.java` | Builds the full Cloth Config screen with CRUD |

---

## Task 1: Build Setup

**Files:**
- Modify: `build.gradle`
- Modify: `gradle.properties`

> Note: Verify these versions are latest compatible with MC 1.21.4 on Modrinth before proceeding. ModMenu: https://modrinth.com/mod/modmenu/versions — Cloth Config: https://modrinth.com/mod/cloth-config/versions

- [ ] **Step 1: Add Maven repositories to `build.gradle`**

Replace the existing empty `repositories {}` block with:

```groovy
repositories {
    maven { url "https://maven.shedaniel.me/" }
    maven { url "https://maven.terraformersmc.com/releases" }
}
```

- [ ] **Step 2: Add dependencies to `build.gradle`**

Inside the existing `dependencies {}` block, after the `fabric-api` line, add:

```groovy
modImplementation "com.terraformersmc:modmenu:${modmenu_version}"
modImplementation "me.shedaniel.cloth:cloth-config-fabric:${cloth_config_version}"
include "me.shedaniel.cloth:cloth-config-fabric:${cloth_config_version}"
```

The `include` line jars-in-jar Cloth Config so it's bundled in the final mod. ModMenu is a user-provided dependency (not bundled).

- [ ] **Step 3: Add versions to `gradle.properties`**

At the bottom of `gradle.properties`, add:

```properties
modmenu_version=13.0.0
cloth_config_version=17.0.144
```

- [ ] **Step 4: Run Gradle refresh and verify compile**

```bash
./gradlew dependencies --configuration modImplementation
```

Expected: Cloth Config and ModMenu artifacts resolved without errors. If resolution fails, check version numbers on Modrinth and update `gradle.properties`.

- [ ] **Step 5: Commit**

```bash
git add build.gradle gradle.properties
git commit -m "build: add ModMenu and Cloth Config dependencies"
```

---

## Task 2: fabric.mod.json — Add Entrypoint and Suggests

**Files:**
- Modify: `src/main/resources/fabric.mod.json`

- [ ] **Step 1: Add `modmenu` entrypoint and `suggests` block**

Replace the entire file content with:

```json
{
	"schemaVersion": 1,
	"id": "orm",
	"version": "${version}",
	"name": "OverrideResourceManager",
	"description": "Client-side item texture override system keyed by item + custom_model_data + NBT.",
	"authors": [
		"Llauma"
	],
	"license": "CC0-1.0",
	"icon": "assets/orm/icon.png",
	"environment": "client",
	"entrypoints": {
		"main": [
			"com.lauma.OverrideResourceManager"
		],
		"client": [
			"com.lauma.OverrideResourceManager"
		],
		"modmenu": [
			"com.lauma.modmenu.ORMModMenuIntegration"
		]
	},
	"mixins": [
		"orm.mixins.json"
	],
	"depends": {
		"fabricloader": ">=0.19.2",
		"minecraft": "~1.21.4",
		"java": ">=21",
		"fabric-api": "*"
	},
	"suggests": {
		"modmenu": "*",
		"cloth-config": "*"
	}
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/fabric.mod.json
git commit -m "feat: add modmenu entrypoint to fabric.mod.json"
```

---

## Task 3: ButtonEntry — Custom Cloth Config Widget

**Files:**
- Create: `src/main/java/com/lauma/modmenu/ButtonEntry.java`

> This class extends Cloth Config's `AbstractConfigListEntry<Void>` to render a standard Minecraft `ButtonWidget` inside the config list. It has no value and does not participate in save.

- [ ] **Step 1: Create `ButtonEntry.java`**

```java
package com.lauma.modmenu;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

public class ButtonEntry extends AbstractConfigListEntry<Void> {

    private final ButtonWidget button;

    public ButtonEntry(Text label, Runnable action) {
        super(label, false);
        this.button = ButtonWidget.builder(label, b -> action.run())
            .dimensions(0, 0, 150, 20)
            .build();
    }

    @Override
    public Void getValue() { return null; }

    @Override
    public Optional<Void> getDefaultValue() { return Optional.empty(); }

    @Override
    public void save() {}

    @Override
    public boolean isRequiresRestart() { return false; }

    @Override
    public List<? extends Element> children() { return List.of(button); }

    @Override
    public List<? extends Selectable> selectableChildren() { return List.of(button); }

    @Override
    public void render(DrawContext context, int index, int y, int x, int entryWidth,
                       int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        button.setX(x + entryWidth / 2 - 75);
        button.setY(y);
        button.setWidth(150);
        button.render(context, mouseX, mouseY, tickDelta);
    }
}
```

> **If this doesn't compile:** Cloth Config 17 may have changed the `AbstractConfigListEntry` constructor. Try removing the `false` argument (`super(label)`) or check the Cloth Config source at https://github.com/shedaniel/cloth-config for the exact signature.

- [ ] **Step 2: Verify compile**

```bash
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL with no errors.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/lauma/modmenu/ButtonEntry.java
git commit -m "feat: add ButtonEntry for Cloth Config"
```

---

## Task 4: ORMModMenuIntegration — ModMenu API Entrypoint

**Files:**
- Create: `src/main/java/com/lauma/modmenu/ORMModMenuIntegration.java`

- [ ] **Step 1: Create `ORMModMenuIntegration.java`**

```java
package com.lauma.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ORMModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ORMConfigScreen::create;
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/lauma/modmenu/ORMModMenuIntegration.java
git commit -m "feat: add ORMModMenuIntegration entrypoint"
```

---

## Task 5: ORMConfigScreen — Full CRUD Screen Builder

**Files:**
- Create: `src/main/java/com/lauma/modmenu/ORMConfigScreen.java`

> **Design recap:**
> - `create(Screen parent)` loads current overrides from disk into a mutable list and calls `create(parent, list)`
> - `create(Screen parent, List<OverrideEntry> entries)` builds the Cloth Config screen
> - `fieldRefs` is a `List<EntryFields>` where each element holds all 7 field references for one entry
> - Before Add/Remove rebuilds the screen, `sync(fieldRefs, entries)` reads `getValue()` from all field refs and writes to the `entries` objects — this preserves in-progress typed values across rebuilds
> - `setSaveConsumer` on each field writes to `entry.field` when the Save button is clicked
> - `setSavingRunnable` creates a new `ORMConfig` from the `entries` list and persists it via `ORMConfigManager.save()`

- [ ] **Step 1: Create `ORMConfigScreen.java`**

```java
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

    public static Screen create(Screen parent, List<OverrideEntry> entries) {
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
                SystemToast.Type.PACK_COPY_FAILURE,
                Text.literal("ORM Config Saved"),
                Text.literal("Restart or press F3+T to apply")
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
```

- [ ] **Step 2: Build the mod**

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL. JAR created in `build/libs/`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/lauma/modmenu/ORMConfigScreen.java
git commit -m "feat: add ORMConfigScreen with full CRUD via Cloth Config"
```

---

## Task 6: Manual Verification in Game

> No automated tests are possible for Minecraft GUI code. Verify by running the game.

- [ ] **Step 1: Start the game via Gradle**

```bash
./gradlew runClient
```

- [ ] **Step 2: Verify ModMenu shows ORM config button**

In the main menu, open Mods. Find OverrideResourceManager. Confirm a "Config" button appears.

- [ ] **Step 3: Test Add Entry**

Click Config → click "+ Add Entry". Confirm a new `#1: ` subcategory appears.

- [ ] **Step 4: Test field editing**

Expand entry #1. Fill in `Item` = `minecraft:diamond_sword`, `Custom Model Data` = `1`, `Texture` = `diablo_weapon_set/sword`. Click Save.

Open `config/orm/overrides.json` — confirm it contains the entry.

- [ ] **Step 5: Test Remove Entry**

Reopen Config. Expand entry #1. Click "Remove Entry". Confirm entry disappears. Click Save. Confirm `overrides.json` is now empty.

- [ ] **Step 6: Test value persistence across Add clicks**

Add entry, fill in Item = `minecraft:bow`. Click "+ Add Entry". Confirm entry #1 still shows `minecraft:bow` pre-filled in the new screen.

- [ ] **Step 7: Final commit**

```bash
git add -A
git commit -m "feat: ModMenu + Cloth Config integration complete"
```
