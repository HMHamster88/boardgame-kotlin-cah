# boardgame-kotlin-cah

Kotlin Cards Against Humanity module and its Vue front.

## Layout

```
back/    :back    Kotlin game plugin (rules, state, bot actions)
front/            Vue Module Federation remote (copied from boardgame-web-cah/front)
```

## Status

Scaffold: `front/` copied; the Kotlin module is not created yet and the front does not compile until
the domain extraction below is done.

## Front copy TODO

The front imports the card/question texts (`answers`, `questions`) and
`CahGamePhase`, `cahPlayerCardsCount`, `WordCase` from the old TS `cah-back`. Copy the texts into
`front/src/domain/texts/` and keep them in sync with the Kotlin resources (or generate one from the
other) before deleting the dependency.

`front/package.json` now depends on `boardgame-web-common` via `file:../../core/common`.
