@file:Suppress("NAME_SHADOWING")
package mods.eln.misc

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import mods.eln.misc.Utils.isTheClass
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import net.minecraft.core.Direction as McDirection

/**
 * Represents the 6 possible directions along the axis of a block.
 */
enum class Direction(var int: Int) {
    /**
     * -X
     */
    XN(0),

    /**
     * +X
     */
    XP(1),

    /**
     * -Y
     */
    YN(2),  //MC-Code starts with 0 here

    /**
     * +Y
     */
    YP(3),  // 1...

    /**
     * -Z
     */
    ZN(4),

    /**
     * +Z
     */
    ZP(5);

    val isNotY: Boolean
        get() = this != YP && this != YN
    val isY: Boolean
        get() = this == YP || this == YN

    fun applyTo(vector: DoubleArray, distance: Double) {
        if (int == 0) vector[0] -= distance
        if (int == 1) vector[0] += distance
        if (int == 2) vector[1] -= distance
        if (int == 3) vector[1] += distance
        if (int == 4) vector[2] -= distance
        if (int == 5) vector[2] += distance
    }

    fun applyTo(vector: IntArray, distance: Int) {
        if (int == 0) vector[0] -= distance
        if (int == 1) vector[0] += distance
        if (int == 2) vector[1] -= distance
        if (int == 3) vector[1] += distance
        if (int == 4) vector[2] -= distance
        if (int == 5) vector[2] += distance
    }

    val horizontalIndex: Int
        get() = when (this) {
            XN -> 0
            XP -> 1
            YN -> 0
            YP -> 0
            ZN -> 2
            ZP -> 3
        }
    /*public CoordinateTuple ApplyToCoordinates(CoordinateTuple coordinates) {
		CoordinateTuple ret = new CoordinateTuple(coordinates);
		
		ret.coords[dir / 2] += GetSign();
		
		return ret;
	}*/
    /**
     * Get the tile entity next to a tile entity following this direction.
     *
     * @param tileEntity tile entity to check
     * @return Adjacent tile entity or null if none exists
     */
    fun applyToTileEntity(tileEntity: BlockEntity?): BlockEntity? {
        if (tileEntity == null) return null
        val level = tileEntity.level ?: return null
        val pos = tileEntity.blockPos.relative(toForge())
        return if (level.isLoaded(pos)) level.getBlockEntity(pos) else null
    }

    fun applyToTileEntityAndSameClassThan(tileEntity: BlockEntity?, c: Class<*>?): BlockEntity? {
        if (tileEntity == null) return null
        val findedEntity = applyToTileEntity(tileEntity) ?: return null
        return if (!isTheClass(findedEntity, c!!)) null else findedEntity
    }

    /**
     * Get the inverse of this direction (XN -> XP, XP -> XN, etc.)
     *
     * @return Inverse direction
     */
    val inverse: Direction
        get() {
            val inverseDir = int - sign
            for (direction in values()) {
                if (direction.int == inverseDir) return direction
            }
            return this
        }

    /**
     * Convert this direction to a Minecraft side value.
     *
     * @return Minecraft side value
     */
    fun toSideValue(): Int {
        return (int + 4) % 6
    }

    /**
     * Determine direction sign (N for negative or P for positive).
     *
     * @return -1 if the direction is negative, +1 if the direction is positive
     */
    private val sign: Int
        get() = int % 2 * 2 - 1

    // PORT: renderBlockFace (GL11 immediate mode) returns as VertexConsumer code with the renderers.

    fun right(): Direction {
        return when (this) {
            XN -> ZP
            XP -> ZN
            YN -> ZN
            YP -> ZP
            ZN -> XN
            ZP -> XP
        }
    }

    fun left(): Direction {
        return right().inverse
    }

    fun up(): Direction {
        return when (this) {
            XN -> YP
            XP -> YP
            YN -> XP
            YP -> XP
            ZN -> YP
            ZP -> YP
        }
    }

    fun down(): Direction {
        return up().inverse
    }

    fun back(): Direction {
        return inverse
    }

    fun applyLRDU(lrdu: LRDU): Direction {
        return when (lrdu) {
            LRDU.Down -> down()
            LRDU.Left -> left()
            LRDU.Right -> right()
            LRDU.Up -> up()
        }
    }

    fun getLRDUGoingTo(target: Direction): LRDU? {
        for (lrdu in LRDU.values()) {
            if (target == applyLRDU(lrdu)) return lrdu
        }
        return null
    }

