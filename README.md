# boardgame-kotlin-cah

Kotlin Cards Against Humanity module and its Vue front.

## Layout

```
back/    :back    Kotlin game plugin (rules, state, bot actions)
front/            Vue Module Federation remote (copied from boardgame-web-cah/front)
```

## Status

Implemented: `back/` — full CAH rules (start, answers, voting, points, draw, round end, winners,
bots), texts as resources (`texts/questions.json`, `texts/meta.json`), tests.

Verified: `:back:test` and the end-to-end protocol scenario through the launcher.

Pending: `front/src/domain` extraction (the copied front still needs the texts/enums moved before the
old `cah-back` TS import can be removed).

## Build / test

```bash
gradle :back:test
gradle :back:jar        # -> back/build/libs/back-<version>.jar
```

## Development build

```bash
gradle devBuild         # :back:generateTs + npm run build-front + :back:jar
gradle devFront         # front only (-> front/dist)
```

The launcher's `DEV=true` watcher reloads the module when `back/build/libs/back-<version>.jar`
changes and copies `front/dist` for the front. Requires `npm` on `PATH`.

IntelliJ IDEA: the shared **Build CAH (dev)** run configuration (`.idea/runConfigurations`) runs
`devBuild`.

## Version

The version is resolved in this order:

1. `-PcahVersion=…`
2. `RELEASE_VERSION` environment variable
3. the latest reachable git tag (`v0.2.0` → `0.2.0`)
4. default `0.1.0`

Tag the release before building (`git tag v0.2.0 && git push --tags`, or create the release on GitHub
and run `git fetch --tags`); the **Build CAH release** configuration then produces
`cah-0.2.0.zip` and `back-0.2.0.jar` with `version.properties = 0.2.0` automatically. Pass
`-PcahVersion=…` in the run configuration's Arguments field for a one-off override.

## Release packaging

```bash
gradle releaseBuild     # :devFront + :back:releaseZip -> back/build/dist/cah-<version>.zip
gradle :back:releaseZip # same, but assumes front/dist is already built
```

The asset is `back/cah.jar` (classes + `classes.dex` + texts/service) and `front/**`, versioned in
the file name (`cah-<version>.zip`). It installs as `games-modules/cah/<version>/`, so a new release
does not overwrite the running one and the core loads the newest version.

IntelliJ IDEA: the shared **Build CAH release** run configuration runs `releaseBuild`.

## TypeScript model codegen

```bash
gradle :back:generateTs   # -> front/src/domain/generated/types.ts (from Kotlin serializers)
```

Models are discovered via `@GenerateTs` on the Kotlin classes (add `name`/`typeLiteral` when the TS
name or discriminator differs); the task scans compiled classes, so there is no manual list.

`front/src/domain/types.ts` re-exports the generated model types and keeps only front-only helpers
(`CahGamePhase`, `WordCase`, `QuestionPlaceHolder`, `cahPlayerCardsCount`). The generated file is the
source of truth for the wire model; `npm run build-front` fails if the Kotlin and front models drift
in shape.

After changing Kotlin models:

```bash
gradle :back:generateTs
git diff --exit-code front/src/domain/generated/types.ts   # CI guard
npm run build-front
```

`phase` is a number on the wire; the front keeps the numeric `CahGamePhase` enum for readable
constants and comparisons.

## Front copy TODO

The front imports the card/question texts (`answers`, `questions`) and
`CahGamePhase`, `cahPlayerCardsCount`, `WordCase` from the old TS `cah-back`. Copy the texts into
`front/src/domain/texts/` and keep them in sync with the Kotlin resources (or generate one from the
other) before deleting the dependency.

`front/package.json` now depends on `boardgame-web-common` via `file:../../core/common`.
