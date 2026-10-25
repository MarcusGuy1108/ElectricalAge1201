package mods.eln

import mods.eln.metrics.MetricsSubsystem
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.server.ServerAboutToStartEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import net.minecraftforge.fml.common.Mod

@Mod(Eln.MODID)
object ElnMod {
    init {
        Eln.loadConfig()
        MinecraftForge.EVENT_BUS.addListener { _: ServerAboutToStartEvent -> Eln.simulator.init() }
        MinecraftForge.EVENT_BUS.addListener { event: TickEvent.ServerTickEvent -> Eln.simulator.tick(event) }
        MinecraftForge.EVENT_BUS.addListener { _: ServerStoppedEvent ->
            Eln.simulator.stop()
            MetricsSubsystem.shutdown()
        }
        Eln.logger.info("Electrical Age loading")
    }
}
