// Model types are generated from the Kotlin back-end (`gradle :back:generateTs`).
// Only front-only helpers (numeric phase constants, Russian case enum) stay hand-written here.
export * from './generated/types'

export const cahPlayerCardsCount = 10

export enum CahGamePhase {
    PLAYERS_CHOOSE_ANSWERS,
    VOTING_FOR_ANSWERS,
    SHOW_ROUND_RESULTS
}

export enum WordCase {
    NOMINATIVE_CASE = 'NOMINATIVE_CASE',
    GENITIVE_CASE = 'GENITIVE_CASE',
    DATIVE_CASE = 'DATIVE_CASE',
    ACCUSATIVE_CASE = 'ACCUSATIVE_CASE',
    INSTRUMENTAL_CASE = 'INSTRUMENTAL_CASE',
    PREPOSITIONAL_CASE = 'PREPOSITIONAL_CASE'
}

export interface QuestionPlaceHolder {
    case: WordCase | undefined
}
