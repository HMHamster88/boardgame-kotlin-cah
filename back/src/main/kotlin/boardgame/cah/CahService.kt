package boardgame.cah

import boardgame.api.BotAction
import boardgame.api.BotGameContext
import boardgame.api.Game
import boardgame.api.GameBackModule
import boardgame.api.GameBackService
import boardgame.api.GameContext
import boardgame.api.GameStatePayload
import boardgame.api.GameStaticSettings
import boardgame.api.GameStatus
import boardgame.api.apiJson
import boardgame.api.decodeTree
import boardgame.api.encodeTree
import boardgame.api.int
import boardgame.api.privateStateAs
import boardgame.api.publicStateAs
import boardgame.api.randomElement
import boardgame.api.settingsAs
import boardgame.api.shuffledBy
import boardgame.api.str
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class CahGameModule : GameBackModule {
    override fun createService(): GameBackService = CahGameBackService()
}

class CahGameBackService : GameBackService {

    override val type: String = CAH_TYPE
    override val version: String = BuildInfo.version
    override val homepage: String = "https://github.com/HMHamster88/boardgame-kotlin-cah"
    override val gameStaticSettings: GameStaticSettings = GameStaticSettings(minPlayers = 2, maxPlayers = 6)

    override val localizedName: JsonObject = buildJsonObject {
        putJsonObject("en") { put(CAH_TYPE, "Cards Against Humanity") }
        putJsonObject("ru") { put(CAH_TYPE, "Карты против всех") }
    }

    override fun getDefaultSettings(): JsonObject =
        encodeTree(CahGameSettings()).jsonObject

    override fun startGame(game: Game, settings: JsonObject): GameStatePayload {
        val answerDeck = (0 until answersCount).toList().shuffledBy().toMutableList()
        val privateStates = game.players.map { player ->
            CahPrivatePlayerState(
                playerId = player.userId,
                onHandAswersIds = MutableList(CARDS_PER_PLAYER) { answerDeck.removeAt(0) }
            )
        }.toMutableList()

        val questionDeck = (0 until questions.size).toList().shuffledBy().toMutableList()
        val privateState = CahPrivateGameState(
            playersStates = privateStates,
            questionDeck = questionDeck,
            answerDeck = answerDeck,
            questionDiscardPile = mutableListOf(),
            answerDiscardPile = mutableListOf()
        )

        val publicState = CahPublicGameState(
            phase = CahPhase.CHOOSE_ANSWERS,
            questionCardId = privateState.questionDeck.removeAt(0),
            playersSlectedAswers = mutableListOf(),
            activePlayerIndex = 0,
            playersStates = game.players.map { CahPublicPlayerState(playerId = it.userId) }.toMutableList(),
            winnersIds = mutableListOf(),
            statistics = CahStatistics()
        )

        game.status = GameStatus.STARTED

        return GameStatePayload(
            id = game.id,
            publicState = encodeTree(publicState),
            privateState = encodeTree(privateState)
        )
    }

    override suspend fun performAction(ctx: GameContext, action: JsonElement, playerId: String): JsonElement? {
        val publicState = decodeTree<CahPublicGameState>(ctx.publicState)
        val privateState = ctx.privateState?.let { decodeTree<CahPrivateGameState>(it) } ?: return null
        val settings = decodeTree<CahGameSettings>(ctx.settings)
        val activePlayerId = ctx.game.players.getOrNull(publicState.activePlayerIndex)?.userId

        when ((action as? JsonObject)?.str("type")) {
            "CahSendAnswersAction" -> sendAnswers(ctx, publicState, privateState, settings, action, playerId, activePlayerId)
            "CahVoteForAnswerAction" -> vote(ctx, publicState, privateState, settings, action, playerId, activePlayerId)
            "CahDrawCardsAction" -> drawCards(publicState, privateState, playerId)
            "EndRoundAction" -> endRound(publicState, privateState, ctx.game)
            else -> return null
        }

        ctx.updatePublicState(encodeTree(publicState))
        ctx.updatePrivateState(encodeTree(privateState))
        return null
    }

