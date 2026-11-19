// Generated from Kotlin models by `gradle :back:generateTs`. Do not edit.
// The hand-written types in ../types.ts are validated against this file.

export interface CahGameSettings {
    id: string
    pointsToWin: number
    voteMode: boolean
}

export interface CahPublicPlayerState {
    playerId: string
    points: number
}

export interface PlayerAnswers {
    playerId: string
    answersIds: number[]
    playerVotes: string[]
}

export interface CahStatistics {
    turnCount: number
}

export interface CahGamePublicState {
    phase: number
    questionCardId: number
    playersSlectedAswers: PlayerAnswers[]
    activePlayerIndex: number
    playersStates: CahPublicPlayerState[]
    winnersIds: string[]
    statistics: CahStatistics
}

export interface CahPrivatePlayerState {
    playerId: string
    onHandAswersIds: number[]
}

export interface CahGamePrivateState {
    playersStates: CahPrivatePlayerState[]
    questionDeck: number[]
    answerDeck: number[]
    questionDiscardPile: number[]
    answerDiscardPile: number[]
}

export interface CahSendAnswersAction {
    type: 'CahSendAnswersAction'
    answersIds: number[]
}

export interface CahVoteForAnswerAction {
    type: 'CahVoteForAnswerAction'
    playerId: string
}

export interface CahDrawCardsAction {
    type: 'CahDrawCardsAction'
}

export interface EndRoundAction {
    type: 'EndRoundAction'
}

