package mods.eln.misc

import java.io.IOException
import net.minecraft.world.item.Item
import mods.eln.generic.IDescriptorItem
import mods.eln.generic.GenericItem
import mods.eln.Eln
import mods.eln.client.GL11
import mods.eln.network.ElnNetwork
import mods.eln.sim.mna.SubSystem
import mods.eln.node.ITileEntitySpawnClient
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.util.Mth
import net.minecraft.world.Container
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.LevelResource
import net.minecraftforge.server.ServerLifecycleHooks
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.text.DecimalFormat
import java.util.Random
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

// PORT: partial; the rest of 1.7.10 Utils.kt comes over as the code that needs it is ported.
object Utils {
    const val minecraftDay = (60 * 24).toDouble()
    val random = Random()

    @JvmStatic
    var uuid = 1
        get() {
            if (field < 1) field = 1
            return field++
        }
        private set

    @JvmStatic
    fun rand(min: Double, max: Double): Double {
        return random.nextDouble() * (max - min) + min
    }

    @JvmStatic
    fun isTheClass(o: Any, c: Class<*>): Boolean {
        if (o.javaClass == c) return true
        var classIterator: Class<*>? = o.javaClass.superclass
        while (classIterator != null) {
            if (classIterator == c) {
                return true
            }
            classIterator = classIterator.superclass
        }
        return false
    }

    @JvmStatic
    fun println(str: String?) {
        if (Eln.config.getBooleanOrElse("debug.logging.enabled", false)) Eln.logger.info(str)
    }

    @JvmStatic
    fun println(str: Any?) {
        if (str != null) println(str.toString())
    }

    @JvmStatic
    fun println(format: String?, vararg data: Any?) {
        println(String.format(format!!, *data))
    }

    @JvmStatic
    fun floatToStr(f: Double, high: Int, low: Int): String {
        var temp = ""
        for (idx in 0 until high) temp += "0"
        temp = "$temp."
        for (idx in 0 until low) temp += "0"
        val str = DecimalFormat(temp).format(f)
        var idx = 0
        val ch = str.toCharArray()
        while (true) {
            if (str.length == idx) break
            if (ch[idx] == '.') {
                ch[idx - 1] = '0'
                break
            }
            if (ch[idx] != '0' && ch[idx] != ' ') break
            ch[idx] = '_'
            idx++
        }
        return String(ch)
    }


    @JvmStatic
    fun entityLivingViewDirection(entityLiving: LivingEntity): Direction {
        if (entityLiving.xRot > 45) return Direction.YN
        if (entityLiving.xRot < -45) return Direction.YP
        val dirx = Mth.floor((entityLiving.yRot * 4.0f / 360.0f).toDouble() + 0.5) and 3
        if (dirx == 3) return Direction.XP
        if (dirx == 0) return Direction.ZP
        return if (dirx == 1) Direction.XN else Direction.ZN
    }

    @JvmStatic
    fun entityLivingHorizontalViewDirection(entityLiving: LivingEntity): Direction {
        val dirx = Mth.floor((entityLiving.yRot * 4.0f / 360.0f).toDouble() + 0.5) and 3
        if (dirx == 3) return Direction.XP
        if (dirx == 0) return Direction.ZP
        return if (dirx == 1) Direction.XN else Direction.ZN
    }

