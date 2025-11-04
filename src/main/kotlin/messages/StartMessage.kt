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

object StartMessage : DynamicMessage<Game?> {
    override fun MessageBuilder.init(data: Game?, disable: Boolean) {
        if (data !is Game.Starting) TODO()

        embed {
            title = "Ito"
            description = data.teams.withIndex().joinToString("\n") { (index, team) -> "${COLORS[index]} ${team.joinToString(" ") { it.mention }}" }
            field("Question") { data.question ?: "Unset" }
        }

        actionRow {
            interactionButton(ButtonStyle.Secondary, "create-team") {
                label = "Create Team"
                disabled = disable || data.teams.size >= COLORS.size
            }
            interactionButton(ButtonStyle.Danger, "leave") {
                label = "Leave"
                disabled = disable
            }
        }
        if (data.teams.isNotEmpty()) actionRow {
            stringSelect("join-team") {
                data.teams.forEachIndexed { index, team ->
                    option(team.joinToString { it.effectiveName }, index.toString()) {
                        emoji = DiscordPartialEmoji(name = COLORS[index])
                    }
                }
                placeholder = "Join Team"
                disabled = disable
            }
        }
        actionRow {
            interactionButton(if (data.question == null) ButtonStyle.Primary else ButtonStyle.Secondary, "set-question") {
                label = "Set Question"
                disabled = disable
            }
            interactionButton(ButtonStyle.Primary, "start") {
                label = "Start"
                disabled = disable || data.question == null || data.teams.size < 2 // TODO 2
            }
            interactionButton(ButtonStyle.Danger, "cancel") {
                label = "Cancel"
                disabled = disable
            }
        }
    }

    override suspend fun ComponentInteractionCreateEvent.onComponent(data: Ref<Game?>) {

        val member = interaction.user as? Member ?: run {
            interaction.respondEphemeral {
                content = "Must be a user"
            }
            return
        }

        when (interaction.componentId) {
            "create-team" -> {
                val game = data.value as? Game.Starting ?: return
                game.removeMember(member)
                game.teams.add(mutableListOf(member))
            }
            "join-team" -> {
                val game = data.value as? Game.Starting ?: return
                if (this !is SelectMenuInteractionCreateEvent) TODO()
                val team = game.teams[interaction.values.first().toInt()]
                game.removeMember(member, prune = false)
                team.add(member)
                game.removeEmpty()
            }
            "leave" -> {
                val game = data.value as? Game.Starting ?: return
                game.removeMember(member)
            }
            "set-question" -> {
                interaction.modal("Set Question", "set-question") {
                    actionRow {
                        textInput(TextInputStyle.Short, "question", "Question") {
                            required = true
                        }
                    }
                    actionRow {
                        textInput(TextInputStyle.Short, "1", "1") {
                            placeholder = "Least"
                            required = false
                        }
                    }
                    actionRow {
                        textInput(TextInputStyle.Short, "100", "100") {
                            placeholder = "Most"
                            required = false
                        }
                    }
                }
                return
            }
            "start" -> {
                val game = data.value as? Game.Starting ?: return
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
        val game = data.value as? Game.Starting ?: TODO()
        game.question = interaction.textInputs["question"]!!.value!!
        game.oneMeaning = interaction.textInputs["1"]!!.value.let { if (it == null || it.isEmpty()) "Least" else it }
        game.hundredMeaning = interaction.textInputs["100"]!!.value.let { if (it == null || it.isEmpty()) "Most" else it }
        interaction.deferPublicMessageUpdate()
        update(data)
    }
}