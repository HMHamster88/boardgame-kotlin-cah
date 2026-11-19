package boardgame.cah

import kotlinx.serialization.Serializable
import boardgame.api.codegen.GenerateTs

const val CAH_TYPE = "CARDS_AGAINST_HUMANITY"
const val CARDS_PER_PLAYER = 10

object CahPhase {
    const val CHOOSE_ANSWERS = 0
    const val VOTING = 1
    const val ROUND_RESULTS = 2
}

@GenerateTs
@Serializable
data class CahGameSettings(
    val id: String = "",
    val pointsToWin: Int = 10,
    val voteMode: Boolean = false
)

@GenerateTs
@Serializable
data class CahPublicPlayerState(
    val playerId: String,
    var points: Int = 0
)

@GenerateTs
@Serializable
data class PlayerAnswers(
    val playerId: String,
    val answersIds: MutableList<Int> = mutableListOf(),
    val playerVotes: MutableList<String> = mutableListOf()
)

@GenerateTs
@Serializable
data class CahStatistics(
    var turnCount: Int = 0
)

@GenerateTs(name = "CahGamePublicState")
@Serializable
data class CahPublicGameState(
    var phase: Int = CahPhase.CHOOSE_ANSWERS,
    var questionCardId: Int = 0,
    var playersSlectedAswers: MutableList<PlayerAnswers> = mutableListOf(),
    var activePlayerIndex: Int = 0,
    var playersStates: MutableList<CahPublicPlayerState> = mutableListOf(),
    var winnersIds: MutableList<String> = mutableListOf(),
    var statistics: CahStatistics = CahStatistics()
)

@GenerateTs
@Serializable
data class CahPrivatePlayerState(
    val playerId: String,
    val onHandAswersIds: MutableList<Int> = mutableListOf()
)

@GenerateTs(name = "CahGamePrivateState")
@Serializable
data class CahPrivateGameState(
    var playersStates: MutableList<CahPrivatePlayerState> = mutableListOf(),
    var questionDeck: MutableList<Int> = mutableListOf(),
    var answerDeck: MutableList<Int> = mutableListOf(),
    var questionDiscardPile: MutableList<Int> = mutableListOf(),
    var answerDiscardPile: MutableList<Int> = mutableListOf()
)

@GenerateTs(name = "CahSendAnswersAction", typeLiteral = "CahSendAnswersAction")
@Serializable
data class SendAnswersAction(
    val type: String = "CahSendAnswersAction",
    val answersIds: List<Int> = emptyList()
)

@GenerateTs(name = "CahVoteForAnswerAction", typeLiteral = "CahVoteForAnswerAction")
@Serializable
data class VoteForAnswerAction(
    val type: String = "CahVoteForAnswerAction",
    val playerId: String = ""
)

@GenerateTs(name = "CahDrawCardsAction", typeLiteral = "CahDrawCardsAction")
@Serializable
data class DrawCardsAction(
    val type: String = "CahDrawCardsAction"
)

@GenerateTs(name = "EndRoundAction", typeLiteral = "EndRoundAction")
@Serializable
data class EndRoundAction(
    val type: String = "EndRoundAction"
)
