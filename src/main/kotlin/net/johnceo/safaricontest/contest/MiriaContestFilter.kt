package net.johnceo.safaricontest.contest

import net.minecraft.world.scores.PlayerScoreEntry

/** Select only the two detail rows in Miria's scoreboard section. */
object MiriaContestFilter {
    private val miria = Regex("^miria['’]s contest(?=\\s|:|$)", RegexOption.IGNORE_CASE)
    private val contest = Regex("\\bcontest\\b", RegexOption.IGNORE_CASE)
    private val formatting = Regex("§.")
    fun sortScores(entries: Collection<PlayerScoreEntry>) = entries.sortedWith(
        compareByDescending<PlayerScoreEntry> { it.value() }.thenComparator { a, b -> String.CASE_INSENSITIVE_ORDER.compare(a.owner(), b.owner()) })
    fun miriaLines(lines: List<String>): List<String> {
        val clean = lines.map { it.replace(formatting, "").trim() }
        val header = clean.indexOfFirst(miria::containsMatchIn)
        return if (header < 0) emptyList() else clean.drop(header + 1).take(2).takeWhile { !contest.containsMatchIn(it) }
    }
}
