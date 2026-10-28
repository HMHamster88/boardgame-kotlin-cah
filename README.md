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

## Release packaging

```bash
npm run build-front     # -> front/dist
gradle :back:releaseZip # -> back/build/dist/cah.zip
```

The asset is `back/cah.jar` (classes + `classes.dex` + texts/service) and `front/**`. It is named
`cah.zip` on purpose: the core installs a game asset into `games-modules/<asset-name>/`, so the
directory (and therefore the front URL) stays stable across versions; the version lives in the tag
and in `version.properties`.

## Front copy TODO

The front imports the card/question texts (`answers`, `questions`) and
`CahGamePhase`, `cahPlayerCardsCount`, `WordCase` from the old TS `cah-back`. Copy the texts into
`front/src/domain/texts/` and keep them in sync with the Kotlin resources (or generate one from the
other) before deleting the dependency.

`front/package.json` now depends on `boardgame-web-common` via `file:../../core/common`.