    fun glRotateXnRef(pose: PoseStack) {
        //toCheck
        when (this) {
            XN -> {
            }
            XP ->                 //pose.scale(-1f, 1, -1f);
                pose.mulPose(Axis.of(Vector3f(0f, 1f, 0f)).rotationDegrees(180f))
            YN -> {
                pose.mulPose(Axis.of(Vector3f(0f, 0f, 1f)).rotationDegrees(90f))
                pose.scale(1f, -1f, -1f)
            }
            YP -> pose.mulPose(Axis.of(Vector3f(0f, 0f, -1f)).rotationDegrees(90f))
            ZN -> pose.mulPose(Axis.of(Vector3f(0f, 1f, 0f)).rotationDegrees(270f))
            ZP -> pose.mulPose(Axis.of(Vector3f(0f, 1f, 0f)).rotationDegrees(90f))
        }
    }

    fun glRotateXnRefInv(pose: PoseStack) {
        //toCheck
        when (this) {
            XN -> {
            }
            XP ->                 //pose.scale(-1f, 1, -1f);
                pose.mulPose(Axis.of(Vector3f(0f, -1f, 0f)).rotationDegrees(180f))
            YN -> {
                pose.scale(1f, -1f, -1f)
                pose.mulPose(Axis.of(Vector3f(0f, 0f, -1f)).rotationDegrees(90f))
            }
            YP -> pose.mulPose(Axis.of(Vector3f(0f, 0f, 1f)).rotationDegrees(90f))
            ZN -> pose.mulPose(Axis.of(Vector3f(0f, -1f, 0f)).rotationDegrees(270f))
            ZP -> pose.mulPose(Axis.of(Vector3f(0f, -1f, 0f)).rotationDegrees(90f))
        }
    }

    fun glRotateZnRef(pose: PoseStack) {
        //toCheck
        when (this) {
            XN -> pose.mulPose(Axis.of(Vector3f(0f, 1f, 0f)).rotationDegrees(90f))
            XP -> pose.mulPose(Axis.of(Vector3f(0f, -1f, 0f)).rotationDegrees(90f))
            YN -> {
                pose.mulPose(Axis.of(Vector3f(1f, 0f, 0f)).rotationDegrees(90f))
                pose.scale(1f, -1f, 1f)
            }
            YP -> {
                pose.mulPose(Axis.of(Vector3f(1f, 0f, 0f)).rotationDegrees(90f))
                pose.scale(1f, 1f, 1f)
            }
            ZN -> {
            }
            ZP -> pose.mulPose(Axis.of(Vector3f(0f, 1f, 0f)).rotationDegrees(180f))
        }
    }

    fun getTileEntity(coordinate: Coordinate): BlockEntity? {
        var x = coordinate.x
        var y = coordinate.y
        var z = coordinate.z
        when (this) {
            XN -> x--
            XP -> x++
            YN -> y--
            YP -> y++
            ZN -> z--
            ZP -> z++
        }
        return coordinate.world().getBlockEntity(BlockPos(x, y, z))
    }

    fun writeToNBT(nbt: CompoundTag, name: String?) {
        nbt.putByte(name, int.toByte())
    }

    fun rotateFromXN(p: DoubleArray) {
        val x = p[0]
        val y = p[1]
        val z = p[2]
        when (this) {
            XN -> {
            }
            XP -> {
                p[0] = -x
                p[2] = -z
            }
            YN -> {
                p[0] = y
                p[1] = x
                p[2] = -z
            }
            YP -> {
                p[0] = y
                p[1] = -x
                p[2] = z
            }
            ZN -> {
                p[0] = -z
                p[2] = x
            }
            ZP -> {
                p[0] = z
                p[2] = -x
            }
        }
    }

    fun rotateFromXN(p: IntArray) {
        val x = p[0]
        val y = p[1]
        val z = p[2]
        when (this) {
            XN -> {
            }
            XP -> {
                p[0] = -x
                p[2] = -z
            }
            YN -> {
                p[0] = y
                p[1] = x
                p[2] = -z
            }
            YP -> {
                p[0] = y
                p[1] = -x
                p[2] = z
            }
            ZN -> {
                p[0] = -z
                p[2] = x
            }
            ZP -> {
                p[0] = z
                p[2] = -x
            }
        }
    }

    // 1.20.1 Vec3 is immutable, so this returns the rotated vector instead of changing p.
    fun rotatedFromXN(p: Vec3): Vec3 {
        val x = p.x
        val y = p.y
        val z = p.z
        return when (this) {
            XN -> p
            XP -> Vec3(-x, y, -z)
            YN -> Vec3(y, x, -z)
            YP -> Vec3(y, -x, z)
            ZN -> Vec3(-z, y, x)
            ZP -> Vec3(z, y, -x)
        }
    }