    @JvmStatic
    fun plotValue(value: Double): String {
        val valueAbs = abs(value)
        return when {
            valueAbs < 0.0001 ->
                "0"
            valueAbs < 0.000999 ->
                String.format("%1.2fµ", value * 10_000)
            valueAbs < 0.00999 ->
                String.format("%1.2fm", value * 1_000)
            valueAbs < 0.0999 ->
                String.format("%2.1fm", value * 1_000)
            valueAbs < 0.999 ->
                String.format("%3.0fm", value * 1_000)
            valueAbs < 9.99 ->
                String.format("%1.2f", value)
            valueAbs < 99.9 ->
                String.format("%2.1f", value)
            valueAbs < 999 ->
                String.format("%3.0f", value)
            valueAbs < 9999 ->
                String.format("%1.2fk", value / 1_000.0)
            valueAbs < 99999 ->
                String.format("%2.1fk", value / 1_000.0)
            valueAbs < 999999 ->
                String.format("%3.0fK", value / 1_000.0)
            valueAbs < 9999999 ->
                String.format("%1.2fM", value / 1_000_000.0)
            valueAbs < 99999999 ->
                String.format("%2.1fM", value / 1_000_000.0)
            valueAbs < 999999999 ->
                String.format("%3.0fM", value / 1_000_000.0)
            valueAbs < 9999999999 ->
                String.format("%1.2fG", value / 1_000_000_000.0)
            valueAbs < 99999999999 ->
                String.format("%2.1fG", value / 1_000_000_000.0)
            valueAbs < 999999999999 ->
                String.format("%3.0fG", value / 1_000_000_000.0)
            valueAbs < 9999999999999 ->
                String.format("%1.2fT", value / 1_000_000_000_000.0)
            valueAbs < 99999999999999 ->
                String.format("%2.1fT", value / 1_000_000_000_000.0)
            valueAbs < 999999999999999 ->
                String.format("%3.0fT", value / 1_000_000_000_000.0)
            valueAbs < 9999999999999999 ->
                String.format("%1.2fP", value / 1_000_000_000_000_000.0)
            valueAbs < 99999999999999999 ->
                String.format("%2.1fP", value / 1_000_000_000_000_000.0)
            valueAbs < 999999999999999999 ->
                String.format("%3.0fP", value / 1_000_000_000_000_000.0)
            valueAbs < 9999999999999999999.0 ->
                String.format("%1.2fE", value / 1_000_000_000_000_000_000.0)
            valueAbs < 99999999999999999999.0 ->
                String.format("%2.1fE", value / 1_000_000_000_000_000_000.0)
            else ->
                String.format("%3.0fE", value / 1_000_000_000_000_000_000.0)
        }
    }

    @JvmStatic
    fun plotValue(value: Double, unit: String): String {
        return plotValue(value) + unit
    }

    @JvmStatic
    fun plotVolt(value: Double): String {
        return plotValue(value, "V  ")
    }

    @JvmStatic
    fun plotVolt(header: String, value: Double): String {
        var header = header
        if (header != "") header += " "
        return header + plotVolt(value)
    }

    @JvmStatic
    fun plotAmpere(value: Double): String {
        return plotValue(value, "A  ")
    }

    @JvmStatic
    fun plotAmpere(header: String, value: Double): String {
        var header = header
        if (header != "") header += " "
        return header + plotAmpere(value)
    }

    @JvmStatic
    fun plotCelsius(header: String, value: Double): String {
        var header = header
        if (header != "") header += " "
        return header + plotValue(value, "\u00B0C ")
    }

    @JvmStatic
    fun plotPercent(header: String, value: Double): String {
        var header = header
        if (header != "") header += " "
        return if (value >= 1.0) header + String.format("%3.0f", value * 100.0) + "%   " else header + String.format("%3.1f", value * 100.0) + "%   "
    }

    @JvmStatic
    fun plotEnergy(value: Double): String {
        return plotValue(value, "J  ")
    }

    @JvmStatic
    fun plotEnergy(header: String, value: Double): String {
        var header = header
        if (header != "") header += " "
        return header + plotEnergy(value)
    }

    @JvmStatic
    fun plotRads(header: String, value: Double): String {
        var header = header
        if (header != "") header += " "
        return header + plotValue(value, "rad/s ")
    }

    @JvmStatic
    fun plotER(E: Double, R: Double): String {
        return plotEnergy("E", E) + plotRads("R", R)
    }

    @JvmStatic
    fun plotPower(value: Double): String {
        return plotValue(value, "W  ")
    }

    @JvmStatic
    fun plotPower(header: String, value: Double): String {
        var header = header
        if (header != "") header += " "
        return header + plotPower(value)
    }

    @JvmStatic
    fun plotOhm(value: Double): String {
        return plotValue(value, "\u2126 ")
    }

    @JvmStatic
    fun plotOhm(header: String, value: Double): String {
        var header = header
        if (header != "") header += " "
        return header + plotOhm(value)
    }

    @JvmStatic
    fun plotUIP(U: Double, I: Double): String {
        return plotVolt("U", U) + plotAmpere("I", I) + plotPower("P", abs(U * I))
    }

    @JvmStatic
    fun plotUIP(U: Double, I: Double, R: Double): String {
        return plotVolt("U", U) + plotAmpere("I", I) + plotPower("P", I * I * R)
    }

    @JvmStatic
    fun plotTime(value: Double): String {
        var value = value
        var str = ""
        if (value == 0.0) return str + "0''"
        val h: Int = (value / 3600).toInt()
        value %= 3600
        val mn: Int = (value / 60).toInt()
        value %= 60
        val s: Int = (value / 1).toInt()
        if (h != 0) str += h.toString() + "h"
        if (mn != 0) str += "$mn'"
        if (s != 0) str += "$s''"
        return str
    }

    @JvmStatic
    fun plotTime(header: String, value: Double): String {
        var header = header
        if (header != "") header += " "
        return header + plotTime(value)
    }

