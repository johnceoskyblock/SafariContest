package net.johnceo.safaricontest.party

import net.johnceo.safaricontest.contest.ContestTicketState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ContestPartyAuthorizationTest {
    @Test fun `joining with an already earned ticket cannot send until self contest request`() {
        val auth = ContestPartyAuthorization()
        val tickets = ContestTicketState()
        auth.observePartySignal("You have joined Alex's party!")
        auth.observeRoster("self",setOf("self","alex"))
        var sends = 0
        fun attempt() = tickets.claim("self",10,"Uncommon",60) {
            if (auth.allows("self",10)) { sends++; true } else false
        }
        assertFalse(attempt()); assertEquals(0,sends)
        auth.observe("Party > [MVP+] Me: !dt contest","self","Me",10)
        assertTrue(attempt()); assertFalse(attempt()); assertEquals(1,sends)
    }
    @Test fun `only the local party echo with exact contest reason authorizes any ticket message`() {
        for (text in listOf("Party > Alex: !dt contest","Guild > Me: !dt contest","From Me: !dt contest",
            "Me: !dt contest","Party > Me: !dt food","Party > Me: !dt","Party > Me: 1s","Party > Me: sec",
            "Party > Me: quoted !dt contest","Party > Alex: Me: !dt contest","Party > Me: !dt contestant")) {
            val auth = ContestPartyAuthorization()
            auth.observe(text,"self","Me",10)
            assertFalse(auth.allows("self",10),text)
        }
        val auth = ContestPartyAuthorization()
        auth.observe("§9Party §8> §b[MVP+] mE§f: !DT   CONTEST ","self","Me",10)
        assertTrue(auth.allows("self",10))
        assertFalse(auth.allows("another-account",10)); assertFalse(auth.allows("self",11))
    }
    @Test fun `own ready does not undo having requested contest in this party`() {
        val auth = ContestPartyAuthorization()
        auth.observe("Party > Me: !dt contest","self","Me",10)
        auth.observe("Party > Me: r","self","Me",10)
        assertTrue(auth.allows("self",10))
    }
    @Test fun `joining leaving disbanding and disconnecting remove previous party permission`() {
        for (signal in listOf("You have joined Alex's party!","You left the party.",
            "You have been kicked from the party by [MVP+] Alex!","The party was disbanded.",
            "[MVP+] Alex has disbanded the party!","You are not currently in a party.")) {
            val auth = ContestPartyAuthorization()
            auth.observe("Party > Me: !dt contest","self","Me",10)
            assertTrue(auth.observePartySignal(signal),signal)
            assertFalse(auth.allows("self",10),signal)
        }
        val auth = ContestPartyAuthorization()
        auth.observe("Party > Me: !dt contest","self","Me",10)
        auth.reset(); assertFalse(auth.allows("self",10))
    }
    @Test fun `same party updates and transfers retain permission but disjoint roster does not`() {
        val auth = ContestPartyAuthorization()
        auth.observeRoster("self",setOf("self","alex"))
        auth.observe("Party > Me: !dt contest","self","Me",10)
        auth.observeRoster("self",setOf("self","alex","steve"))
        assertTrue(auth.allows("self",10))
        assertFalse(auth.observePartySignal("Alex has promoted Me to Party Leader!"))
        assertFalse(auth.observePartySignal("Party > Alex: You left the party."))
        auth.observeRoster("self",setOf("self","steve"))
        assertTrue(auth.allows("self",10))
        auth.observeRoster("self",setOf("self","pat"))
        assertFalse(auth.allows("self",10))
    }
    @Test fun `solo and account changes cannot retain permission`() {
        val auth = ContestPartyAuthorization()
        auth.observeRoster("self",setOf("self","alex"))
        auth.observe("Party > Me: !dt contest","self","Me",10)
        auth.observeRoster("self",setOf("self"))
        assertFalse(auth.allows("self",10))
        auth.observe("Party > Me: !dt contest","self","Me",10)
        auth.observeRoster("other",setOf("other","alex"))
        assertFalse(auth.allows("self",10))
    }
}
