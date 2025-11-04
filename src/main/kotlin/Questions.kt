package archives.tater.discordito

import java.io.File

private val QUESTIONS_FILE = File("questions.txt")
var QUESTIONS: List<String> = QUESTIONS_FILE.useLines { it.toList() }
    set(value) {
        field = value
        QUESTIONS_FILE.writeText(value.joinToString("", postfix = "\n"))
    }
