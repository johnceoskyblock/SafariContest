package net.johnceo.safaricontest.party

import net.johnceo.safaricontest.party.TicketCommandGuard.Decision.*
import net.johnceo.safaricontest.contest.ContestTicketState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TicketCommandGuardTest {
    @Test fun `party downtime and short waits track each sender including self`() {
        val pending = linkedSetOf<String>()
        for ((sender,body) in listOf("Me" to "!dt contest", "Alex" to "!dt food", "Steve" to "1s", "Pat" to "sec")) {
            assertTrue(TicketCommandGuard.observe("§9Party §8> §b[MVP+] $sender§f: $body",pending))
        }
        assertEquals(setOf("me","alex","steve","pat"),pending)
        assertFalse(TicketCommandGuard.observe("Party > Alex: !dt food",pending))
        assertTrue(TicketCommandGuard.observe("Party > Alex: READY!",pending))
        assertEquals(setOf("me","steve","pat"),pending)
    }
    @Test fun `public guild private quoted and non-readiness chat cannot release another requester`() {
        val pending = linkedSetOf("alex")
        for (text in listOf("Guild > Alex: ready", "From Alex: r", "[MVP+] Alex: !dt", "Party > Steve: Alex is ready", "Party > Alex: not ready", "Party > Alex: ready?", "Party > Alex: !dtfoo")) {
            assertFalse(TicketCommandGuard.observe(text,pending),text)
        }
        assertEquals(setOf("alex"),pending)
        assertTrue(TicketCommandGuard.observe("Party > Alex: r",pending))
        assertTrue(pending.isEmpty())
    }
    @Test fun `members send r for commands while anyone including self is waiting`() {
        for (requests in listOf(setOf("me"),setOf("alex"),setOf("me","alex"))) {
            for (command in listOf("!m7","!t5"," !custom")) assertEquals(READY,TicketCommandGuard.decide(command,"Me",false,requests))
            for (text in listOf("r","ready","ticket earned","hello")) assertEquals(SEND,TicketCommandGuard.decide(text,"Me",false,requests))
        }
        assertEquals(SEND,TicketCommandGuard.decide("!m7","Me",false,emptySet()))
        assertEquals(WAIT,TicketCommandGuard.decide("!m7","Me",null,emptySet()))
    }
    @Test fun `leader waits for every other requester then sends command once without r fallback`() {
        val requests = linkedSetOf("me","alex","steve")
        val tickets = ContestTicketState()
        val sent = mutableListOf<String>()
        fun attempt() = tickets.claim("Me",1,"Uncommon",20) {
            when (TicketCommandGuard.decide("!m7","Me",true,requests)) {
                WAIT -> false
                READY -> error("Leader must never fall back to r")
                SEND -> { sent += "!m7"; true }
            }
        }
        assertFalse(attempt())
        assertTrue(TicketCommandGuard.observe("Party > Alex: r",requests))
        assertFalse(attempt())
        assertTrue(TicketCommandGuard.observe("Party > Steve: ready",requests))
        assertTrue(attempt()); assertFalse(attempt())
        assertEquals(listOf("!m7"),sent)
    }
    @Test fun `leader with only own contest request sends the chosen command immediately`() {
        assertEquals(SEND,TicketCommandGuard.decide("!t5","Me",true,setOf("me")))
        assertEquals(SEND,TicketCommandGuard.decide("!m7","Me",true,emptySet()))
    }
    @Test fun `departures release just that player and party replacement clears requests`() {
        val requests = linkedSetOf("alex","steve")
        assertTrue(TicketCommandGuard.observeDeparture("[MVP+] Alex has left the party.",requests))
        assertEquals(setOf("steve"),requests)
        assertFalse(TicketCommandGuard.observeDeparture("Party > Steve: The party was disbanded.",requests))
        assertTrue(TicketCommandGuard.observeDeparture("You have joined [MVP+] Pat's party!",requests))
        assertTrue(requests.isEmpty())
    }
}
