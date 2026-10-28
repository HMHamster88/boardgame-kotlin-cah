package boardgame.cah

import boardgame.api.BotGameContext
import boardgame.api.Game
import boardgame.api.GameContext
import boardgame.api.GameStatePayload
import boardgame.api.GameStatus
import boardgame.api.Player
import boardgame.api.decodeTree
import boardgame.api.encodeTree
import boardgame.api.nowIso
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CahServiceTest {

    private val service = CahGameBackService()

    private fun game(vararg players: Player) = Game(
        id = "game-1",
        owner = players.first().userId,
        name = "test",
        type = CAH_TYPE,
        players = players.toMutableList(),
        status = GameStatus.CREATED,
        created = nowIso()
    )

    private fun player(id: String, bot: Boolean = false) =
        Player(userId = id, name = id, color = "#ff0000", online = true, isBot = bot)

    private class TestContext(
        override val game: Game,
        override val settings: JsonObject,
        initial: GameStatePayload
    ) : GameContext {
        private var publicValue: JsonElement = initial.publicState
        private var privateValue: JsonElement? = initial.privateState
        val notifications = mutableListOf<Triple<String?, String, JsonElement?>>()

        override val publicState: JsonElement get() = publicValue
        override val privateState: JsonElement? get() = privateValue

        override fun updatePublicState(value: JsonElement) {
            publicValue = value
        }

        override fun updatePrivateState(value: JsonElement?) {
            privateValue = value
        }

        override fun notify(peerId: String?, message: String, params: JsonElement?) {
            notifications += Triple(peerId, message, params)
        }

        fun payload() = GameStatePayload(game.id, publicValue, privateValue)
    }

    private class TestBotContext(
        override val game: Game,
        override val settings: JsonObject,
        override val publicState: JsonElement,
        override val privateState: JsonElement?
    ) : BotGameContext

    @Test
    fun `start deals hands and creates decks`() {
        val game = game(player("alice"), player("bob"))
        val state = service.startGame(game, service.getDefaultSettings())

        val public = decodeTree<CahPublicGameState>(state.publicState)
        val private = decodeTree<CahPrivateGameState>(assertNotNull(state.privateState))

        assertEquals(GameStatus.STARTED, game.status)
        assertEquals(2, public.playersStates.size)
        assertEquals(CahPhase.CHOOSE_ANSWERS, public.phase)
        assertEquals(0, public.activePlayerIndex)
        assertEquals(2, private.playersStates.size)
        private.playersStates.forEach { assertEquals(CARDS_PER_PLAYER, it.onHandAswersIds.size) }
        assertEquals(88, private.questionDeck.size + 1)
    }

    @Test
    fun `full round awards a point and resets`() = runBlocking {
        val game = game(player("alice"), player("bob"))
        var state = service.startGame(game, service.getDefaultSettings())

        val bobHand = decodeTree<CahPrivateGameState>(assertNotNull(state.privateState))
            .playersStates.first { it.playerId == "bob" }.onHandAswersIds
        assertTrue(bobHand.isNotEmpty())

        // bob submits, alice (active player) votes
        val answerContext = TestContext(game, service.getDefaultSettings(), state)
        service.performAction(answerContext, encodeTree(SendAnswersAction(answersIds = listOf(bobHand.first()))), "bob")
        state = answerContext.payload()
        assertEquals(CahPhase.VOTING, decodeTree<CahPublicGameState>(state.publicState).phase)
        assertTrue(answerContext.notifications.any { it.second == "selectCard" && it.first == "alice" })

        val voteContext = TestContext(game, service.getDefaultSettings(), state)
        service.performAction(voteContext, encodeTree(VoteForAnswerAction(playerId = "bob")), "alice")
        state = voteContext.payload()

        val publicAfterVote = decodeTree<CahPublicGameState>(state.publicState)
        assertEquals(CahPhase.ROUND_RESULTS, publicAfterVote.phase)
        assertEquals(1, publicAfterVote.playersStates.first { it.playerId == "bob" }.points)
        assertTrue(voteContext.notifications.any { it.second == "playersGotPoints" })

        // bob spends the point to redraw
        val drawContext = TestContext(game, service.getDefaultSettings(), state)
        service.performAction(drawContext, encodeTree(DrawCardsAction()), "bob")
        state = drawContext.payload()
        val publicAfterDraw = decodeTree<CahPublicGameState>(state.publicState)
        assertEquals(0, publicAfterDraw.playersStates.first { it.playerId == "bob" }.points)
        assertEquals(
            CARDS_PER_PLAYER,
            decodeTree<CahPrivateGameState>(assertNotNull(state.privateState))
                .playersStates.first { it.playerId == "bob" }.onHandAswersIds.size
        )

        // next round
        val endContext = TestContext(game, service.getDefaultSettings(), state)
        service.performAction(endContext, encodeTree(EndRoundAction()), "alice")
        state = endContext.payload()
        val publicAfterEnd = decodeTree<CahPublicGameState>(state.publicState)
        assertEquals(CahPhase.CHOOSE_ANSWERS, publicAfterEnd.phase)
        assertEquals(1, publicAfterEnd.activePlayerIndex)
        assertTrue(publicAfterEnd.playersSlectedAswers.isEmpty())
    }

    @Test
    fun `bots submit answers and vote`() = runBlocking {
        val game = game(player("alice"), player("bob", bot = true))
        val state = service.startGame(game, service.getDefaultSettings())
        val settings = service.getDefaultSettings()

        val action = service.nextBotAction(
            TestBotContext(game, settings, state.publicState, state.privateState)
        )
        assertNotNull(action, "bot should want to submit an answer")
        assertEquals("bob", action.playerId)
        assertEquals("CahSendAnswersAction", action.action.str("type"))
    }

    private fun JsonElement.str(key: String): String? =
        (this as? JsonObject)?.get(key)?.let { (it as kotlinx.serialization.json.JsonPrimitive).content }
}