    override suspend fun nextBotAction(ctx: BotGameContext): BotAction? {
        val publicState = ctx.publicStateAs<CahPublicGameState>()
        val privateState = ctx.privateStateAs<CahPrivateGameState>()
        val settings = ctx.settingsAs<CahGameSettings>()
        val activePlayerId = ctx.game.players.getOrNull(publicState.activePlayerIndex)?.userId

        for (bot in ctx.game.players.filter { it.isBot }) {
            val botState = privateState.playersStates.firstOrNull { it.playerId == bot.userId } ?: continue
            when (publicState.phase) {
                CahPhase.CHOOSE_ANSWERS -> {
                    if (publicState.playersSlectedAswers.any { it.playerId == bot.userId }) continue
                    if (!settings.voteMode && activePlayerId == bot.userId) continue
                    val question = questions.getOrNull(publicState.questionCardId) ?: continue
                    val ids = botState.onHandAswersIds.shuffledBy().take(countRequiredAnswers(question))
                    return BotAction(encodeTree(SendAnswersAction(answersIds = ids)), bot.userId)
                }

                CahPhase.VOTING -> {
                    if (publicState.playersSlectedAswers.any { bot.userId in it.playerVotes }) continue
                    if (!settings.voteMode && activePlayerId != bot.userId) continue
                    val candidates = publicState.playersSlectedAswers.filter { it.playerId != bot.userId }
                    val answer = candidates.randomElement() ?: continue
                    return BotAction(encodeTree(VoteForAnswerAction(playerId = answer.playerId)), bot.userId)
                }
            }
        }
        return null
    }

    // ---------------------------------------------------------------- actions

    private fun sendAnswers(
        ctx: GameContext,
        publicState: CahPublicGameState,
        privateState: CahPrivateGameState,
        settings: CahGameSettings,
        action: JsonElement,
        playerId: String,
        activePlayerId: String?
    ) {
        if (publicState.phase != CahPhase.CHOOSE_ANSWERS) return
        val playerState = privateState.playersStates.firstOrNull { it.playerId == playerId } ?: return
        val payload = decodeTree<SendAnswersAction>(action)

        playerState.onHandAswersIds.removeAll(payload.answersIds)
        publicState.playersSlectedAswers.add(
            PlayerAnswers(playerId = playerId, answersIds = payload.answersIds.toMutableList())
        )

        val required = if (settings.voteMode) ctx.game.players.size else ctx.game.players.size - 1
        if (publicState.playersSlectedAswers.size >= required) {
            publicState.playersSlectedAswers = publicState.playersSlectedAswers.shuffledBy().toMutableList()
            publicState.phase = CahPhase.VOTING
            ctx.notify(activePlayerId, "selectCard", null)
        }
    }

