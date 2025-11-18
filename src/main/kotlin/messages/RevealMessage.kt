package archives.tater.discordito.messages

import archives.tater.discordito.*
import archives.tater.discordito.DynamicMessage.Companion.invalid
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.ComponentInteractionCreateEvent
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.rest.builder.component.option
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.delay

object RevealMessage : DynamicMessage<Game?> {
    override fun MessageBuilder.init(
        data: Game?,
        disable: Boolean
    ) {
        if (data !is Game.Reveal) return invalid()

        embed {
            title = data.question
            color = MAIN_EMBED_COLOR
            description = data.entries.joinToString("\n") { entry ->
                val (team, word) = entry
                "${team.colorEmoji} `${if (entry in data.revealed) team.number else "?"}` **$word** (${team.memberNames})"
            }
        }

        if (data.complete) embed {
            if (data.entries.map { it.team.number }.isSorted()) {
                title = "You won!"
                color = Color(0x33CD5B)
            } else {
                title = "You lost!"
                color = Color(0xCD3354)
            }
        }

        actionRow {
            if (data.complete) {
                interactionButton(ButtonStyle.Secondary, "replay") {
                    label = "Play Again"
                    disabled = disable
                }
                interactionButton(ButtonStyle.Secondary, "quit") {
                    label = "Quit"
                    disabled = disable
                }
            } else {
                stringSelect("reveal-entry") {
                    placeholder = "Reveal"
                    disabled = disable || data.entries.size == data.revealed.size
                    allowedValues = 0..data.entries.size

                    data.entries.forEachIndexed { index, entry ->
                        val (team, word) = entry
                        option(word, index.toString()) {
                            emoji = DiscordPartialEmoji(name = team.colorEmoji)
                        }
                    }
                }
            }
        }
    }

    override suspend fun ComponentInteractionCreateEvent.onComponent(data: Ref<Game?>) {
        val game = data.value as? Game.Reveal ?: return

        when (interaction.componentId) {
            "reveal-entry" -> {
                this as SelectMenuInteractionCreateEvent
                val revealed = interaction.values.map { game.entries[it.toInt()] }
                game.revealed.addAll(revealed)
                interaction.respondPublic {
                    content = revealed.joinToString("\n") { (team, word) -> "${team.colorEmoji} `${team.number}` **$word** (${team.memberNames})" }
                }
                update(data)
                if (game.entries.size == game.revealed.size) {
                    delay(5000)
                    game.complete = true
                    update(data)
                }
            }
            "replay" -> {
                update(data, disable = true)
                data.value = game.toStarting()
                StartMessage.send(data)
            }
            "quit" -> {
                interaction.deferPublicMessageUpdate()
                update(data, disable = true)
                data.value = null
            }
        }
    }
}