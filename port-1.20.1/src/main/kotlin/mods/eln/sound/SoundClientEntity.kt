package mods.eln.sound

import mods.eln.client.IUuidEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SoundInstance

// Keeps a played sound stoppable by the UUID of the element that played it.
class SoundClientEntity(private val sound: SoundInstance) : IUuidEntity {
    private var borneTimer: Int = 5

    override fun isAlive(): Boolean {
        if (borneTimer != 0) {
            borneTimer--
            return true
        }
        return Minecraft.getInstance().soundManager.isActive(sound)
    }

    override fun kill() {
        Minecraft.getInstance().soundManager.stop(sound)
    }
}
