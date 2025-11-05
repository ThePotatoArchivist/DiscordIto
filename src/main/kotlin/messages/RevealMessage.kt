package archives.tater.discordito.messages

import archives.tater.discordito.*
import archives.tater.discordito.DynamicMessage.Companion.invalid
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
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
            description = data.entries.joinToString("\n") { entry ->
                val (team, word) = entry
                "${COLORS[data.teams.indexOf(team)]} `${if (entry in data.revealed) team.number else "?"}` $word"
            }
        }

        if (data.complete) embed {
            title = if (data.entries.map { it.team.number }.isSorted()) "You won!" else "You lost!"
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
                            emoji = DiscordPartialEmoji(name = COLORS[data.teams.indexOf(team)])
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
                for (index in interaction.values)
                    game.revealed.add(game.entries[index.toInt()])
                interaction.deferPublicMessageUpdate()
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