    @JvmStatic
    fun plotBuckets(header: String, buckets: Double): String {
        var header = header
        if (header != "") header += " "
        return header + plotValue(buckets, "B ")
    }

    @JvmStatic
    fun plotSignal(u: Double): String {
        return plotVolt("U", u) + plotPercent("Value", u / Eln.SVU)
    }

    @JvmStatic
    fun limit(value: Float, min: Float, max: Float): Float {
        return value.coerceAtMost(max).coerceAtLeast(min)
    }

    @JvmStatic
    fun limit(value: Double, min: Double, max: Double): Double {
        return value.coerceAtMost(max).coerceAtLeast(min)
    }

    @JvmStatic
    fun isCreative(entityPlayer: ServerPlayer): Boolean {
        return entityPlayer.isCreative
    }

    @JvmStatic
    fun mustDropItem(entityPlayer: ServerPlayer?): Boolean {
        return if (entityPlayer == null) true else !isCreative(entityPlayer)
    }

    // PORT: the wrench key (ServerKeyHandler.WRENCH) and Eln's wrench item return with the key handler and items.
    fun isWrench(stack: ItemStack): Boolean {
        return stack.hoverName.string.lowercase().contains("wrench")
    }

    @JvmStatic
    fun isPlayerUsingWrench(player: Player?): Boolean {
        if (player == null) return false
        val stack = player.mainHandItem
        return !stack.isEmpty && isWrench(stack)
    }

    @JvmStatic
    fun readFromNBT(nbt: CompoundTag, str: String?, inventory: Container) {
        val list = nbt.getList(str ?: "", Tag.TAG_COMPOUND.toInt())
        for (idx in 0 until list.size) {
            val entry = list.getCompound(idx)
            val slot = entry.getByte("Slot").toInt() and 255
            if (slot < inventory.containerSize) {
                inventory.setItem(slot, ItemStack.of(entry))
            }
        }
    }

    @JvmStatic
    fun writeToNBT(nbt: CompoundTag, str: String?, inventory: Container) {
        val list = ListTag()
        for (slot in 0 until inventory.containerSize) {
            val stack = inventory.getItem(slot)
            if (!stack.isEmpty) {
                val entry = CompoundTag()
                entry.putByte("Slot", slot.toByte())
                stack.save(entry)
                list.add(entry)
            }
        }
        nbt.put(str ?: "", list)
    }

    @JvmStatic
    fun setGlColorFromDye(damage: Int) {
        setGlColorFromDye(damage, 1.0f)
    }

    @JvmStatic
    fun setGlColorFromDye(damage: Int, gain: Float) {
        setGlColorFromDye(damage, gain, 0f)
    }

    @JvmStatic
    fun setGlColorFromDye(damage: Int, gain: Float, bias: Float) {
        when (damage) {
            0 -> GL11.glColor4f(0.2f * gain + bias, 0.2f * gain + bias, 0.2f * gain + bias, 1.0f)
            1 -> GL11.glColor4f(1.0f * gain + bias, 0.05f * gain + bias, 0.05f * gain + bias, 1.0f)
            2 -> GL11.glColor4f(0.2f * gain + bias, 0.5f * gain + bias, 0.1f * gain + bias, 1.0f)
            3 -> GL11.glColor4f(0.3f * gain + bias, 0.2f * gain + bias, 0.1f * gain + bias, 1.0f)
            4 -> GL11.glColor4f(0.2f * gain + bias, 0.2f * gain + bias, 1.0f * gain + bias, 1.0f)
            5 -> GL11.glColor4f(0.7f * gain + bias, 0.05f * gain + bias, 1.0f * gain + bias, 1.0f)
            6 -> GL11.glColor4f(0.2f * gain + bias, 0.7f * gain + bias, 0.9f * gain + bias, 1.0f)
            7 -> GL11.glColor4f(0.7f * gain + bias, 0.7f * gain + bias, 0.7f * gain + bias, 1.0f)
            8 -> GL11.glColor4f(0.4f * gain + bias, 0.4f * gain + bias, 0.4f * gain + bias, 1.0f)
            9 -> GL11.glColor4f(1.0f * gain + bias, 0.5f * gain + bias, 0.5f * gain + bias, 1.0f)
            10 -> GL11.glColor4f(0.05f * gain + bias, 1.0f * gain + bias, 0.05f * gain + bias, 1.0f)
            11 -> GL11.glColor4f(0.9f * gain + bias, 0.8f * gain + bias, 0.1f * gain + bias, 1.0f)
            12 -> GL11.glColor4f(0.4f * gain + bias, 0.5f * gain + bias, 1.0f * gain + bias, 1.0f)
            13 -> GL11.glColor4f(0.9f * gain + bias, 0.3f * gain + bias, 0.9f * gain + bias, 1.0f)
            14 -> GL11.glColor4f(1.0f * gain + bias, 0.6f * gain + bias, 0.3f * gain + bias, 1.0f)
            15 -> GL11.glColor4f(1.0f * gain + bias, 1.0f * gain + bias, 1.0f * gain + bias, 1.0f)
            else -> GL11.glColor4f(0.05f * gain + bias, 0.05f * gain + bias, 0.05f * gain + bias, 1.0f)
        }
    }

