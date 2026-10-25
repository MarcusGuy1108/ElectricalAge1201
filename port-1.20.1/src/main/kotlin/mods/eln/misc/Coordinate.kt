package mods.eln.misc

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.server.ServerLifecycleHooks
import kotlin.math.abs
import kotlin.math.floor

/**
 * Block position plus dimension. 1.20.1 has no numeric dimension ids, so the dimension is a level key.
 */
class Coordinate : INBTTReady {
    @JvmField
    var x = 0
    @JvmField
    var y = 0
    @JvmField
    var z = 0
    @JvmField
    var dimension: ResourceKey<Level> = Level.OVERWORLD

    constructor() {
        x = 0
        y = 0
        z = 0
        dimension = Level.OVERWORLD
    }

    constructor(coord: Coordinate) {
        x = coord.x
        y = coord.y
        z = coord.z
        dimension = coord.dimension
    }

    constructor(nbt: CompoundTag, str: String) {
        readFromNBT(nbt, str)
    }

    // Emulates the default Minecraft behavior for determining block coordinates
    constructor(v: Vec3, d: ResourceKey<Level>) {
        x = floor(v.x).toInt()
        y = floor(v.y).toInt()
        z = floor(v.z).toInt()
        dimension = d
    }

    override fun hashCode(): Int {
        return (x + y) * 0x10101010 + z
    }

    fun worldDimension(): ResourceKey<Level> {
        return dimension
    }

    private var w: Level? = null
    fun world(): Level {
        return w ?: checkNotNull(ServerLifecycleHooks.getCurrentServer()?.getLevel(dimension)) {
            "Dimension ${dimension.location()} is not loaded"
        }
    }

    constructor(x: Int, y: Int, z: Int, dimension: ResourceKey<Level>) {
        this.x = x
        this.y = y
        this.z = z
        this.dimension = dimension
    }

    constructor(x: Int, y: Int, z: Int, world: Level) {
        this.x = x
        this.y = y
        this.z = z
        dimension = world.dimension()
        if (world.isClientSide) w = world
    }

    constructor(entity: BlockEntity) {
        x = entity.blockPos.x
        y = entity.blockPos.y
        z = entity.blockPos.z
        val level = checkNotNull(entity.level) { "Block entity at ${entity.blockPos} has no level" }
        dimension = level.dimension()
        if (level.isClientSide) w = level
    }

    val blockPos: BlockPos
        get() = BlockPos(x, y, z)

