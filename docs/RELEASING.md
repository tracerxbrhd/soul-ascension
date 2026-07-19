# Releasing Soul Ascension

Release publishing is tag-driven. Normal pushes and pull requests do not publish anything.

## GitHub setup

Create these repository secrets:

- `MODRINTH_TOKEN` — Modrinth API token. This is mandatory.
- `CURSEFORGE_TOKEN` — CurseForge API token. Optional; only needed when publishing to CurseForge.

Create these repository variables:

- `MODRINTH_PROJECT_ID` — Soul Ascension project ID or slug on Modrinth. This is mandatory.
- `CURSEFORGE_PROJECT_ID` — Soul Ascension project ID on CurseForge. Optional.
- `U_API_MODRINTH_PROJECT_ID` — U-API project ID or slug on Modrinth. This is mandatory because U-API is a required dependency.
- `U_API_CURSEFORGE_PROJECT_ID` — U-API project ID on CurseForge. Required only when CurseForge publishing is configured.
- `U_API_REPOSITORY` — optional GitHub repository to checkout for compile-time U-API sources, for example `tracerxbrhd/u-api`. Defaults to `<owner>/u-api`.
- `U_API_REF` — optional branch, tag or commit to checkout from `U_API_REPOSITORY`. Defaults to `v<u_api_version>+mc<minecraft_version>`, for example `v2.0.0+mc1.21.1`.

If the U-API repository is private and the default `GITHUB_TOKEN` cannot read it, create this repository secret:

- `U_API_REPOSITORY_TOKEN` — GitHub token with read access to the U-API repository.

Secrets are encrypted and intended for tokens. Variables are non-secret repository configuration such as project IDs.

## Release order for connected mods

Publish connected versions in this order:

1. Release U-API.
2. Verify that U-API is available on Modrinth.
3. Update and test the U-API dependency used by Soul Ascension if needed.
4. Release Soul Ascension.

Soul Ascension must not be published with a dependency on a U-API version that is not available to users yet.

The GitHub workflow checks out U-API into `u-api/` inside the Soul Ascension workspace for compile-time sources. Local development still uses the sibling `../u-api` checkout when it exists.

## Optional Epic Fight compatibility

Soul Ascension 2.0 compiles its optional bridge against Epic Fight `21.17.3.1`. Release metadata
must keep Epic Fight optional with the supported range `>=21.17.3.1` and `<21.18`; it must never be
published as a required runtime dependency. Before publishing, verify both configurations:

- build and start without Epic Fight;
- compile and start with a supported Epic Fight `21.17.x` release;
- inspect the Soul Ascension JAR and confirm it contains no Epic Fight classes, assets or nested JAR;
- confirm damage-based Soul XP is awarded exactly once for an Epic Fight hit;
- confirm stat allocation, respec, login, respawn and dimension change refresh Epic Fight attributes
  and clamp current stamina through the supported capability API;
- generate a clean 2.0 `attribute_rewards.json` and confirm all four native entries are present;
- confirm starting with an edited current-format file does not inject or overwrite native entries.

The full integration and configuration contract is in
[`EPIC_FIGHT_INTEGRATION.md`](EPIC_FIGHT_INTEGRATION.md).

## Local dry-run

Run from the repository root:

```powershell
.\scripts\release.ps1 -DryRun
```

Beta and alpha dry-runs:

```powershell
.\scripts\release.ps1 -Channel beta -DryRun
.\scripts\release.ps1 -Channel alpha -DryRun
```

Dry-run performs the same local checks and clean Gradle build, then stops before creating or pushing a tag.

## Normal release

```powershell
git status
git add .
git commit -m "Release 2.0.0"
git push github <branch>
.\scripts\release.ps1
```

Beta:

```powershell
.\scripts\release.ps1 -Channel beta
```

Alpha:

```powershell
.\scripts\release.ps1 -Channel alpha
```

The script creates and pushes an annotated tag only after all checks pass.

## Tag format

Release:

```text
v2.0.0+mc1.21.1
```

Beta/alpha:

```text
v2.0.0-beta+mc1.21.1
v2.0.0-alpha+mc1.21.1
```

The internal `gradle.properties` values remain separate:

```properties
mod_version=2.0.0
minecraft_version=1.21.1
u_api_version=2.0.0
u_api_version_range=[2.0.0,3.0.0)
release_remote=github
```

The built user-facing JAR includes the Minecraft suffix, for example:

```text
soul-ascension-2.0.0+mc1.21.1.jar
```

The Maven coordinate version remains the plain mod version, so the local U-API dependency stays compatible with the existing Gradle configuration.

## What the local script checks

- It is running inside this Git repository and from the repository root.
- `gradle.properties` and `gradlew.bat` exist.
- The current branch is not detached.
- The working tree has no modified or untracked files.
- The current branch exists on the configured `release_remote`.
- Local `HEAD` exactly matches `<release_remote>/<branch>`.
- `mod_version`, `minecraft_version`, `u_api_version`, `u_api_version_range` and `mod_name` are readable.
- The target tag does not exist locally or on the configured `release_remote`.
- `.\gradlew.bat clean build` succeeds.

The script never commits source files, pushes branches, rewrites tags, or creates GitHub Releases locally.

## What GitHub Actions does

When the tag is pushed, `.github/workflows/release.yml`:

1. validates that the tag matches `gradle.properties`;
2. validates mandatory Modrinth settings and required U-API dependency settings;
3. builds from the tagged commit on GitHub;
4. selects exactly one user-facing JAR from `build/libs`;
5. publishes to Modrinth with U-API marked as a required dependency;
6. publishes to CurseForge if both CurseForge settings are present, also marking U-API as required;
7. creates the GitHub Release and uploads the same JAR;
8. writes a release summary.

## Recovery after errors

If the local build fails, fix the project, commit the fix, push the branch and run the script again.

If the tag push fails, the script removes the local tag automatically. If you need to remove a mistaken tag manually:

```powershell
git tag -d v2.0.0+mc1.21.1
git push github :refs/tags/v2.0.0+mc1.21.1
```

Do not reuse a published tag for a different commit. Users, GitHub Releases and publishing platforms can cache tag state. Prefer increasing the patch version and publishing a new release.

If Modrinth rejects a file, fix the cause, bump the patch version and publish a new tag. The workflow intentionally fails before creating the final GitHub Release when mandatory Modrinth publication or the required U-API Modrinth dependency is not configured.

## Multiple Minecraft versions

Tags are unique across the entire repository:

```text
v2.0.0+mc1.21.1
v2.0.0+mc1.21.4
```

The workflow only compares previous tags with the same `+mc<version>` suffix when generating fallback release notes.