    @JvmStatic
    fun setGlColorFromLamp(colorIdx: Int) {
        when (colorIdx) {
            15 -> GL11.glColor3f(1.0f, 1.0f, 1.0f)
            0 -> GL11.glColor3f(0.25f, 0.25f, 0.25f)
            1 -> GL11.glColor3f(1.0f, 0.5f, 0.5f)
            2 -> GL11.glColor3f(0.5f, 1.0f, 0.5f)
            3 -> GL11.glColor3f(0.5647f, 0.36f, 0.36f)
            4 -> GL11.glColor3f(0.5f, 0.5f, 1.0f)
            5 -> GL11.glColor3f(0.78125f, 0.46666f, 1.0f)
            6 -> GL11.glColor3f(0.5f, 1.0f, 1.0f)
            7 -> GL11.glColor3f(0.75f, 0.75f, 0.75f)
            8 -> GL11.glColor3f(0.5f, 0.5f, 0.5f)
            9 -> GL11.glColor3f(1.0f, 0.5f, 0.65882f)
            10 -> GL11.glColor3f(0.75f, 1.0f, 0.5f)
            11 -> GL11.glColor3f(1.0f, 1.0f, 0.5f)
            12 -> GL11.glColor3f(0.5f, 0.75f, 1.0f)
            13 -> GL11.glColor3f(1.0f, 0.5f, 1.0f)
            14 -> GL11.glColor3f(1.0f, 0.80f, 0.5f)
            else -> GL11.glColor3f(1.0f, 1.0f, 1.0f)
        }
    }


    @JvmStatic
    fun traceRay(w: Level, posX: Double, posY: Double, posZ: Double, targetX: Double, targetY: Double, targetZ: Double, weight: TraceRayWeight): Float {
        val posXint = posX.roundToInt()
        val posYint = posY.roundToInt()
        val posZint = posZ.roundToInt()
        var x = (posX - posXint).toFloat()
        var y = (posY - posYint).toFloat()
        var z = (posZ - posZint).toFloat()
        var vx = (targetX - posX).toFloat()
        var vy = (targetY - posY).toFloat()
        var vz = (targetZ - posZ).toFloat()
        val rangeMax = sqrt((vx * vx + vy * vy + vz * vz).toDouble()).toFloat()
        val normInv = 1f / rangeMax
        vx *= normInv
        vy *= normInv
        vz *= normInv
        if (vx == 0f) vx += 0.0001f
        if (vy == 0f) vy += 0.0001f
        if (vz == 0f) vz += 0.0001f
        val vxInv = 1f / vx
        val vyInv = 1f / vy
        val vzInv = 1f / vz
        var stackRed = 0f
        var d = 0f
        while (d < rangeMax) {
            val xFloor = Mth.floor(x).toFloat()
            val yFloor = Mth.floor(y).toFloat()
            val zFloor = Mth.floor(z).toFloat()
            var dx = x - xFloor
            var dy = y - yFloor
            var dz = z - zFloor
            dx = if (vx > 0) (1 - dx) * vxInv else -dx * vxInv
            dy = if (vy > 0) (1 - dy) * vyInv else -dy * vyInv
            dz = if (vz > 0) (1 - dz) * vzInv else -dz * vzInv
            val dBest = dx.coerceAtMost(dy).coerceAtMost(dz) + 0.01f
            val xInt = xFloor.toInt()
            val yInt = yFloor.toInt()
            val zInt = zFloor.toInt()
            val blockPos = BlockPos(xInt + posXint, yInt + posYint, zInt + posZint)
            val block = if (w.isLoaded(blockPos)) w.getBlockState(blockPos) else null
            var dToStack: Float = if (d + dBest < rangeMax) dBest else {
                rangeMax - d
            }
            stackRed += weight.getWeight(block) * dToStack
            x += vx * dBest
            y += vy * dBest
            z += vz * dBest
            d += dBest
        }
        return stackRed
    }

