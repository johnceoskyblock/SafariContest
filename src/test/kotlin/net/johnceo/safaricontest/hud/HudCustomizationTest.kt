package net.johnceo.safaricontest.hud

import net.johnceo.safaricontest.config.ContestConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Properties

class HudCustomizationTest {
    @Test fun `dragging resizing and normalized placement persist exactly`() {
        val layout = HudLayout(.01f,.02f)
        layout.move(200.0,80.0,640,360,100,17)
        layout.scale = .7f
        val before = layout.bounds(640,360,100,17)
        val saved = Properties().also { layout.save(it,"contest") }
        val restored = HudLayout(0f,0f).apply { load(saved,"contest") }
        assertEquals(before,restored.bounds(640,360,100,17))
        assertEquals(.7f,restored.scale)
        restored.move(-999.0,9999.0,320,180,100,17)
        val edge = restored.bounds(320,180,100,17)
        assertEquals(0,edge.x); assertTrue(edge.y+edge.height <= 180)
        restored.scale = Float.NaN; assertEquals(1f,restored.scale)
    }
    @Test fun `center and edge snapping use SparklingMutuals tolerances`() {
        assertEquals(270,HudSnapping.position(272.0,20.0,640,360,100,17).x)
        assertEquals(0,HudSnapping.position(2.0,20.0,640,360,100,17).x)
        assertEquals(20,HudSnapping.position(20.0,20.0,640,360,100,17).x)
    }
    @Test fun `color controls accept only complete six digit RGB colors`() {
        assertEquals(0xE6C878,ContestConfig.parseColor("#E6C878"))
        assertNull(ContestConfig.parseColor("#123")); assertNull(ContestConfig.parseColor("not a color"))
        assertEquals("#0000FF",ContestConfig.hex(255))
    }
}