    private fun vote(
        ctx: GameContext,
        publicState: CahPublicGameState,
        privateState: CahPrivateGameState,
        settings: CahGameSettings,
        action: JsonElement,
        playerId: String,
        activePlayerId: String?
    ) {
        if (publicState.phase != CahPhase.VOTING) return
        if (playerId != activePlayerId && !settings.voteMode) return

        val payload = decodeTree<VoteForAnswerAction>(action)
        if (playerId == payload.playerId) return
        if (publicState.playersSlectedAswers.any { playerId in it.playerVotes }) return

        val answer = publicState.playersSlectedAswers.firstOrNull { it.playerId == payload.playerId } ?: return
        answer.playerVotes.add(playerId)

        val votes = publicState.playersSlectedAswers.sumOf { it.playerVotes.size }
        val required = if (settings.voteMode) ctx.game.players.size else 1
        if (votes < required) return

        publicState.playersSlectedAswers.forEach { playerAnswer ->
            val pointsState = publicState.playersStates.firstOrNull { it.playerId == playerAnswer.playerId }
            if (pointsState != null) {
                pointsState.points += playerAnswer.playerVotes.size
            }
        }

        val summary = publicState.playersSlectedAswers
            .filter { it.playerVotes.isNotEmpty() }
            .mapNotNull { playerAnswer ->
                val name = ctx.game.players.firstOrNull { it.userId == playerAnswer.playerId }?.name
                name?.let { "$it: ${playerAnswer.playerVotes.size}" }
            }
            .joinToString(", ")
        ctx.notify(null, "playersGotPoints", buildJsonObject { put("players", summary) })

        val maxPoints = publicState.playersStates.maxOfOrNull { it.points } ?: 0
        if (maxPoints >= settings.pointsToWin) {
            publicState.winnersIds = publicState.playersStates
                .filter { it.points >= settings.pointsToWin }
                .map { it.playerId }
                .toMutableList()
            ctx.game.status = GameStatus.FINISHED
        }

        publicState.playersSlectedAswers.forEach { playerAnswer ->
            privateState.answerDiscardPile.addAll(playerAnswer.answersIds)
        }
        privateState.playersStates.forEach { playerState ->
            pushFromDeck(
                deck = privateState.answerDeck,
                discardPile = privateState.answerDiscardPile,
                target = playerState.onHandAswersIds,
                count = CARDS_PER_PLAYER - playerState.onHandAswersIds.size
            )
        }

        privateState.questionDiscardPile.add(publicState.questionCardId)
        if (privateState.questionDeck.isEmpty()) {
            privateState.questionDeck = privateState.questionDiscardPile.shuffledBy().toMutableList()
        }
        publicState.phase = CahPhase.ROUND_RESULTS
    }

    private fun drawCards(publicState: CahPublicGameState, privateState: CahPrivateGameState, playerId: String) {
        val pointsState = publicState.playersStates.firstOrNull { it.playerId == playerId } ?: return
        if (pointsState.points <= 0) return
        val playerState = privateState.playersStates.firstOrNull { it.playerId == playerId } ?: return

        privateState.answerDiscardPile.addAll(playerState.onHandAswersIds)
        playerState.onHandAswersIds.clear()
        pushFromDeck(
            deck = privateState.answerDeck,
            discardPile = privateState.answerDiscardPile,
            target = playerState.onHandAswersIds,
            count = CARDS_PER_PLAYER
        )
        pointsState.points -= 1
    }

    private fun endRound(publicState: CahPublicGameState, privateState: CahPrivateGameState, game: Game) {
        if (privateState.questionDeck.isEmpty()) {
            privateState.questionDeck = privateState.questionDiscardPile.shuffledBy().toMutableList()
            privateState.questionDiscardPile.clear()
        }
        if (privateState.questionDeck.isNotEmpty()) {
            publicState.questionCardId = privateState.questionDeck.removeAt(0)
        }
        publicState.playersSlectedAswers.clear()
        publicState.phase = CahPhase.CHOOSE_ANSWERS
        if (game.players.isNotEmpty()) {
            publicState.activePlayerIndex = (publicState.activePlayerIndex + 1) % game.players.size
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun pushFromDeck(
        deck: MutableList<Int>,
        discardPile: MutableList<Int>,
        target: MutableList<Int>,
        count: Int
    ) {
        if (count <= 0) return
        if (deck.size < count) {
            val additional = count - deck.size
            target.addAll(deck)
            deck.addAll(discardPile.shuffledBy())
            val moved = deck.take(additional)
            target.addAll(moved)
            repeat(moved.size) { deck.removeAt(0) }
        } else {
            repeat(count) { target.add(deck.removeAt(0)) }
        }
    }

    private fun countRequiredAnswers(question: String): Int =
        Regex("\\{(.*?)\\}").findAll(question).count()

    private val questions: List<String> by lazy {
        val text = CahGameBackService::class.java.getResourceAsStream("/texts/questions.json")
            ?.readBytes()?.decodeToString()
            ?: error("texts/questions.json is missing")
        apiJson.decodeFromString<List<String>>(text)
    }

    private val answersCount: Int by lazy {
        val text = CahGameBackService::class.java.getResourceAsStream("/texts/meta.json")
            ?.readBytes()?.decodeToString()
            ?: error("texts/meta.json is missing")
        apiJson.parseToJsonElement(text).jsonObject.int("answersCount") ?: 0
    }
}
