package net.johnceo.safaricontest.contest

import net.johnceo.safaricontest.safari.ContestInstance
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ContestInstanceTest {
    @Test fun `only exact dungeon and kuudra modes permit automatic contest starts`() {
        for (mode in listOf("dungeon","kuudra","DUNGEON")) assertTrue(ContestInstance.eligible(mode))
        for (mode in listOf(null,"","dungeon_hub","hub","dynamic","garden","foraging_3","crimson_isle","island","combat_3")) assertFalse(ContestInstance.eligible(mode),mode)
    }
    @Test fun `entering an instance after the start does not catch up a skipped announcement`() {
        val state = ContestStartState()
        assertFalse(state.poll(1,1190,true,true))
        assertFalse(state.poll(2,0,true,ContestInstance.eligible("dungeon_hub")))
        assertFalse(state.poll(2,4,true,ContestInstance.eligible("dungeon")))
        assertTrue(state.poll(3,0,true,ContestInstance.eligible("kuudra")))
    }
}
