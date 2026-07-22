# Epic Fight compatibility on Minecraft 26.2

Epic Fight does not currently publish an API artifact compatible with Minecraft 26.2. Soul
Ascension 3.0 therefore ships without direct Epic Fight linkage, a compile dependency or release
metadata that advertises a compatible Epic Fight version.

The optional-integration bootstrap is deliberately fail-closed. If a mod with ID `epicfight` is
present, Soul Ascension logs that the 26.2 bridge is unavailable and continues without applying or
synchronizing Epic Fight attributes. Core progression remains available.

Existing generated `attribute_rewards.json` entries with `required_mod: "epicfight"` are retained as
dormant data for a future bridge. They must not be described as working compatibility on 26.2.

## Restoring the integration later

Only restore the bridge after Epic Fight publishes an official build for the same Minecraft and
NeoForge line. A compatibility port must then:

1. add the official artifact as `compileOnly`, never bundle its classes or JAR;
2. implement the version-specific bridge using supported public API;
3. advertise Epic Fight as optional and constrain metadata to tested versions;
4. test startup both with and without Epic Fight on client and dedicated server;
5. verify allocation, respec, login, respawn, dimension changes and weapon swaps;
6. confirm damage-based Soul XP is awarded exactly once per eligible hit.

Until all checks pass, keep the integration disabled and publish no Epic Fight compatibility claim.
