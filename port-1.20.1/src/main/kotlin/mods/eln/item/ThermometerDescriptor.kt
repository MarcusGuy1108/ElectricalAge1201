package mods.eln.item

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

        val climateAtClick = BiomeClimateService.sample(world, x, y, z)
        val biomeTempC = climateAtClick.temperatureCelsius
        val biomeTempF = biomeTempC * 9.0 / 5.0 + 32.0
        // PORT: room temperatures (RoomThermalManager) return with the room thermal system; until then the biome reading.
        val message = buildString {
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
        Utils.addChatMessage(player, message)
        return false
    }
}
