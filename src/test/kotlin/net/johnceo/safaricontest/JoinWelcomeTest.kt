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
    @Test fun `welcome contains only mod name and the main command`() {
        val text = JoinWelcome.message().string
        assertTrue(text.contains("SafariContest")); assertTrue(text.contains("/s"))
        assertFalse(text.contains("/sc")); assertFalse(text.contains("Settings"))
    }
}
