# Releasing Soul Ascension

Soul Ascension builds against an exact U-API source ref, then publishes the JAR to GitHub Releases
and Modrinth. CurseForge remains optional.

## Repository settings

Create these GitHub Actions settings before pushing the port branch:

- secret `MODRINTH_TOKEN` and variable `MODRINTH_PROJECT_ID`;
- variable `U_API_MODRINTH_PROJECT_ID`;
- variable `U_API_REPOSITORY` (normally `tracerxbrhd/u-api`);
- variable `U_API_CI_REF` set to `port/26.2` while the U-API port is under review, then `master`;
- optional secret `U_API_REPOSITORY_TOKEN` only when U-API is private;
- optional secret `CURSEFORGE_TOKEN`, variable `CURSEFORGE_PROJECT_ID` and variable
  `U_API_CURSEFORGE_PROJECT_ID`.

`U_API_REF` is normally left unset: the release workflow derives
`v3.0.0-beta.2+mc26.2` from `gradle.properties`.

## Branch and release

First publish the matching U-API tag. Then push and merge Soul Ascension:

```text
git switch port/26.2
git add -A
git commit -m "Fix Soul Ascension startup on Minecraft 26.2"
git push -u github port/26.2
```

After the version branch passes `CI`, perform a local release dry-run from the synchronized branch.
Merging into `master` is optional and is not required by the release workflow:

```text
.\scripts\release.ps1 -DryRun
```

Then run `.\scripts\release.ps1` to create and push the exact tag safely, or use the equivalent
manual commands below:

```text
git tag -a v3.0.0-beta.2+mc26.2 -m "Soul Ascension 3.0.0-beta.2 for Minecraft 26.2"
git push github v3.0.0-beta.2+mc26.2
```

The tag triggers the GitHub/Modrinth release. The workflow declares U-API as a required Modrinth
dependency and marks this version as beta.
