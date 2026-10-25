package mods.eln.misc

import net.minecraft.world.phys.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals

// Coordinate needs registries (dimension keys), which plain JUnit cannot bootstrap on Forge; it is covered by GameTests.
class GeometryPortTest {
    @Test
    fun immutableRotationsMatchArrayRotations() {
        val start = doubleArrayOf(1.0, 2.0, 3.0)
        for (direction in Direction.values()) {
            val expected = start.copyOf().also { direction.rotateFromXN(it) }
            assertEquals(Vec3(expected[0], expected[1], expected[2]), direction.rotatedFromXN(Vec3(1.0, 2.0, 3.0)), "$direction")
        }
        for (lrdu in LRDU.values()) {
            val expected = start.copyOf().also { lrdu.rotateOnXnLeft(it) }
            assertEquals(Vec3(expected[0], expected[1], expected[2]), lrdu.rotatedOnXnLeft(Vec3(1.0, 2.0, 3.0)), "$lrdu")
        }
    }
}
