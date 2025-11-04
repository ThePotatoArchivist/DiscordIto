package archives.tater.discordito.messages

import archives.tater.discordito.COLORS
import archives.tater.discordito.DynamicMessage
import archives.tater.discordito.DynamicMessage.Companion.invalid
import archives.tater.discordito.DynamicMessage.Companion.member
import archives.tater.discordito.Game
import archives.tater.discordito.Ref
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.event.interaction.ComponentInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.rest.builder.component.option
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed

object StartMessage : DynamicMessage<Game?> {
    override fun MessageBuilder.init(data: Game?, disable: Boolean) {
        if (data !is Game.Starting) return invalid()

        embed {
            title = "Ito"
            description = data.teams.withIndex().joinToString("\n") { (index, team) -> "${COLORS[index]} ${team.joinToString(" ") { it.mention }}" }
            field("Question") { data.question ?: "Unset" }
        }

        actionRow {
            interactionButton(ButtonStyle.Primary, "create-team") {
                label = "Create Team"
                disabled = disable || data.teams.size >= COLORS.size
            }
            interactionButton(ButtonStyle.Secondary, "leave") {
                label = "Leave"
                disabled = disable
            }
        }
        actionRow {
            stringSelect("join-team") {
                if (data.teams.isEmpty())
                    option("N/A", "na")
                else
                    data.teams.forEachIndexed { index, team ->
                        option(team.joinToString { it.effectiveName }, index.toString()) {
                            emoji = DiscordPartialEmoji(name = COLORS[index])
                        }
                    }
                placeholder = "Join Team"
                disabled = disable || data.teams.isEmpty()
            }
        }
        actionRow {
            interactionButton(if (data.question == null) ButtonStyle.Primary else ButtonStyle.Secondary, "set-question") {
                label = "Set Question"
                disabled = disable
            }
            interactionButton(ButtonStyle.Success, "start") {
                label = "Start"
                disabled = disable || data.question == null || data.teams.size < 2
            }
            interactionButton(ButtonStyle.Danger, "cancel") {
                label = "Cancel"
                disabled = disable
            }
        }
    }

    override suspend fun ComponentInteractionCreateEvent.onComponent(data: Ref<Game?>) {
        val game = data.value as? Game.Starting ?: return

        val member = member() ?: return

        when (interaction.componentId) {
            "create-team" -> {
                game.removeMember(member)
                game.teams.add(mutableListOf(member))
            }
            "join-team" -> {
                this as SelectMenuInteractionCreateEvent
                val team = game.teams[interaction.values.first().toInt()]
                game.removeMember(member, prune = false)
                team.add(member)
                game.removeEmpty()
            }
            "leave" -> game.removeMember(member)
            "set-question" -> {
                interaction.modal("Set Question", "set-question") {
                    actionRow {
                        textInput(TextInputStyle.Short, "question", "Question") {
                            required = true
                            value = game.question
                        }
                    }
                    actionRow {
                        textInput(TextInputStyle.Short, "1", "1") {
                            placeholder = "Least"
                            required = false
                            value = game.oneMeaning
                        }
                    }
                    actionRow {
                        textInput(TextInputStyle.Short, "100", "100") {
                            placeholder = "Most"
                            required = false
                            value = game.hundredMeaning
                        }
                    }
                }
                return
            }
            "start" -> {
                update(data, disable = true)
                data.value = game.toRunning()
                PlayMessage.send(data)
                return
            }
            "cancel" -> {
                interaction.message.delete()
                data.value = null
                return
            }
            else -> return
        }

        interaction.deferPublicMessageUpdate();
        update(data)
    }

    override suspend fun ModalSubmitInteractionCreateEvent.onModal(data: Ref<Game?>) {
        if (interaction.modalId != "set-question") return
        val game = data.value as? Game.Starting ?: return
        game.question = interaction.textInputs["question"]!!.value!!
        game.oneMeaning = interaction.textInputs["1"]!!.value.let { if (it == null || it.isEmpty()) "Least" else it }
        game.hundredMeaning = interaction.textInputs["100"]!!.value.let { if (it == null || it.isEmpty()) "Most" else it }
        interaction.deferPublicMessageUpdate()
        update(data)
    }
}