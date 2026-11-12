package net.johnceo.safaricontest.contest

import net.johnceo.safaricontest.config.PartyMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ContestBehaviorTest {
    @Test fun `start fires on a new window anywhere in SkyBlock without any party role`() {
        val state = ContestStartState()
        assertFalse(state.poll(10, 1199, true, true))
        assertTrue(state.poll(11, 0, true, true))
        assertFalse(state.poll(11, 1, true, true))
        assertTrue(state.poll(12, 2, true, true))
    }
    @Test fun `joining late enabling and entering SkyBlock do not send stale requests`() {
        val state = ContestStartState()
        assertFalse(state.poll(10, 0, true, true))
        assertFalse(state.poll(11, 0, false, true))
        assertFalse(state.poll(11, 1, true, true))
        assertFalse(state.poll(12, 0, true, false))
        assertFalse(state.poll(12, 1, true, true))
        assertFalse(state.poll(13, 11, true, true))
    }
    @Test fun `ticket requires current qualifying evidence and successful send and deduplicates per target`() {
        val tickets = ContestTicketState()
        assertFalse(tickets.claim("me", 10, "Common", 30))
        assertFalse(tickets.claim("me", 10, "Uncommon", 9))
        assertFalse(tickets.claim("me", 10, "Uncommon", 10) { false })
        assertTrue(tickets.claim("me", 10, "Uncommon", 10))
        assertFalse(tickets.claim("ME", 10, "Legendary", 30))
        assertFalse(tickets.claim("me", 10, "Epic", 30, true, 4999))
        assertTrue(tickets.claim("me", 10, "Legendary", 30, true, 5000))
        assertFalse(tickets.claim("me", 10, "Legendary", 40, true, 5000))
        assertTrue(tickets.claim("me", 11, "Uncommon", 10))
    }
    @Test fun `message presets custom fallback and control characters are safe party bodies`() {
        val choices = listOf("r", "!m7", "!t5", "other")
        for (preset in choices.dropLast(1)) assertEquals(preset, PartyMessage.resolve(preset, "", choices, "r"))
        assertEquals("r", PartyMessage.resolve("other", " \n ", choices, "r"))
        assertEquals("!m6", PartyMessage.resolve("other", " !m6 ", choices, "r"))
        assertEquals("/warp hub", PartyMessage.sanitize("/warp hub\r\n"))
        assertEquals(240, PartyMessage.sanitize("a".repeat(300)).length)
        assertEquals("!dt contest", PartyMessage.resolve("other", "", listOf("!dt contest", "other"), "!dt contest"))
    }
    @Test fun `Miria filter excludes unrelated contest scores`() {
        assertEquals(listOf("Uncommon with 1,234", "10:00 remaining"), MiriaContestFilter.miriaLines(listOf(
            "Jacob's Contest", "Gold with 99999", "Miria's Contest", "Uncommon with 1,234", "10:00 remaining", "Rare with 9k")))
        assertTrue(MiriaContestFilter.miriaLines(listOf("Jacob's Contest", "Legendary with 5000")).isEmpty())
    }
    @Test fun `windows roll at 15 35 55 and remain unique across midnight and dates`() {
        val t = LocalDateTime.of(2026, 11, 12, 12, 15)
        val key = ContestTracker.absoluteWindow(t)
        assertEquals(key-1, ContestTracker.absoluteWindow(t.minusSeconds(1)))
        assertEquals(key, ContestTracker.absoluteWindow(t.plusMinutes(19)))
        assertEquals(key+1, ContestTracker.absoluteWindow(t.plusMinutes(20)))
        assertEquals(key+72, ContestTracker.absoluteWindow(t.plusDays(1)))
        assertEquals(ContestTracker.absoluteWindow(t.withHour(23).withMinute(55)),
            ContestTracker.absoluteWindow(t.plusDays(1).withHour(0).withMinute(0)))
    }
}
