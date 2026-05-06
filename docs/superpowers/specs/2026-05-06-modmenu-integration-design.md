# ModMenu Integration Design

**Date:** 2026-05-06
**Status:** Approved

## Summary

Add ModMenu + Cloth Config integration to OverrideResourceManager so users can manage override entries (full CRUD) through an in-game GUI. Changes are saved to `config/orm/overrides.json` and applied after F3+T or game restart.

## Dependencies

Add to `build.gradle`:
```gradle
modImplementation "com.terraformersmc:modmenu:${modmenu_version}"
modImplementation "me.shedaniel.cloth:cloth-config-fabric:${cloth_config_version}"
```

Add to `gradle.properties`:
```properties
modmenu_version=13.0.0
cloth_config_version=17.0.144
```

Both are **optional** — declared under `suggests` in `fabric.mod.json`, not `depends`. The mod functions normally without them.

## fabric.mod.json Changes

1. Add `modmenu` entrypoint pointing to `com.lauma.modmenu.ORMModMenuIntegration`
2. Add `suggests` block:
```json
"suggests": {
  "modmenu": "*",
  "cloth-config": "*"
}
```

## New Classes

### `com.lauma.modmenu.ORMModMenuIntegration`
Implements `ModMenuApi`. Returns `ORMConfigScreen::create` from `getModConfigScreenFactory()`.

### `com.lauma.modmenu.ORMConfigScreen`
Static `create(Screen parent)` method that builds and returns a Cloth Config screen.

**Screen structure:**
- One category: **"Overrides"**
- Top button: **"+ Add Entry"** — appends a blank `OverrideEntry` to the list
- Each entry rendered as a `SubCategoryBuilder` titled `#N: <item>`:
  - `StringField` → `item` (required)
  - `IntField` → `custom_model_data` (-1 = not set)
  - `StringField` → `name`
  - `StringField` → `target`
  - `StringField` → `texture`
  - `StringField` → `model`
  - `StringField` (multiline) → `nbt` (raw JSON string)
  - Button **"Remove"** — deletes this entry
- Standard Cloth Config **"Save"** button calls `ORMConfigManager.save()` then shows toast: "Restart or press F3+T to apply changes"

The entry list is built dynamically on screen open by calling `ORMConfigManager.load()`.

## Modified Files

| File | Change |
|---|---|
| `build.gradle` | Add ModMenu + Cloth Config deps |
| `gradle.properties` | Add `modmenu_version`, `cloth_config_version` |
| `fabric.mod.json` | Add `modmenu` entrypoint + `suggests` block |

## Isolation

`ORMModMenuIntegration` and `ORMConfigScreen` live in the `com.lauma.modmenu` package and are never imported from core code. If ModMenu is absent, the entrypoint is never called and there are no classloading issues.

`ORMConfigManager` is unchanged — `save()` already exists and is reused directly.

## Apply Behavior

Changes are written to disk on Save. No hot-reload. User applies via F3+T or game restart.
