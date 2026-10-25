package mods.eln.sound

import mods.eln.misc.Coordinate
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource

/**
 * Machine sound that loops while [active]. Subclasses override getVolume/getPitch, as they did for 1.7.10 ITickableSound.
 */
abstract class LoopedSound(
    val sample: String,
    val coord: Coordinate,
    attentuationType: SoundInstance.Attenuation = SoundInstance.Attenuation.LINEAR
) : AbstractTickableSoundInstance(
    SoundEvent.createVariableRangeEvent(ResourceLocation(sample)), SoundSource.BLOCKS, SoundInstance.createUnseededRandom()
) {
    var active = true

    init {
        x = coord.x + 0.5
        y = coord.y + 0.5
        z = coord.z + 0.5
        looping = true
        delay = 0
        attenuation = attentuationType
    }

    override fun getPitch() = 1f
    override fun getVolume() = 1f
    override fun isStopped() = !active
    override fun tick() {}
}
