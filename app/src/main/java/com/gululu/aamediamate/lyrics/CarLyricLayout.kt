package com.gululu.aamediamate.lyrics

import java.text.BreakIterator
import java.util.Locale

/** Fits lyric pages to the two text rows in the car's compact media preview. */
internal object CarLyricLayout {
    // The tested 800 px car display fits about 12 CJK characters per text row.
    private const val ROW_COLUMNS = 24

    fun pages(text: String): List<String> = rows(text).chunked(2).map { it.joinToString("\n") }

    /** Page boundaries use track time, so seeking/resyncing does not restart the first page. */
    fun timeline(lyrics: List<LyricLine>, durationMs: Long, playbackSpeed: Float = 1f): List<LyricLine> =
        lyrics.flatMapIndexed { index, line ->
            val pages = pages(line.text)
            if (pages.size <= 1) return@flatMapIndexed listOf(line.copy(text = pages.firstOrNull().orEmpty()))
            val start = line.timeSec.toDouble()
            val end = lyrics.getOrNull(index + 1)?.timeSec?.toDouble()
                ?: (durationMs / 1000.0).takeIf { it > start }
                ?: (start + pages.size * 2.0)
            val span = (end - start).coerceAtLeast(0.0)
            // Keep the first page still for up to two real seconds, then distribute the rest
            // within this lyric's interval. The next lyric always retains its original time.
            val firstHold = minOf(2.0 * playbackSpeed.coerceAtLeast(0.1f), span / pages.size)
            pages.mapIndexed { pageIndex, page ->
                val offset = if (pageIndex == 0) 0.0 else
                    firstHold + (span - firstHold) * (pageIndex - 1) / (pages.size - 1)
                LyricLine((start + offset).toFloat(), page)
            }
        }

    internal fun rows(text: String): List<String> {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        if (clean.isEmpty()) return listOf("")
        val chars = graphemes(clean)
        val result = mutableListOf<String>()
        var start = 0
        while (start < chars.size) {
            var end = start
            var columns = 0
            var breakAt = -1
            while (end < chars.size && columns + width(chars[end]) <= ROW_COLUMNS) {
                columns += width(chars[end])
                end++
                if (columns >= ROW_COLUMNS / 2 && chars[end - 1].last() in " ,.!?;:，。！？；：、") breakAt = end
            }
            if (end < chars.size && breakAt > start) end = breakAt
            if (end == start) end++
            result += chars.subList(start, end).joinToString("").trim()
            start = end
            while (start < chars.size && chars[start].isBlank()) start++
        }
        return result
    }

    internal fun columns(text: String): Int = graphemes(text).sumOf(::width)

    private fun width(text: String): Int = if (text.codePoints().anyMatch {
        it >= 0x2e80 || it == 'W'.code || it == 'M'.code || it == 'w'.code || it == 'm'.code
    }) 2 else 1

    private fun graphemes(text: String): List<String> {
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(text) }
        val result = mutableListOf<String>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val part = text.substring(start, end)
            val first = part.codePointAt(0)
            val joinsPrevious = first == 0x200d || first in 0xfe00..0xfe0f || first in 0x1f3fb..0x1f3ff ||
                Character.getType(first) in listOf(Character.NON_SPACING_MARK.toInt(), Character.COMBINING_SPACING_MARK.toInt()) ||
                result.lastOrNull()?.endsWith("\u200d") == true
            if (joinsPrevious && result.isNotEmpty()) result[result.lastIndex] += part else result += part
            start = end
            end = iterator.next()
        }
        return result
    }
}
