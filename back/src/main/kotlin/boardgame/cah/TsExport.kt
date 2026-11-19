package boardgame.cah

import boardgame.api.codegen.TsTypeSpec
import boardgame.api.codegen.generateTs
import java.io.File

/**
 * Entry point for `:back:generateTs`: writes the TypeScript model types used by the front.
 * Only the models shared with the back are generated; front-only types stay in ../types.ts.
 */
object TsExport {
    @JvmStatic
    fun main(args: Array<String>) {
        val output = File(args[0])
        val types = listOf(
            TsTypeSpec(CahGameSettings.serializer(), "CahGameSettings"),
            TsTypeSpec(CahPublicPlayerState.serializer(), "CahPublicPlayerState"),
            TsTypeSpec(PlayerAnswers.serializer(), "PlayerAnswers"),
            TsTypeSpec(CahStatistics.serializer(), "CahStatistics"),
            TsTypeSpec(CahPublicGameState.serializer(), "CahGamePublicState"),
            TsTypeSpec(CahPrivatePlayerState.serializer(), "CahPrivatePlayerState"),
            TsTypeSpec(CahPrivateGameState.serializer(), "CahGamePrivateState"),
            TsTypeSpec(SendAnswersAction.serializer(), "CahSendAnswersAction", "CahSendAnswersAction"),
            TsTypeSpec(VoteForAnswerAction.serializer(), "CahVoteForAnswerAction", "CahVoteForAnswerAction"),
            TsTypeSpec(DrawCardsAction.serializer(), "CahDrawCardsAction", "CahDrawCardsAction"),
            TsTypeSpec(EndRoundAction.serializer(), "EndRoundAction", "EndRoundAction")
        )
        output.parentFile?.mkdirs()
        output.writeText(generateTs(types))
        println("Generated ${output.absolutePath}")
    }
}
