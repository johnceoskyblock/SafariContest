package net.johnceo.safaricontest.compat

internal object EspBoxLines {
    inline fun edges(minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float,
        emit: (Float, Float, Float, Float, Float, Float, Int) -> Unit) {
        for (i in 0..7) {
            val x = if (i and 1 == 0) minX else maxX
            val y = if (i and 2 == 0) minY else maxY
            val z = if (i and 4 == 0) minZ else maxZ
            var bit = 1
            while (bit <= 4) {
                if (i and bit == 0) {
                    val end = i or bit
                    val ex = if (end and 1 == 0) minX else maxX
                    val ey = if (end and 2 == 0) minY else maxY
                    val ez = if (end and 4 == 0) minZ else maxZ
                    if (x.toBits() != ex.toBits() || y.toBits() != ey.toBits() || z.toBits() != ez.toBits())
                        emit(x, y, z, ex, ey, ez, if (bit == 1) 0 else if (bit == 2) 1 else 2)
                }
                bit = bit shl 1
            }
        }
    }
}
