package mods.eln.sound

import mods.eln.client.ClientProxy
import mods.eln.misc.Utils.TraceRayWeightOpaque
import mods.eln.misc.Utils.traceRay
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import mods.eln.misc.Utils
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import kotlin.math.pow
import kotlin.math.sqrt

object SoundClient {
    fun play(p: SoundCommand) {
        val player = Minecraft.getInstance().player ?: return
        if (p.world!!.dimension() != player.level().dimension()) return
        val distance = sqrt((p.x - player.x).pow(2.0) + (p.y - player.y).pow(2.0) + (p.z - player.z).pow(2.0))
        if (distance >= p.rangeMax) return
        var distanceFactor = 1f
        if (distance > p.rangeNominal) {
            distanceFactor = ((p.rangeMax - distance) / (p.rangeMax - p.rangeNominal)).toFloat()
        }
        val blockFactor = traceRay(p.world!!, player.x, player.y, player.z, p.x, p.y, p.z, TraceRayWeightOpaque()) * p.blockFactor
        // 1.7.10 split some tracks into frequency bands (SoundLoader.getTrackCount); every track has one band now.
        val temp = 1.0f / (1 + blockFactor)
        p.volume *= temp.pow(2.0f)
        p.volume *= distanceFactor
        if (p.volume <= 0) return
        // Played 2 blocks from the player towards the source, as 1.7.10 did, so its own attenuation stays out of the way.
        val sound = SimpleSoundInstance(
            SoundEvent.createVariableRangeEvent(Utils.resourceLocation(p.track!!)), SoundSource.BLOCKS, p.volume, p.pitch,
            SoundInstance.createUnseededRandom(),
            player.x + 2 * (p.x - player.x) / distance,
            player.y + 2 * (p.y - player.y) / distance,
            player.z + 2 * (p.z - player.z) / distance
        )
        Minecraft.getInstance().soundManager.play(sound)
        if (p.uuid.isNotEmpty()) ClientProxy.uuidManager.add(p.uuid, SoundClientEntity(sound))
    }
}
