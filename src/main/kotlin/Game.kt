package archives.tater.discordito

import dev.kord.core.entity.Member
import kotlin.random.Random

sealed interface Game {
    data class Team(val members: List<Member>, val number: Int)
    data class Entry(val team: Team, val word: String)

    data class Running(
        val question: String,
        val oneMeaning: String,
        val hundredMeaning: String,
        val teams: List<Team>,
        val entries: MutableList<Entry> = mutableListOf(),
        var selected: Int? = null,
    ) : Game {
        fun toReveal() = Reveal(
            question,
            teams,
            entries,
        )
    }

    data class Reveal(
        val question: String,
        val teams: List<Team>,
        val entries: List<Entry>,
        var revealed: MutableSet<Entry> = mutableSetOf(),
    ) : Game {
        fun toStarting() = Starting(
            teams = teams.map { it.members.toMutableList() }.toMutableList(),
        )
    }

    data class Starting(
        var question: String? = null,
        var oneMeaning: String? = null,
        var hundredMeaning: String? = null,
        val teams: MutableList<MutableList<Member>> = mutableListOf(),
    ): Game {
        fun toRunning() = Running(
            question ?: throw AssertionError("Question empty"),
            oneMeaning ?: throw AssertionError("1 meaning empty"),
            hundredMeaning ?: throw AssertionError("100 meaning empty"),
            teams.map { Team(it, Random.nextInt(1, 100)) },
        )

        fun removeMember(member: Member, prune: Boolean = true) {
            for (team in teams)
                team.remove(member)
            if (prune)
                removeEmpty()
        }

        fun removeEmpty() {
            teams.removeIf { it.isEmpty() }
        }
    }
}