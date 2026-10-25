package mods.eln.misc

import mods.eln.Eln
import mods.eln.network.ElnNetwork
import mods.eln.node.ITileEntitySpawnClient
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.storage.LevelResource
import net.minecraftforge.server.ServerLifecycleHooks
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.Random

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
}
