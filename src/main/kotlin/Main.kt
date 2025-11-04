@file:JvmName("Main")

package archives.tater.discordito

import archives.tater.discordito.messages.PlayMessage
import archives.tater.discordito.messages.RevealMessage
import archives.tater.discordito.messages.StartMessage
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import io.github.cdimascio.dotenv.Dotenv

val DATA = mutableMapOf<Snowflake, Game>()

suspend fun main() {
    val dotenv = Dotenv.load()

    with (Kord(dotenv["BOT_TOKEN"])) {
        // /start <question>
        // -> Interface
        //    - Create Team
        //    - Join Team
        //    - Leave
        //    - Start Game

        val startGame = createGlobalChatInputCommand("start", "Start a game of Ito") {
            dmPermission = false
        }

        on<ChatInputCommandInteractionCreateEvent> {
            if (interaction.command.rootId == startGame.id) {
                if (interaction.channel.id in DATA) {
                    interaction.respondEphemeral {
                        content = "A game is already running!"
                    }
                    return@on
                }
                DATA[interaction.channel.id] = Game.Starting()
                StartMessage.send(DATA.getRef(interaction.channel.id))
            }
        }

        val getData: (InteractionCreateEvent) -> Ref<Game?> = { DATA.getRef(it.interaction.channel.id) }
        StartMessage.register(getData)
        PlayMessage.register(getData)
        RevealMessage.register(getData)

        login {
            intents += Intent.GuildMessages + Intent.Guilds
        }
    }
}