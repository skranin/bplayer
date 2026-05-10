package com.bplayer.data

import java.text.Collator
import java.util.Locale

private val ruCollator: Collator = Collator.getInstance(Locale("ru")).apply {
    strength = Collator.SECONDARY
}

private val numericChunk = Regex("\\d+|\\D+")

/**
 * Compares strings using Russian collation, with embedded numeric segments compared as ints.
 * "01.mp3" < "10.mp3" < "Глава 2" < "Глава 10" — and Cyrillic sorts in alphabetic order
 * (not Unicode code-point order, which is Voice's pain point).
 */
val NaturalRuComparator: Comparator<String> = Comparator { a, b ->
    val ax = numericChunk.findAll(a).iterator()
    val bx = numericChunk.findAll(b).iterator()
    while (ax.hasNext() && bx.hasNext()) {
        val sa = ax.next().value
        val sb = bx.next().value
        val aNum = sa.firstOrNull()?.isDigit() == true
        val bNum = sb.firstOrNull()?.isDigit() == true
        val cmp = when {
            aNum && bNum -> {
                val la = sa.toLongOrNull() ?: Long.MAX_VALUE
                val lb = sb.toLongOrNull() ?: Long.MAX_VALUE
                la.compareTo(lb).let { if (it != 0) it else sa.length.compareTo(sb.length) }
            }
            else -> ruCollator.compare(sa, sb)
        }
        if (cmp != 0) return@Comparator cmp
    }
    when {
        ax.hasNext() -> 1
        bx.hasNext() -> -1
        else -> 0
    }
}
