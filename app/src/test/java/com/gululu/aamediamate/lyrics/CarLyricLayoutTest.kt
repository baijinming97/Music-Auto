package com.gululu.aamediamate.lyrics

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CarLyricLayoutTest {
    private val longText = "春夏秋冬山川湖海日月星辰".repeat(5)

    @Test fun `short lines remain unchanged`() {
        assertEquals(listOf("短句"), CarLyricLayout.pages("短句"))
        assertEquals(listOf(""), CarLyricLayout.pages(""))
    }

    @Test fun `long Chinese line uses two rows without losing characters`() {
        val text = "春夏秋冬山川湖海日月星辰一路同行"
        val pages = CarLyricLayout.pages(text)
        assertEquals(1, pages.size)
        assertEquals(2, pages.single().lines().size)
        assertEquals(text, pages.single().replace("\n", ""))
        assertTrue(pages.single().lines().all { CarLyricLayout.columns(it) <= 24 })
    }

    @Test fun `extra long text occupies all pages without truncation`() {
        val pages = CarLyricLayout.pages(longText)
        assertEquals(3, pages.size)
        assertTrue(pages.all { it.lines().size <= 2 })
        assertEquals(longText, pages.joinToString("").replace("\n", ""))
        assertTrue(pages.flatMap { it.lines() }.all { CarLyricLayout.columns(it) <= 24 })
    }

    @Test fun `English wraps at spaces and handles wide Latin letters`() {
        val text = "A gentle evening with warm memories waiting for tomorrow"
        val rows = CarLyricLayout.rows(text)
        assertTrue(rows.all { CarLyricLayout.columns(it) <= 24 })
        assertEquals(text, rows.joinToString(" "))
        assertTrue(CarLyricLayout.rows("W".repeat(30)).all { it.length <= 12 })
    }

    @Test fun `Unicode characters and joined emoji stay intact`() {
        val symbol = "👩‍🎤"
        val text = "山".repeat(11) + symbol + "e\u0301" + "河".repeat(15)
        val rows = CarLyricLayout.rows(text)
        assertEquals(text, rows.joinToString(""))
        assertTrue(rows.any { symbol in it })
        assertTrue(rows.none { it.startsWith("\u0301") || it.endsWith("\u200d") })
    }

    @Test fun `page timing gives initial hold and preserves next lyric timestamp`() {
        val lines = listOf(LyricLine(10f, longText), LyricLine(19f, "下一句"))
        val timeline = CarLyricLayout.timeline(lines, 30_000)
        assertEquals(listOf(10f, 12f, 15.5f, 19f), timeline.map { it.timeSec })
        assertEquals("下一句", timeline.last().text)
        assertEquals(longText, lines.first().text)
    }

    @Test fun `very short lyric interval never delays the following line`() {
        val timeline = CarLyricLayout.timeline(listOf(LyricLine(0f, longText), LyricLine(1f, "Next")), 30_000)
        assertTrue(timeline.dropLast(1).all { it.timeSec < 1f })
        assertEquals(1f, timeline.last().timeSec)
    }

    @Test fun `last lyric uses duration with a finite fallback for streams`() {
        val line = listOf(LyricLine(10f, longText))
        val timed = CarLyricLayout.timeline(line, 18_000)
        assertTrue(timed.all { it.timeSec in 10f..18f })
        val untimed = CarLyricLayout.timeline(line, 0)
        assertEquals(listOf(10f, 12f, 14f), untimed.map { it.timeSec })
    }

    @Test fun `sync holds first page then advances and seeking selects the right page`() = runTest {
        val timeline = CarLyricLayout.timeline(listOf(LyricLine(0f, longText), LyricLine(9f, "Next")), 20_000)
        val seen = mutableListOf<String>()
        val job = backgroundScope.launch {
            LyricSyncEngine.sync(timeline, 0, nowMs = { testScheduler.currentTime }) { text, _ -> seen += text }
        }
        runCurrent()
        advanceTimeBy(1999)
        runCurrent()
        assertEquals(listOf(timeline[0].text), seen)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(timeline[1].text, seen.last())
        job.cancel()
        val sought = mutableListOf<String>()
        backgroundScope.launch {
            LyricSyncEngine.sync(timeline, 6_000, nowMs = { testScheduler.currentTime }) { text, _ -> sought += text }
        }
        runCurrent()
        assertEquals(listOf(timeline[2].text), sought)
        advanceTimeBy(3_000)
        runCurrent()
        assertEquals("Next", sought.last())
    }
}