    fun rotateFromXN(p: Coordinate) {
        val x = p.x
        val y = p.y
        val z = p.z
        when (this) {
            XN -> {
            }
            XP -> {
                p.x = -x
                p.z = -z
            }
            YN -> {
                p.x = y
                p.y = x
                p.z = -z
            }
            YP -> {
                p.x = y
                p.y = -x
                p.z = z
            }
            ZN -> {
                p.x = -z
                p.z = x
            }
            ZP -> {
                p.x = z
                p.z = -x
            }
        }
    }

    fun glTranslate(pose: PoseStack, v: Float) {
        when (this) {
            XN -> pose.translate(-v, 0f, 0f)
            XP -> pose.translate(v, 0f, 0f)
            YN -> pose.translate(0f, -v, 0f)
            YP -> pose.translate(0f, v, 0f)
            ZN -> pose.translate(0f, 0f, -v)
            ZP -> pose.translate(0f, 0f, v)
        }
    }

    fun toForge(): McDirection {
        return when (this) {
            YN -> McDirection.DOWN
            XP -> McDirection.EAST
            ZN -> McDirection.NORTH
            ZP -> McDirection.SOUTH
            YP -> McDirection.UP
            XN -> McDirection.WEST
        }
    }

    fun glRotateZnRefInv(pose: PoseStack) {
        when (this) {
            XN -> pose.mulPose(Axis.of(Vector3f(0f, 1f, 0f)).rotationDegrees(-90f))
            XP -> pose.mulPose(Axis.of(Vector3f(0f, -1f, 0f)).rotationDegrees(-90f))
            YN -> {
                pose.mulPose(Axis.of(Vector3f(1f, 0f, 0f)).rotationDegrees(-90f))
                pose.scale(1f, -1f, 1f)
            }
            YP -> {
                pose.mulPose(Axis.of(Vector3f(1f, 0f, 0f)).rotationDegrees(-90f))
                pose.scale(1f, 1f, 1f)
            }
            ZN -> {
            }
            ZP -> pose.mulPose(Axis.of(Vector3f(0f, 1f, 0f)).rotationDegrees(-180f))
        }
    }

    fun toHybridNodeDirection(): HybridNodeDirection {
        return when (this) {
            XN -> HybridNodeDirection.XN
            XP -> HybridNodeDirection.XP
            YN -> HybridNodeDirection.YN
            YP -> HybridNodeDirection.YP
            ZN -> HybridNodeDirection.ZN
            ZP -> HybridNodeDirection.ZP
        }
    }

    companion object {
        val intToDir = arrayOf(XN, XP, YN, YP, ZN, ZP)
        val all = arrayOf(XN, XP, YN, YP, ZN, ZP)
        val axes = arrayOf(arrayOf(XN, XP), arrayOf(YN, YP), arrayOf(ZN, ZP))
        @JvmStatic
        fun fromHorizontalIndex(nbr: Int): Direction {
            return when (nbr) {
                0 -> XN
                1 -> XP
                2 -> ZN
                3 -> ZP
                else -> XN
            }
        }

        @JvmStatic
        fun fromInt(idx: Int): Direction? {
            for (direction in values()) {
                if (direction.int == idx) return direction
            }
            return null
        }

        @JvmStatic
        fun fromIntMinecraftSide(idx: Int): Direction? {
            var idx = idx
            idx = (idx + 2) % 6
            for (direction in values()) {
                if (direction.int == idx) return direction
            }
            return null
        }

        @JvmStatic
        fun readFromNBT(nbt: CompoundTag, name: String): Direction? {
            return fromInt(nbt.getByte(name).toInt())
        }

        fun from(direction: McDirection?): Direction {
            return when (direction) {
                McDirection.DOWN -> YN
                McDirection.EAST -> XP
                McDirection.NORTH -> ZN
                McDirection.SOUTH -> ZP
                McDirection.UP -> YP
                McDirection.WEST -> XN
                else -> YN
            }
        }

        fun rotateLocalDirection(front: Direction, local: Direction): Direction {
            val vec = directionToVector(local)
            front.rotateFromXN(vec)
            return fromVector(vec)
        }

        private fun directionToVector(direction: Direction): IntArray {
            return when (direction) {
                XN -> intArrayOf(-1, 0, 0)
                XP -> intArrayOf(1, 0, 0)
                YN -> intArrayOf(0, -1, 0)
                YP -> intArrayOf(0, 1, 0)
                ZN -> intArrayOf(0, 0, -1)
                ZP -> intArrayOf(0, 0, 1)
            }
        }

        private fun fromVector(vec: IntArray): Direction {
            return when {
                vec[0] > 0 -> XP
                vec[0] < 0 -> XN
                vec[1] > 0 -> YP
                vec[1] < 0 -> YN
                vec[2] > 0 -> ZP
                else -> ZN
            }
        }
    }
}
