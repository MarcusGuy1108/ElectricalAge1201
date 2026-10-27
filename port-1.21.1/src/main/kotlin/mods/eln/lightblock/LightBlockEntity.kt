package mods.eln.lightblock

import mods.eln.misc.Coordinate
import mods.eln.misc.INBTTReady
import mods.eln.misc.Utils
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class LightBlockEntity(pos: BlockPos, state: BlockState) : BlockEntity(LightBlock.ENTITY.get(), pos, state) {

    companion object {
        @JvmField
        val observers: MutableList<LightBlockObserver> = mutableListOf()

        @JvmStatic
        fun addLight(w: Level, x: Int, y: Int, z: Int, requestedLight: Int, timeout: Int) {
            // 1.7.10 clamped light levels to 15 itself; some callers ask for more (the turret's muzzle flash asks 25).
            val light = requestedLight.coerceIn(0, 15)
            val pos = BlockPos(x, y, z)
            val state = w.getBlockState(pos)

            if (!state.`is`(LightBlock.BLOCK.get())) {
                if (!state.isAir) return
                w.setBlock(pos, LightBlock.BLOCK.get().defaultBlockState().setValue(LightBlock.LIGHT, light), Block.UPDATE_CLIENTS)
            }

            val t = w.getBlockEntity(pos)

            if (t is LightBlockEntity) t.addLight(light, timeout)
            else Utils.println("Error in setting light at %d %d %d", x, y, z)
        }

        @JvmStatic
        fun addLight(coord: Coordinate, light: Int, timeout: Int) {
            addLight(coord.world(), coord.x, coord.y, coord.z, light, timeout)
        }

        // 1.7.10 updateEntity(), run every server tick.
        fun tick(level: Level, pos: BlockPos, state: BlockState, entity: LightBlockEntity) {
            if (entity.lightList.isEmpty()) {
                level.removeBlock(pos, false)
                Utils.println("Destroy light at %d %d %d", pos.x, pos.y, pos.z)
                return
            }

            var light = 0
            val iterator: MutableIterator<LightHandle> = entity.lightList.iterator()

            while (iterator.hasNext()) {
                val l = iterator.next()
                if (light < l.value) light = l.value
                l.timeout--
                if (l.timeout <= 0) iterator.remove()
            }

            if (light != state.getValue(LightBlock.LIGHT)) {
                level.setBlock(pos, state.setValue(LightBlock.LIGHT, light), Block.UPDATE_CLIENTS)
            }
        }
    }

    interface LightBlockObserver {

        fun lightBlockDestructor(coord: Coordinate)

    }

    private val lightList: MutableList<LightHandle> = mutableListOf()

    private fun addLight(light: Int, timeout: Int) {
        lightList.add(LightHandle(light, timeout))
    }

    internal class LightHandle(var value: Int = 0, var timeout: Int = 0) : INBTTReady {

        override fun readFromNBT(nbt: CompoundTag, str: String) {
            value = nbt.getInt(str + "value")
            timeout = nbt.getInt(str + "timeout")
        }

        override fun writeToNBT(nbt: CompoundTag, str: String) {
            nbt.putInt(str + "value", value)
            nbt.putInt(str + "timeout", timeout)
        }

    }

}
