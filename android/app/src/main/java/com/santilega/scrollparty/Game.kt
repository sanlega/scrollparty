package com.santilega.scrollparty

import kotlin.random.Random

data class Group(val code: String, val adminId: String, val status: String, val roundIds: List<String>, val roundIndex: Int)
data class Member(val id: String, val name: String, val color: Int, val photo: String?, val count: Int, val joinedAt: Long)
data class Link(val id: String, val url: String, val clue: String = "")
data class Round(
    val id: String,
    val url: String,
    val authorId: String?,
    val votes: Map<String, String>,
    val reactions: Map<String, String> = emptyMap(),
    val clue: String = "",
)
data class Standing(val memberId: String, val correct: Int, val fooled: Int) {
    val score get() = correct + fooled
}

// Sin I, O, 0 ni 1 para que el código se pueda dictar en voz alta.
private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
fun newCode(random: Random = Random.Default) = (1..6).map { CODE_ALPHABET.random(random) }.joinToString("")

private val LINK = Regex("""https://(([a-z]+\.)?(instagram|tiktok|youtube)\.com|youtu\.be)/[^\s]+""")

/** Saca el enlace de un texto pegado o compartido, que a menudo trae más palabras. Debe coincidir con firestore.rules. */
fun extractLink(text: String): String? = LINK.find(text)?.value?.takeIf { it.length <= 500 }

/** +1 por acertar el autor; +1 al autor por cada amigo que falla. */
fun standings(rounds: List<Round>, memberIds: List<String>): List<Standing> {
    val correct = memberIds.associateWith { 0 }.toMutableMap()
    val fooled = memberIds.associateWith { 0 }.toMutableMap()
    for (round in rounds) {
        val author = round.authorId ?: continue
        for ((voter, guess) in round.votes) {
            if (voter == author) continue
            if (guess == author) correct.merge(voter, 1, Int::plus) else fooled.merge(author, 1, Int::plus)
        }
    }
    return memberIds.map { Standing(it, correct.getValue(it), fooled.getValue(it)) }.sortedByDescending { it.score }
}

/** Los premiados: todos los empatados en el máximo; nadie si el máximo es 0. */
fun winners(standings: List<Standing>, value: (Standing) -> Int): List<String> {
    val best = standings.maxOfOrNull(value) ?: 0
    return if (best == 0) emptyList() else standings.filter { value(it) == best }.map { it.memberId }
}

const val PLAYER_COLORS = 12
const val MAX_CLUE = 80
val REACTIONS = listOf("laugh", "skull", "fire", "sleep")

/** Color de jugador (0-11) libre en el grupo; si el grupo tiene más de 12, se repite alguno. */
fun freeColor(taken: Collection<Int>, random: Random = Random.Default): Int {
    val free = (0 until PLAYER_COLORS).filter { it !in taken }
    return free.ifEmpty { (0 until PLAYER_COLORS).toList() }.random(random)
}

/** Reacciones nuevas o cambiadas entre dos instantáneas de la ronda: son las que la tele lanza. */
fun reactionChanges(old: Map<String, String>, new: Map<String, String>): List<String> =
    new.filter { (uid, reaction) -> old[uid] != reaction }.values.toList()

/** Autor de la ronda revelada con más reacciones laugh+skull; el primero si empatan; nadie si ninguna tiene. */
fun reelOfTheNight(rounds: List<Round>): String? {
    fun laughs(r: Round) = r.reactions.values.count { it == "laugh" || it == "skull" }
    val best = rounds.filter { it.authorId != null }.maxByOrNull { laughs(it) } ?: return null
    return if (laughs(best) == 0) null else best.authorId
}

/** Hasta dos iniciales del nombre, para las caras sin foto. */
fun initials(name: String): String =
    name.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }
