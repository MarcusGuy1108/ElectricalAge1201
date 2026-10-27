package mods.eln.item

import mods.eln.environment.RoomThermalManager
import mods.eln.compat.*
import mods.eln.environment.BiomeClimateService
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N
import mods.eln.misc.Utils
import mods.eln.node.NodeBlock
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class ThermometerDescriptor(name: String) : GenericItemUsingDamageDescriptor(name) {
    override fun onItemUse(
        stack: ItemStack?,
        player: Player?,
        world: Level?,
        x: Int,
        y: Int,
        z: Int,
        side: Int,
        vx: Float,
        vy: Float,
        vz: Float
    ): Boolean {
        if (player == null || world == null || world.isRemote) {
            return false
        }

        val block = world.getBlock(x, y, z)
        if (block is NodeBlock) {
            return false
        }

        val playerPos = player.blockPosition()
        val climateAtClick = BiomeClimateService.sample(world, x, y, z)
        val climateAtPlayer = BiomeClimateService.sample(world, playerPos.x, playerPos.y, playerPos.z)
        val biomeTempC = climateAtClick.temperatureCelsius
        val biomeTempF = biomeTempC * 9.0 / 5.0 + 32.0
        val room = RoomThermalManager.getRoomAt(world, playerPos.x, playerPos.y, playerPos.z)
        val message = buildString {
            if (room != null) {
                val absoluteRoomTempC = climateAtPlayer.temperatureCelsius + room.temperatureCelsius
                val roomTempF = absoluteRoomTempC * 9.0 / 5.0 + 32.0
                append(I18N.tr("Room T:"))
                append(" ")
                append(Utils.plotCelsius("", absoluteRoomTempC))
                append("(")
                append(Utils.plotValue(roomTempF, "°F "))
                append(")")
            } else {
                append(I18N.tr("Biome T:"))
                append(" ")
                append(Utils.plotCelsius("", biomeTempC))
                append("(")
                append(Utils.plotValue(biomeTempF, "°F "))
                append(")")
                append(" ")
                append(I18N.tr("RH:"))
                append(String.format("%.0f%%", climateAtClick.relativeHumidityPercent))
            }
        }
        Utils.addChatMessage(player, message)
        return false
    }
}
