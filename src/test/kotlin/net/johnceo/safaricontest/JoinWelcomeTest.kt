package net.johnceo.safaricontest

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JoinWelcomeTest {
    @Test fun `welcome waits for stable skyblock and only shows once per connection`() {
        val state = JoinWelcomeState()
        assertFalse(state.shouldShow(false,0))
        assertFalse(state.shouldShow(true,100))
        assertFalse(state.shouldShow(false,2000))
        assertFalse(state.shouldShow(true,3000))
        assertTrue(state.shouldShow(true,5000))
        assertFalse(state.shouldShow(true,10000))
    }
    @Test fun `welcome contains mod name settings link and matched dividers`() {
        val text = JoinWelcome.message().string
        assertTrue(text.contains("SafariContest")); assertTrue(text.contains("/s"))
        assertFalse(text.contains("/sc")); assertTrue(text.contains("Open settings"))
        val lines = text.lines()
        assertEquals(lines.first(),lines.last())
    }
    @Test fun `latest release appears below settings for updates and current builds`() {
        for (status in listOf("Up to Date","Update available","Development build")) {
            val text = JoinWelcome.message("v1.1.1",status).string
            assertTrue(text.indexOf("Latest release: v1.1.1") > text.indexOf("/s — Open settings"))
            assertTrue(text.contains("($status)"))
            assertEquals(text.lines().first(),text.lines().last())
        }
        val failed = JoinWelcome.message(null,"Couldn't check").string
        assertTrue(failed.contains("Latest release: Couldn't check")); assertTrue(failed.contains("/s — Open settings"))
    }
    @Test fun `welcome waits for async version result and still shows on failure`() {
        val state = JoinWelcomeState()
        assertFalse(state.shouldShow(true,0,false))
        assertFalse(state.shouldShow(true,3000,false))
        assertTrue(state.shouldShow(true,10000,true))
        assertFalse(state.shouldShow(true,20000,true))
    }
}
