# OverrideResourceManager (ORM) — Архитектурный план (Fabric 1.21.4)

## 🧠 Общая концепция

Мод представляет собой систему точечного переопределения текстур предметов на клиенте.

Ключ идентификации предмета:
item + custom_model_data + nbt (частично/полностью)

Override применяется на этапе рендера (через ItemRenderer), а не через глобальную замену ресурсов.

---

## 📦 Архитектура проекта

orm/
 ├── OverrideResourceManagerMod
 │
 ├── client/
 │    ├── ORMClientInit
 │
 │    ├── input/
 │    │     └── ItemSelectionHandler
 │
 │    ├── render/
 │    │     ├── ORMRenderHook
 │    │     ├── TextureOverrideManager
 │    │     ├── TextureCache
 │    │     └── SpriteLoader
 │
 │    ├── matcher/
 │    │     ├── ItemMatcher
 │    │     ├── MatchContext
 │    │     └── MatchPriorityResolver
 │
 │    ├── mixin/
 │    │     ├── MixinItemRenderer
 │    │     └── MixinResourceReload
 │
 ├── config/
 │    ├── ORMConfig
 │    ├── OverrideEntry
 │    └── ORMConfigManager
 │
 ├── nbt/
 │    ├── NbtExtractor
 │    └── NbtFingerprintResolver
 │
 ├── util/
 │    └── ItemStackUtils

---

## 🔑 Ключевые системы

### MatchContext
Контекст, создаваемый из ItemStack при рендере.
Содержит: item id, custom_model_data, NBT fingerprint.

### OverrideEntry
Описывает правило override:
- item (обязательно)
- custom_model_data (опционально)
- nbt условие (опционально)
- путь к текстуре

### ItemMatcher
Сравнение MatchContext и OverrideEntry.

### MatchPriorityResolver
Приоритеты:
1. item + cmd + nbt
2. item + cmd
3. item + nbt
4. item

### TextureOverrideManager
Главный сервис:
- принимает ItemStack
- создаёт MatchContext
- находит override
- возвращает текстуру

### TextureCache
Кэш текстур.

### SpriteLoader
Загрузка текстур с диска и регистрация.

### NbtExtractor
Извлечение custom_model_data и NBT.

### NbtFingerprintResolver
Создание уникального отпечатка предмета.

---

## 🎨 Рендер пайплайн

ItemRenderer (mixin)
→ ORMRenderHook
→ TextureOverrideManager
→ MatchContext
→ ItemMatcher + PriorityResolver
→ TextureCache / SpriteLoader
→ Возврат текстуры

---

## 🖱️ Выбор предмета

ItemSelectionHandler:
- middle click
- получение ItemStack
- извлечение данных
- запись в конфиг

---

## 🔁 Reload

MixinResourceReload:
- F3+T
- очистка кэша
- перезагрузка конфига

---

## ⚙️ Конфиг

ORMConfigManager:
- загрузка
- сохранение
- валидация

ORMConfig:
- список OverrideEntry

---

## ⚠️ Решения

- не использовать ResourceManager override
- использовать MatchContext
- не хранить полный NBT
- использовать кэш

---

## 🚀 Этапы

1. Структура
2. Конфиг
3. MatchContext
4. Matcher
5. Cache + Loader
6. Mixin
7. Override
8. Input
9. Reload
10. Оптимизация

---

## ✔️ Итог

ItemStack → MatchContext → OverrideEntry → Texture
