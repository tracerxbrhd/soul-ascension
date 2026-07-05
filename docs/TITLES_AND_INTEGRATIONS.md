# SOUL ASCENSION: титулы и вкладки интеграций

## Пользовательские титулы

Встроенный набор содержит только уровневые титулы за уровни 1, 10, 25, 50, 75 и 100. Все они используют тот же data-driven формат, поэтому пороги и названия можно заменить datapack/resource pack.

Титулы загружаются из каждого namespace по пути:

```text
data/<namespace>/level_titles/<id>.json
```

Пример:

```json
{
  "name": "title.example.dragon_hunter",
  "description": "title.example.dragon_hunter.description",
  "icon": "example:textures/gui/titles/dragon_hunter.png",
  "order": 50,
  "hidden": false,
  "required_mods": ["example_bosses"],
  "conditions": {
    "minimum_level": 25,
    "stats": {
      "strength": 10,
      "endurance": 5
    },
    "entity_kills": {
      "example_bosses:dragon": 1
    },
    "items_collected": {
      "minecraft:diamond": 32
    },
    "blocks_mined": {
      "minecraft:ancient_debris": 16
    },
    "required_titles": ["soul_ascension:soulforged"],
    "play_time_ticks": 72000
  }
}
```

Все условия внутри одного титула должны выполниться одновременно. Поддерживаются:

- `minimum_level`;
- `stats`: `strength`, `endurance`, `agility`, `intelligence`, `perception`;
- `entity_kills` по ID типа сущности;
- `items_collected` по ID подобранного предмета;
- `blocks_mined` по ID блока;
- `required_titles`;
- `play_time_ticks`;
- `required_mods`.

Счётчики убийств, подбора и добычи начинают накапливаться после установки этой версии SOUL ASCENSION. Открытый титул сохраняется, даже если характеристика позже была уменьшена. Нажатие на выбранный титул снимает его.

PNG титула может иметь любое разрешение, но рекомендуется 32×32. Встроенная заглушка: `soul_ascension:textures/gui/icons/title.png`.
Если титул распространяется обычным datapack, его переводы и PNG необходимо положить в сопровождающий resource pack. Мод-аддон может хранить `data/.../level_titles` и `assets/...` в одном JAR.

## Динамические атрибуты

Экран перебирает все `AttributeInstance`, которыми реально владеет клиентский игрок. Поэтому существующие атрибуты других модов появляются автоматически, а отсутствующие registry ID не создают ошибок.

В `config/uapi/soul-ascension/client.toml` доступны:

- `ui.hiddenAttributes` — скрытые ID;
- `ui.visibleAttributes` — явное включение с приоритетом над hidden-списком;
- `ui.attributeCategories` — переопределения вида `attribute_id=category`.

Аддоны могут регистрировать категории и правила через U-API `AttributeDisplayRegistry`. Порядок выбора категории: U-API rule, клиентский config, категория config-driven награды, безопасная эвристика, `other`.

## Вкладки интеграций

Мод предоставляет клиентский реестр `CharacterIntegrationRegistry`. Совместимый мод может заменить встроенную диагностическую вкладку и показать типизированные данные своего API:

```java
CharacterIntegrationRegistry.register(new CharacterIntegrationRegistry.Tab(
    ResourceLocation.fromNamespaceAndPath("example", "character"),
    ResourceLocation.fromNamespaceAndPath("example", "textures/gui/icon.png"),
    Component.literal("Example"),
    100,
    () -> ModList.get().isLoaded("example"),
    player -> List.of(Component.literal("Class: Mage"))
));
```

Встроенная отдельная вкладка оставлена только для Origins/NeoOrigins, потому что раса и класс — уникальные данные, а не дубликат списка атрибутов. Атрибуты Iron's Spells, Apotheosis/Apothic Attributes и других модов автоматически отображаются на общей категоризированной странице атрибутов. Для Origins показываются доступные теги; полное имя расы, класса и описание должен предоставить небольшой адаптер для конкретной реализации Origins, поскольку разные NeoForge-порты используют несовместимые API.
