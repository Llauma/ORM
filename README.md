# Override Resource Manager (ORM)

A client-side Fabric mod for Minecraft **1.21.4** that lets you replace item textures and models at runtime — no resource pack required. Overrides live in plain files under `config/orm/` and reload with the resource stack (`F3+T`).

---

## Features

| Feature | Description |
|---|---|
| **Texture override** | Replace a vanilla item's texture with any `.png` from `config/orm/textures/` |
| **3D model override** | Swap an item's model with a Blockbench-exported `.json` from `config/orm/models/` |
| **Animated textures** | Drop a `.png.mcmeta` next to your texture to get MC-standard frame animation |
| **Per-instance matching** | Match by item name, CustomModelData, or NBT — multiple items of the same type can have different overrides |
| **Priority system** | More specific entries win: `item` < `item+name` < `item+cmd` < `item+cmd+nbt` |
| **Quick-add keybind** | Middle-click an item in any inventory screen to auto-generate its config entry |

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/) and [Fabric API](https://modrinth.com/mod/fabric-api) for Minecraft 1.21.4.
2. Drop the ORM `.jar` into your `mods/` folder.
3. Launch the game — `config/orm/` is created automatically.

---

## File Layout

```
config/orm/
├── overrides.json            ← main config (list of override entries)
├── models/
│   └── <name>.json           ← custom Blockbench/JSON models
└── textures/
    └── item/
        └── <name>.png        ← texture files (+ optional .png.mcmeta for animation)
```

Models and textures are referenced in `overrides.json` using the `orm:` namespace:

- `"model": "orm:sword"` → `config/orm/models/sword.json`
- `"texture": "orm:item/texture2d/apple"` → `config/orm/textures/item/texture2d/apple.png`

Subdirectories are fully supported and their structure is preserved:

```
config/orm/textures/item/sword/anim/animation.png
  → orm:item/sword/anim/animation
```

---

## overrides.json Reference

Each entry is a JSON object. Fields:

| Field | Type | Required | Description |
|---|---|---|---|
| `item` | string | **yes** | Minecraft item ID, e.g. `"minecraft:netherite_sword"` |
| `name` | string | no | Match by exact display name |
| `customModelData` | int | no | Match by CustomModelData value |
| `nbtCondition` | object | no | Match by NBT key/value pairs |
| `texture` | string | no | Texture override path: `"orm:<path>"` |
| `model` | string | no | Model override ID: `"orm:<name>"` |
| `target` | string | no | Explicit target texture ID from a modded item model |

At least one of `texture` or `model` should be set.

---

## Example Usage

The example below is taken from a real working config. It overrides two tools with custom Diablo-style 3D models (each with an animated texture layer) and replaces the Golden Apple with a flat 2D texture.

### `config/orm/overrides.json`

```json
[
  {
    "item": "minecraft:netherite_sword",
    "name": "Netherite Sword",
    "model": "orm:sword"
  },
  {
    "item": "minecraft:netherite_pickaxe",
    "name": "Netherite Pickaxe",
    "model": "orm:pickaxe"
  },
  {
    "item": "minecraft:golden_apple",
    "name": "Golden Apple",
    "texture": "orm:item/texture2d/apple"
  }
]
```

### File structure for the example above

```
config/orm/
├── overrides.json
├── models/
│   ├── sword.json
│   └── pickaxe.json
└── textures/
    └── item/
        ├── sword/
        │   ├── diablo_texture.png       ← static base texture
        │   └── anim/
        │       ├── animation.png        ← animated overlay frames
        │       └── animation.png.mcmeta ← frame timing
        ├── pickaxe/
        │   ├── diablo_texture.png
        │   └── anim/
        │       ├── animation.png
        │       └── animation.png.mcmeta
        └── texture2d/
            └── apple.png               ← flat 2D replacement
```

### How each entry works

**Netherite Sword / Pickaxe** — `"model": "orm:sword"` loads `models/sword.json`. The model itself references two texture layers:
- `orm:item/sword/diablo_texture` — a static 128×128 detailed texture
- `orm:item/sword/anim/animation` — an animated overlay driven by `animation.png.mcmeta`

Both are matched by display name (`"name": "Netherite Sword"`) so only items with that exact name are affected. Rename the item in an anvil to anything else and the vanilla model returns.

**Golden Apple** — `"texture": "orm:item/texture2d/apple"` replaces the vanilla `golden_apple.png` globally (no name filter), so every golden apple in the world uses `textures/item/texture2d/apple.png`.

### Animated texture `.mcmeta` format

```json
{
  "animation": {
    "frametime": 3,
    "frames": [0, 1, 2, 3]
  }
}
```

Place the `.mcmeta` file next to the `.png` with the same name plus `.mcmeta` suffix.

---

## Keybind: Quick-Add

Open any inventory screen and **middle-click** an item slot. ORM reads the item's ID, display name, and CustomModelData, appends a new entry to `overrides.json`, and prints a chat confirmation. The generated entry uses the `texture` field with a path derived from the item ID — replace the placeholder texture value with your actual file.

Default binding: **Middle Mouse Button** (rebindable under `Controls → Override Resource Manager`).

---

## Reloading

Press **F3+T** to reload resource packs. ORM re-reads `overrides.json`, rescans `models/` and `textures/`, and rebuilds the override registry without restarting the game.

---

## Requirements

- Minecraft 1.21.4
- Fabric Loader 0.19.2+
- Fabric API 0.119.4+1.21.4
