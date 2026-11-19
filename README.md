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

## TypeScript model codegen (validation-only)

```bash
gradle :back:generateTs   # -> front/src/domain/generated/types.ts (from Kotlin serializers)
```

`front/src/domain/validate.ts` asserts, at the type level, that the hand-written models in
`front/src/domain/types.ts` are structurally identical to the generated ones. `vue-tsc` (run by
`npm run build-front`) checks it, so drift breaks the front build. The hand-written file remains what
the app imports until parity is proven.

After changing Kotlin models:

```bash
gradle :back:generateTs
git diff --exit-code front/src/domain/generated/types.ts   # CI guard
npm run build-front
```

Known mapped difference: `phase` is a numeric TypeScript enum on the front (`CahGamePhase`) and an
`Int` in Kotlin, so the comparison excludes that field and asserts the generated type is `number`.

## Front copy TODO

The front imports the card/question texts (`answers`, `questions`) and
`CahGamePhase`, `cahPlayerCardsCount`, `WordCase` from the old TS `cah-back`. Copy the texts into
`front/src/domain/texts/` and keep them in sync with the Kotlin resources (or generate one from the
other) before deleting the dependency.

`front/package.json` now depends on `boardgame-web-common` via `file:../../core/common`.
