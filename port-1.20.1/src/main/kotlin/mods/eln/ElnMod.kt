package mods.eln

import mods.eln.client.ElnClient
import mods.eln.datagen.ElnDataGen
import mods.eln.metrics.MetricsSubsystem
import mods.eln.generic.GenericItemBlockUsingDamage
import mods.eln.network.ElnNetwork
import mods.eln.node.six.SixNodeItem
import mods.eln.node.six.SixNodeBlock
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeBlockRegistration
import mods.eln.registration.SixNodeRegistration
import mods.eln.registry.ElnRegistries
import mods.eln.node.NodeManager
import mods.eln.node.NodeServer
import mods.eln.server.ServerEventListener
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.event.server.ServerAboutToStartEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.loading.FMLEnvironment
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(Eln.MODID)
object ElnMod {
    init {
        Eln.loadConfig()
        ElnNetwork.register()
        Eln.createCreativeTabs()
        SixNodeBlockRegistration.BLOCK
        NodeManager.registerUuid(SixNodeBlock.NODE_UUID, SixNode::class.java)
        Eln.sixNodeItem = GenericItemBlockUsingDamage { SixNodeItem(it) }
        Eln.obj.loadAllElnModels()
        SixNodeRegistration.registerSix()
        ElnRegistries.register(MOD_BUS)
        MOD_BUS.addListener(ElnDataGen::gatherData)
        if (FMLEnvironment.dist == Dist.CLIENT) ElnClient.init(MOD_BUS)
        Eln.nodeManager = NodeManager("caca")
        Eln.nodeServer = NodeServer()
        Eln.serverEventListener = ServerEventListener()

        val bus = MinecraftForge.EVENT_BUS
        bus.addListener { _: ServerAboutToStartEvent -> Eln.simulator.init() }
        bus.addListener { event: TickEvent.ServerTickEvent ->
            Eln.simulator.tick(event)
            Eln.nodeServer.tick(event)
        }
        bus.addListener(Eln.serverEventListener::onWorldLoad)
        bus.addListener(Eln.serverEventListener::onWorldSave)
        bus.addListener(Eln.serverEventListener::onWorldUnload)
        bus.addListener { _: ServerStoppedEvent ->
            Eln.nodeManager.clear()
            Eln.serverEventListener.clear()
            Eln.nodeServer.stop()
            Eln.simulator.stop()
            MetricsSubsystem.shutdown()
        }
        Eln.logger.info("Electrical Age loading")
    }
}
