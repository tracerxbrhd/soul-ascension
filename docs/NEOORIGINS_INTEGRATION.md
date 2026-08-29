# NeoOrigins integration status

## Current status

Native NeoOrigins identity display is **prepared but not enabled**. Soul Ascension does not
register a NeoOrigins profile provider, does not add a placeholder character tab, and does not
read player tags. A NeoOrigins-only installation therefore behaves exactly like an installation
without this integration, apart from one server log warning explaining why it is unavailable.

This is intentional. On 2026-07-29 the official NeoOrigins `v2.2.21+1.21.1` release was checked
both in source and in its published Modrinth JAR. The official Java API guide shows this call:

```java
Origin current = NeoOriginsAPI.currentOrigin(player, "neoorigins:origin");
```

The method is not present in the released `com.cyberday1.neoorigins.api.NeoOriginsAPI` class.
No published `v2.x` tag contains an implementation. The public API exposes active powers and
change/reload events, but powers cannot reliably identify the selected origin or class: origins
may have no powers and different origins may share powers.

Soul Ascension consequently does not use `OriginAttachments`, `PlayerOriginData`,
`OriginDataManager`, reflection, mixins, or any other NeoOrigins implementation detail.

Official references:

- [NeoOrigins repository](https://github.com/CyberDay1/NeoOrigins)
- [v2.2.21 Java API guide](https://github.com/CyberDay1/NeoOrigins/blob/v2.2.21/docs/JAVA_API.md)
- [v2.2.21 source tag](https://github.com/CyberDay1/NeoOrigins/tree/v2.2.21)
- [v2.2.21+1.21.1 Modrinth artifact](https://modrinth.com/mod/neo-origins/version/tqtCTcwO)

## Required upstream API

The minimum missing official method is:

```java
public static @Nullable Origin currentOrigin(
    ServerPlayer player,
    ResourceLocation layerId
);
```

It must be present in the published NeoOrigins JAR and return the currently selected, datapack-
resolved `Origin` for that layer, or `null` when no selection exists or the selected definition
was removed. An equivalent `Optional<Origin>` return type would also be sufficient.

For Soul Ascension the two queries would be:

```java
NeoOriginsAPI.currentOrigin(player, ResourceLocation.parse("neoorigins:origin"));
NeoOriginsAPI.currentOrigin(player, PowerLayers.CLASS_LAYER);
```

## Prepared architecture

- NeoOrigins is an optional `compileOnly` dependency pinned to exact version
  `v2.2.21+1.21.1`; it is not bundled into the Soul Ascension JAR.
- Optional loading is gated by mod ID before the isolated
  `compat.neoorigins` bootstrap class is referenced.
- U-API profile facets support bounded identity cards containing a semantic type, optional
  datapack ID, rich translatable name and description components, a defensive `ItemStack` icon,
  and bounded metadata.
- `NeoOriginsIdentityProjection` already maps official public `Origin` values into neutral U-API
  cards for Origin and Class, including Impact and explicit unselected states.
- The server remains the future source of truth. Clients will receive only the neutral snapshot;
  no client code will read NeoOrigins state directly.
- U-API keeps public, shared-group, and subject-only audiences. A future provider must use those
  existing privacy rules, so Soul Lens and public profiles cannot bypass visibility settings.

Once the accessor is published, the remaining activation work is deliberately small: query both
layers on the server, register a server-scoped U-API profile provider, and invalidate/rebuild the
projection on the public `OriginChangedEvent` and `OriginsLoadedEvent`. The projection is bounded
to two cards and should be rebuilt on those events or a profile request, not every render tick.

## Verification matrix after upstream activation

Activation must not be declared complete until all of these pass:

1. Dedicated server startup with NeoOrigins absent and with the supported NeoOrigins JAR present.
2. Origin and Class rendering with built-in definitions and with a custom datapack.
3. Name, description, `ItemStack` icon, namespace ID, and Impact localization.
4. Unselected Origin/Class states and removal of a selected datapack definition.
5. Immediate refresh after selection changes and `/reload`.
6. Public-profile and Soul Lens privacy for subject-only, shared-group, and public audiences.
7. Release-JAR inspection proving no `com/cyberday1/neoorigins/**` classes or assets were bundled.