    @JvmStatic
    fun getLength(x: Double, y: Double, z: Double, tx: Double, ty: Double, tz: Double): Double {
        val dx: Double = tx - x
        val dy: Double = ty - y
        val dz: Double = tz - z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    // 1.20.1 weights block states; 1.7.10 weighted blocks.
    interface TraceRayWeight {
        fun getWeight(block: BlockState?): Float
    }

    class TraceRayWeightOpaque : TraceRayWeight {
        override fun getWeight(block: BlockState?): Float {
            if (block == null) return 0f
            return if (block.canOcclude()) 1f else 0f
        }
    }

    @JvmStatic
    fun renderSubSystemWaila(subSystem: SubSystem?): String {
        return if (subSystem != null) {
            val subSystemSize = subSystem.component.size
            val textColor: String = if (subSystemSize <= 8) {
                "§a"
            } else if (subSystemSize <= 15) {
                "§6"
            } else {
                "§c"
            }
            textColor + subSystemSize
        } else {
            "§cnull SubSystem"
        }
    }

    /**
     * 1.20.1 resource paths must match [a-z0-9/._-]; the build copies the 1.7.10 assets under this same mapping
     * (see processResources in build.gradle), so code must map names the same way before making a ResourceLocation.
     */
    @JvmStatic
    fun resourcePath(path: String): String = path.lowercase().replace(Regex("[^a-z0-9/._-]"), "_")

    // "namespace:path" (namespace defaults to minecraft) with the path mapped by [resourcePath].
    @JvmStatic
    fun resourceLocation(id: String): ResourceLocation {
        val split = id.indexOf(':')
        return if (split < 0) ResourceLocation("minecraft", resourcePath(id))
        else ResourceLocation(id.substring(0, split).lowercase(), resourcePath(id.substring(split + 1)))
    }

    @JvmStatic
    fun fatal() {
        try {
            throw Exception()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun notifyNeighbor(t: BlockEntity) {
        val level = t.level ?: return
        for (direction in net.minecraft.core.Direction.values()) {
            (level.getBlockEntity(t.blockPos.relative(direction)) as? ITileEntitySpawnClient)?.tileEntityNeighborSpawn()
        }
    }

    @JvmStatic
    fun sendPacketToClient(bos: ByteArrayOutputStream, player: ServerPlayer) {
        ElnNetwork.sendToPlayer(bos.toByteArray(), player)
    }

    // 1.7.10 packets wrote the dimension as one byte; 1.20.1 dimensions are ids, so packets carry the id string.
    @JvmStatic
    fun writeDimension(stream: DataOutputStream, dimension: ResourceKey<Level>) {
        stream.writeUTF(dimension.location().toString())
    }

    @JvmStatic
    fun readDimension(stream: DataInputStream): ResourceKey<Level> {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation(stream.readUTF()))
    }

    @JvmStatic
    fun addChatMessage(entityPlayer: Player, string: String?) {
        entityPlayer.sendSystemMessage(Component.literal(string ?: ""))
    }

    @JvmStatic
    fun getTags(nbt: CompoundTag): List<CompoundTag> {
        return nbt.allKeys.map { nbt.getCompound(it) }
    }

    @JvmStatic
    fun newNbtTagCompund(nbt: CompoundTag?, string: String): CompoundTag {
        val cmp = CompoundTag()
        nbt?.put(string, cmp)
        return cmp
    }

    // 1.20.1 resolves the save folder directly instead of building "saves/<name>/" by hand.
    fun getMapFile(name: String): File {
        return ServerLifecycleHooks.getCurrentServer().getWorldPath(LevelResource.ROOT).resolve(name).toFile()
    }

    @JvmStatic
    fun mapFileExists(name: String): Boolean {
        return getMapFile(name).exists()
    }

    // 1.7.10 wrote the item id and damage; 1.20.1 items carry their descriptor, so the (session-stable) raw id is enough.
    @JvmStatic
    @Throws(IOException::class)
    fun serialiseItemStack(stream: DataOutputStream, stack: ItemStack?) {
        stream.writeInt(if (stack == null || stack.isEmpty) -1 else Item.getId(stack.item))
    }

    @JvmStatic
    @Throws(IOException::class)
    fun unserialiseItemStack(stream: DataInputStream): ItemStack? {
        val id = stream.readInt()
        return if (id == -1) null else ItemStack(Item.byId(id))
    }

    // The descriptor behind an Eln item stack, or the item itself for other items.
    @JvmStatic
    fun getItemObject(stack: ItemStack?): Any? {
        if (stack == null || stack.isEmpty) return null
        val i = stack.item
        if (i is GenericItem) return i.descriptor
        if (i is IDescriptorItem) return i.descriptor
        return i
    }
}
