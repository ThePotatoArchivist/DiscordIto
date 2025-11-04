package archives.tater.discordito

import dev.kord.core.Kord
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.Member
import dev.kord.core.event.interaction.ActionInteractionCreateEvent
import dev.kord.core.event.interaction.ComponentInteractionCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.MessageBuilder

interface DynamicMessage<T> {
    fun MessageBuilder.init(data: T, disable: Boolean)
    suspend fun ComponentInteractionCreateEvent.onComponent(data: Ref<T>)
    suspend fun ModalSubmitInteractionCreateEvent.onModal(data: Ref<T>) {}

    context(event: ActionInteractionCreateEvent)
    suspend fun send(data: Ref<T>, disable: Boolean = false) {
        event.interaction.respondPublic {
            init(data.get(), disable)
        }
    }

    context(event: ComponentInteractionCreateEvent)
    suspend fun update(data: Ref<T>, disable: Boolean = false) {
        event.interaction.message.edit {
            init(data.get(), disable)
        }
    }

    context(event: ModalSubmitInteractionCreateEvent)
    suspend fun update(data: Ref<T>, disable: Boolean = false) {
        event.interaction.message!!.edit {
            init(data.get(), disable)
        }
    }

    context(kord: Kord)
    fun register(getData: (InteractionCreateEvent) -> Ref<T>) {
        with (kord) {
            on<ComponentInteractionCreateEvent> {
                onComponent(getData(this))
            }
            on<ModalSubmitInteractionCreateEvent> {
                onModal(getData(this))
            }
        }
    }

    companion object {
        fun InteractionCreateEvent.member() = interaction.user as? Member

        fun MessageBuilder.invalid() {
            content = "Invalid State"
        }
    }
}