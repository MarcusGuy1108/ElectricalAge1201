package mods.eln.server

import mods.eln.environment.RoomThermalManager
import mods.eln.item.electricalitem.TreeCapitation
import mods.eln.Eln
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.event.TickEvent
import net.minecraft.world.entity.LightningBolt
import mods.eln.misc.Coordinate
import mods.eln.node.NodeManager
import mods.eln.misc.Utils
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.storage.LevelResource
import net.minecraftforge.event.level.LevelEvent
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Loads and saves Electrical Age world data (nodes and friends) next to each dimension's own data folder, with the
 * same temp file + backup rotation 1.7.10 used. The handlers are registered on the Forge event bus by ElnMod.
 */
class ServerEventListener {
    // Lightning bolts that struck during the last tick; wireless signals lose range near them.
    private var lightningListNext = ArrayList<LightningBolt>()
    private var lightningList = ArrayList<LightningBolt>()

    fun tick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        TreeCapitation.process(0.05)
        lightningList = lightningListNext
        lightningListNext = ArrayList()
    }

    fun onEntityJoin(event: EntityJoinLevelEvent) {
        val bolt = event.entity as? LightningBolt ?: return
        if (!event.level.isClientSide) lightningListNext.add(bolt)
    }

    fun getLightningClosestTo(c: Coordinate): Double {
        var best = 10000000.0
        for (l in lightningList) {
            if (c.dimension != l.level().dimension()) continue
            val d = Math.sqrt(l.distanceToSqr(c.x.toDouble(), c.y.toDouble(), c.z.toDouble()))
            if (d < best) best = d
        }
        return best
    }

    fun clear() {
        lightningList.clear()
        lightningListNext.clear()
    }

    private val loadedWorlds = HashSet<ResourceKey<Level>>()

    fun onWorldLoad(e: LevelEvent.Load) {
        val level = e.level as? ServerLevel ?: return
        loadedWorlds.add(level.dimension())
        val fileNames = FileNames(level)
        val dimension = level.dimension()
        if (!Files.exists(fileNames.worldSave) && !Files.exists(fileNames.backupSave)) return
        try {
            readSave(fileNames.worldSave, dimension)
        } catch (ex: Exception) {
            try {
                ex.printStackTrace()
                println("Using BACKUP Electrical Age save: " + fileNames.backupSave)
                readSave(fileNames.backupSave, dimension)
            } catch (ex2: Exception) {
                ex2.printStackTrace()
                println("Failed to read backup save!")
            }
        }
    }

    @Throws(IOException::class)
    private fun readSave(worldSave: Path, dimension: ResourceKey<Level>) {
        val inputStream = ByteArrayInputStream(Files.readAllBytes(worldSave))
        val nbt = NbtIo.readCompressed(inputStream)
        readFromEaWorldNBT(nbt, dimension)
    }

    fun onWorldUnload(e: LevelEvent.Unload) {
        val level = e.level as? ServerLevel ?: return
        loadedWorlds.remove(level.dimension())
        try {
            NodeManager.instance!!.unload(level.dimension())
            Eln.ghostManager.unload(level.dimension())
            RoomThermalManager.unloadDimension(level.dimension())
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun onWorldSave(e: LevelEvent.Save) {
        val level = e.level as? ServerLevel ?: return
        if (!loadedWorlds.contains(level.dimension())) {
            return
        }
        try {
            val nbt = CompoundTag()
            writeToEaWorldNBT(nbt, level.dimension())
            val fileNames = FileNames(level)

            // Write a new save to a temporary file.
            val bytes = ByteArrayOutputStream(512 * 1024)
            NbtIo.writeCompressed(nbt, bytes)
            Files.createDirectories(fileNames.worldSave.parent)
            Files.write(fileNames.tempSave, bytes.toByteArray())

            // Replace backup save with old save, and old save with new one.
            if (Files.exists(fileNames.worldSave)) replaceFile(fileNames.worldSave, fileNames.backupSave)
            replaceFile(fileNames.tempSave, fileNames.worldSave)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun replaceFile(from: Path, to: Path) {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    // 1.20.1 gives every dimension its own storage folder, so the file no longer needs the dimension in its name.
    private class FileNames(level: ServerLevel) {
        val worldSave: Path
        val tempSave: Path
        val backupSave: Path

        init {
            val root = level.server.getWorldPath(LevelResource.ROOT)
            val saveName = DimensionType.getStorageFolder(level.dimension(), root).resolve("data/electricalAgeWorld.dat")
            worldSave = saveName
            tempSave = saveName.resolveSibling("electricalAgeWorld.dat.tmp")
            backupSave = saveName.resolveSibling("electricalAgeWorld.dat.bak")
        }
    }

    companion object {
        fun readFromEaWorldNBT(nbt: CompoundTag, dim: ResourceKey<Level>) {
            try {
                NodeManager.instance!!.loadFromNbt(nbt.getCompound("nodes"), dim)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                Eln.ghostManager.loadFromNBT(nbt.getCompound("ghost"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                RoomThermalManager.readFromNbt(nbt, dim)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // PORT: MqttManager world data returns with MQTT.
        }

        fun writeToEaWorldNBT(nbt: CompoundTag?, dim: ResourceKey<Level>) {
            try {
                NodeManager.instance!!.saveToNbt(Utils.newNbtTagCompund(nbt, "nodes"), dim)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                Eln.ghostManager.saveToNBT(Utils.newNbtTagCompund(nbt, "ghost"), dim)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                RoomThermalManager.writeToNbt(nbt!!, dim)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // PORT: MqttManager world data returns with MQTT.
        }
    }
}
