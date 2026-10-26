package mods.eln.sim.process.destruct

import mods.eln.node.six.SixNodeElement
import net.minecraft.world.level.block.Blocks

class CableMelt(private val element: SixNodeElement) : IDestructible {
    override fun destructImpl() {
        val coordinate = element.coordinate ?: return
        // PORT: 1.7.10 dropped a wire scrap here; it returns with WireScrapDescriptor.
        coordinate.world().setBlockAndUpdate(coordinate.blockPos, Blocks.AIR.defaultBlockState())
    }

    override fun describe(): String {
        val coordinate = element.coordinate
        return "${element.javaClass.simpleName} (${coordinate?.toString() ?: "unknown"})"
    }
}
