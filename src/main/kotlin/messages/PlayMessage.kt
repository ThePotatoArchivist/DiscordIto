package archives.tater.discordito.messages

import archives.tater.discordito.COLORS
import archives.tater.discordito.DynamicMessage
import archives.tater.discordito.Game
import archives.tater.discordito.Ref
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.Member
import dev.kord.core.event.interaction.ComponentInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.rest.builder.component.option
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed

object PlayMessage : DynamicMessage<Game?> {
    override fun MessageBuilder.init(
        data: Game?,
        disable: Boolean
    ) {
        if (data !is Game.Running) TODO()

        embed {
            title = data.question
            description = data.entries.joinToString("\n") { (team, word) ->
                "${COLORS[data.teams.indexOf(team)]} $word"
            }
            field("0", inline = true) { data.oneMeaning }
            field("100", inline = true) { data.hundredMeaning }
        }

        actionRow {
            interactionButton(ButtonStyle.Secondary, "see-number") {
                label = "See Number"
                disabled = disable
            }
            interactionButton(ButtonStyle.Secondary, "set-word") {
                label = "Set Word"
                disabled = disable
            }
        }
        if (data.entries.isNotEmpty()) {
            actionRow {
                stringSelect("selected-entry") {
                    placeholder = "Rearrange"
                    data.entries.forEachIndexed { index, (team, word) ->
                        option(word, index.toString()) {
                            emoji = DiscordPartialEmoji(name = COLORS[data.teams.indexOf(team)])
                            default = index == data.selected
                        }
                    }
                    disabled = disable
                }
            }
            actionRow {
                interactionButton(ButtonStyle.Secondary, "entry-up") {
                    emoji = DiscordPartialEmoji(name = "\u2b06\ufe0f")
                    disabled = disable || data.selected == null
                }
                interactionButton(ButtonStyle.Secondary, "entry-down") {
                    emoji = DiscordPartialEmoji(name = "\u2b07\ufe0f")
                    disabled = disable || data.selected == null
                }
            }
            actionRow {
                interactionButton(ButtonStyle.Primary, "reveal") {
                    label = "Reveal"
                    disabled = disable || data.entries.size < data.teams.size
                }
            }
        }
    }

    override suspend fun ComponentInteractionCreateEvent.onComponent(data: Ref<Game?>) {
        when (interaction.componentId) {
            "see-number" -> {
                val member = interaction.user as? Member ?: TODO()
                val game = data.value as? Game.Running ?: TODO()
                val team = game.teams.find { member in it.members } ?: run {
                    interaction.respondEphemeral {
                        content = "You are not in the game"
                    }
                    return
                }
                interaction.respondEphemeral {
                    content = "Your number is: `${team.number}`"
                }
                return
            }
            "set-word" -> {
                interaction.modal("Set Word", "set-word") {
                    actionRow {
                        textInput(TextInputStyle.Short, "word", "Word") {
                            required = true
                        }
                    }
                }
                return
            }
            "selected-entry" -> {
                val game = data.value as? Game.Running ?: TODO()
                if (this !is SelectMenuInteractionCreateEvent) TODO()
                game.selected = interaction.values.first().toInt()
            }
            "entry-up" -> {
                val game = data.value as? Game.Running ?: TODO()
                val selected = game.selected ?: TODO()
                if (selected < 1) {
                    interaction.deferPublicMessageUpdate()
                    return
                }
                val entry = game.entries[selected]
                game.entries.remove(entry)
                game.entries.add(selected - 1, entry)
                game.selected = selected - 1
            }
            "entry-down" -> {
                val game = data.value as? Game.Running ?: TODO()
                val selected = game.selected ?: TODO()
                if (selected >= game.entries.size - 1) {
                    interaction.deferPublicMessageUpdate()
                    return
                }
                val entry = game.entries[selected]
                game.entries.remove(entry)
                game.entries.add(selected + 1, entry)
                game.selected = selected + 1
            }
            "reveal" -> {
                val game = data.value as? Game.Running ?: TODO()
                update(data, disable = true)
                data.value = game.toReveal()
                RevealMessage.send(data)
                return
            }
            else -> return
        }
        interaction.deferPublicMessageUpdate()
        update(data)
    }

    override suspend fun ModalSubmitInteractionCreateEvent.onModal(data: Ref<Game?>) {
        if (interaction.modalId != "set-word") return
        val game = data.value as? Game.Running ?: TODO()

        val member = interaction.user as? Member ?: TODO()
        val team = game.teams.find { member in it.members } ?: run {
            interaction.respondEphemeral {
                content = "You are not in the game"
            }
            return
        }
        game.entries.removeIf { it.team == team }
        game.entries.add(Game.Entry(team, interaction.textInputs["word"]?.value!!))
        interaction.deferPublicMessageUpdate()
        update(data)
    }
}