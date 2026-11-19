/**
 * Compile-time parity check: the hand-written model types in ./types.ts must match the types
 * generated from the Kotlin models (./generated/types.ts).
 *
 * This file is intentionally not imported anywhere; `vue-tsc` type-checks it as part of the project.
 * If the Kotlin models change, run `gradle :back:generateTs` and fix the drift here or in the models.
 */
import type {
    CahGameSettings,
    CahGamePublicState,
    CahSendAnswersAction,
    CahVoteForAnswerAction,
    CahDrawCardsAction,
    CahGamePrivateState,
    CahPrivatePlayerState,
    CahPublicPlayerState,
    EndRoundAction,
    PlayerAnswers
} from './types'
import type { GameStatistics } from 'boardgame-web-common'
import type * as Gen from './generated/types'

type Equals<A, B> = (<T>() => T extends A ? 1 : 2) extends (<T>() => T extends B ? 1 : 2) ? true : false
type Assert<T extends true> = T

export type SettingsMatch = Assert<Equals<CahGameSettings, Gen.CahGameSettings>>
export type PublicPlayerMatch = Assert<Equals<CahPublicPlayerState, Gen.CahPublicPlayerState>>
export type AnswersMatch = Assert<Equals<PlayerAnswers, Gen.PlayerAnswers>>
export type PrivatePlayerMatch = Assert<Equals<CahPrivatePlayerState, Gen.CahPrivatePlayerState>>
export type PrivateStateMatch = Assert<Equals<CahGamePrivateState, Gen.CahGamePrivateState>>
export type SendAnswersMatch = Assert<Equals<CahSendAnswersAction, Gen.CahSendAnswersAction>>
export type VoteMatch = Assert<Equals<CahVoteForAnswerAction, Gen.CahVoteForAnswerAction>>
export type DrawMatch = Assert<Equals<CahDrawCardsAction, Gen.CahDrawCardsAction>>
export type EndRoundMatch = Assert<Equals<EndRoundAction, Gen.EndRoundAction>>
export type StatisticsMatch = Assert<Equals<GameStatistics, Gen.CahStatistics>>

// `phase` is a numeric TypeScript enum on the front and `Int` on the Kotlin side; compare the rest
// structurally and assert the generated representation explicitly.
export type PublicStateMatch = Assert<
    Equals<Omit<CahGamePublicState, 'phase'>, Omit<Gen.CahGamePublicState, 'phase'>>
>
export type PhaseIsNumber = Assert<Equals<Gen.CahGamePublicState['phase'], number>>
