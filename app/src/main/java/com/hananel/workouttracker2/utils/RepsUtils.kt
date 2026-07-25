package com.hananel.workouttracker2.utils

/**
 * Turns a planned reps string into the number to pre-fill when logging a set.
 *
 * Plans are written for humans ("10-12", "8", "12 לכל צד"), so the rule is: take the last number in
 * the string — the top of a range is the target you're aiming for. Anything with no digits at all
 * ("מקסימום", "AMRAP") returns 0, which the UI reads as "not tracked" rather than "zero reps".
 */
object RepsUtils {

    private val NUMBER = Regex("""\d+""")

    fun targetReps(planned: String): Int =
        NUMBER.findAll(planned).lastOrNull()?.value?.toIntOrNull()?.coerceIn(0, 999) ?: 0

    /** "12 · 12 · 10 · 8" — the per-set breakdown shown in history. */
    fun formatSetReps(repsPerSet: List<Int>): String =
        repsPerSet.filter { it > 0 }.joinToString(" · ")
}
