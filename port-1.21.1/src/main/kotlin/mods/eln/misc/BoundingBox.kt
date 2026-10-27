package mods.eln.misc

import com.google.common.base.MoreObjects
import net.minecraft.world.phys.Vec3
import java.lang.Float.NEGATIVE_INFINITY
import java.lang.Float.POSITIVE_INFINITY

class BoundingBox(xMin: Float, xMax: Float, yMin: Float, yMax: Float, zMin: Float, zMax: Float) {
    val min: Vec3 = Vec3(xMin.toDouble(), yMin.toDouble(), zMin.toDouble())
    val max: Vec3 = Vec3(xMax.toDouble(), yMax.toDouble(), zMax.toDouble())

    fun merge(other: BoundingBox): BoundingBox {
        return BoundingBox(
            min.x.coerceAtMost(other.min.x).toFloat(),
            max.x.coerceAtLeast(other.max.x).toFloat(),
            min.y.coerceAtMost(other.min.y).toFloat(),
            max.y.coerceAtLeast(other.max.y).toFloat(),
            min.z.coerceAtMost(other.min.z).toFloat(),
            max.z.coerceAtLeast(other.max.z).toFloat()
        )
    }

    fun centre(): Vec3 {
        return Vec3(
            min.x + (max.x - min.x) / 2,
            min.y + (max.y - min.y) / 2,
            min.z + (max.z - min.z) / 2
        )
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
            .add("min", min)
            .add("max", max)
            .toString()
    }

    companion object {
        @JvmStatic
        fun mergeIdentity(): BoundingBox {
            return BoundingBox(POSITIVE_INFINITY, NEGATIVE_INFINITY, POSITIVE_INFINITY, NEGATIVE_INFINITY, POSITIVE_INFINITY, NEGATIVE_INFINITY)
        }
    }

}