    fun newWithOffset(x: Int, y: Int, z: Int): Coordinate {
        return Coordinate(this.x + x, this.y + y, this.z + z, dimension)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Coordinate) return false
        return other.x == x && other.y == y && other.z == z && other.dimension == dimension
    }

    override fun readFromNBT(nbt: CompoundTag, str: String) {
        x = nbt.getInt(str + "x")
        y = nbt.getInt(str + "y")
        z = nbt.getInt(str + "z")
        dimension = dimensionFromString(nbt.getString(str + "d"))
    }

    override fun writeToNBT(nbt: CompoundTag, str: String) {
        nbt.putInt(str + "x", x)
        nbt.putInt(str + "y", y)
        nbt.putInt(str + "z", z)
        nbt.putString(str + "d", dimension.location().toString())
    }

    override fun toString(): String {
        return "X : $x Y : $y Z : $z D : ${dimension.location()}"
    }

    fun move(dir: Direction) {
        when (dir) {
            Direction.XN -> x--
            Direction.XP -> x++
            Direction.YN -> y--
            Direction.YP -> y++
            Direction.ZN -> z--
            Direction.ZP -> z++
        }
    }

    fun moved(direction: Direction): Coordinate {
        val moved = Coordinate(this)
        moved.move(direction)
        return moved
    }

    var block: Block
        get() = world().getBlockState(blockPos).block
        set(b) {
            world().setBlockAndUpdate(blockPos, b.defaultBlockState())
        }

    fun getAxisAlignedBB(ray: Int): AABB {
        return AABB(
            (x - ray).toDouble(), (y - ray).toDouble(), (z - ray).toDouble(),
            (x + ray + 1).toDouble(), (y + ray + 1).toDouble(), (z + ray + 1).toDouble())
    }

    fun distanceTo(e: Entity): Double {
        return abs(e.x - (x + 0.5)) + abs(e.y - (y + 0.5)) + abs(e.z - (z + 0.5))
    }

    // PORT: block metadata (meta/setMetadata) does not exist in 1.20.1; callers move to block states.

    val blockExist: Boolean
        get() {
            val w = ServerLifecycleHooks.getCurrentServer()?.getLevel(dimension) ?: return false
            return w.isLoaded(blockPos)
        }
    val worldExist: Boolean
        get() = ServerLifecycleHooks.getCurrentServer()?.getLevel(dimension) != null

    fun copyTo(v: DoubleArray) {
        v[0] = x + 0.5
        v[1] = y + 0.5
        v[2] = z + 0.5
    }

    fun setPosition(vp: DoubleArray) {
        x = vp[0].toInt()
        y = vp[1].toInt()
        z = vp[2].toInt()
    }

    // Emulates the default Minecraft behavior for determining block coordinates
    fun setPosition(vp: Vec3) {
        x = floor(vp.x).toInt()
        y = floor(vp.y).toInt()
        z = floor(vp.z).toInt()
    }

    val tileEntity: BlockEntity?
        get() = world().getBlockEntity(blockPos)

    fun invalidate() {
        x = -1
        y = -1
        z = -1
        dimension = INVALID_DIMENSION
    }

    val isValid: Boolean
        get() = dimension != INVALID_DIMENSION

    fun trueDistanceTo(c: Coordinate): Double {
        val dx = (x - c.x).toLong()
        val dy = (y - c.y).toLong()
        val dz = (z - c.z).toLong()
        return Math.sqrt((dx * dx + dy * dy + dz * dz).toDouble())
    }

    fun setDimension(dimension: ResourceKey<Level>) {
        this.dimension = dimension
        w = null
    }

    fun copyFrom(c: Coordinate) {
        x = c.x
        y = c.y
        z = c.z
        dimension = c.dimension
    }

    fun applyTransformation(front: Direction, coordinate: Coordinate) {
        front.rotateFromXN(this)
        x += coordinate.x
        y += coordinate.y
        z += coordinate.z
    }

    fun setWorld(worldObj: Level) {
        if (worldObj.isClientSide) w = worldObj
        dimension = worldObj.dimension()
    }

    operator fun compareTo(o: Coordinate): Int {
        return when {
            dimension != o.dimension ->
                dimension.location().compareTo(o.dimension.location())
            x != o.x ->
                x - o.x
            y != o.y ->
                y - o.y
            z != o.z ->
                z - o.z
            else -> 0
        }
    }

    fun subtract(b: Coordinate): Coordinate {
        return newWithOffset(-b.x, -b.y, -b.z)
    }

    fun negate(): Coordinate {
        return Coordinate(-x, -y, -z, dimension)
    }

    // Emulates the default Minecraft behavior for determining block coordinates
    fun toVec3(): Vec3 {
        return Vec3(this.x + 0.5, this.y + 0.5, this.z + 0.5)
    }

    companion object {
        private val INVALID_DIMENSION: ResourceKey<Level> =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation("eln", "invalid"))

        // Missing or malformed ids read as the overworld, which is what 1.7.10 dimension 0 meant.
        @JvmStatic
        fun dimensionFromString(id: String): ResourceKey<Level> {
            if (id.isEmpty()) return Level.OVERWORLD
            val location = ResourceLocation.tryParse(id) ?: return Level.OVERWORLD
            return ResourceKey.create(Registries.DIMENSION, location)
        }

        @JvmStatic
        fun getAxisAlignedBB(a: Coordinate, b: Coordinate): AABB {
            return AABB(
                a.x.coerceAtMost(b.x).toDouble(), a.y.coerceAtMost(b.y).toDouble(), a.z.coerceAtMost(b.z).toDouble(),
                a.x.coerceAtLeast(b.x) + 1.0, a.y.coerceAtLeast(b.y) + 1.0, a.z.coerceAtLeast(b.z) + 1.0)
        }
    }
}
