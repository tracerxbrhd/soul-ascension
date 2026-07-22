# Releasing Soul Ascension

The current Minecraft 26.2 release procedure, repository settings, tag format and publication order
are maintained in the repository-root [`RELEASING.md`](../RELEASING.md).

The short version is:

1. publish the matching U-API tag first;
2. wait until U-API is available on Modrinth;
3. merge Soul Ascension only after CI succeeds;
4. push the exact tag `v3.0.0-beta.1+mc26.2`.

That tag publishes the same JAR to GitHub Releases and Modrinth. CurseForge remains optional.
