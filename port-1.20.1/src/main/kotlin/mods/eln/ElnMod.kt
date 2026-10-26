package mods.eln

import mods.eln.item.electricalinterface.ItemEnergyInventoryProcess
import mods.eln.craft.CraftingRecipes
import mods.eln.ore.ElnOre
import mods.eln.item.CopperGear
import mods.eln.debug.SmokeCommands
import mods.eln.item.electricalitem.ElectricalArmor
import net.minecraftforge.event.entity.living.LivingHurtEvent
import mods.eln.block.ArcBlock
import net.minecraft.world.item.Item
import mods.eln.generic.GenericItem
import mods.eln.ghost.GhostBlock
import mods.eln.lightblock.LightBlockEntity
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement
import mods.eln.sixnode.lampsupply.LampSupplyElement
import mods.eln.misc.WindProcess
import mods.eln.registration.TransparentNodeRegistration
import mods.eln.node.transparent.TransparentNodeItem
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeBlock
import mods.eln.registration.ItemRegistration
import mods.eln.item.lampitem.LampLists
import mods.eln.lightblock.LightBlock
import mods.eln.generic.GenericItemUsingDamage
import mods.eln.gui.ElnMenus
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
        TransparentNodeBlock.BLOCK
        NodeManager.registerUuid(TransparentNodeBlock.NODE_UUID, TransparentNode::class.java)
        Eln.sixNodeItem = GenericItemBlockUsingDamage { SixNodeItem(it) }
        Eln.transparentNodeItem = GenericItemBlockUsingDamage { TransparentNodeItem(it) }
        Eln.sharedItem = GenericItemUsingDamage()
        Eln.sharedItemStackOne = GenericItemUsingDamage { GenericItem(it, Item.Properties().stacksTo(1)) }
        LightBlock.BLOCK
        GhostBlock.BLOCK
        ElnOre.register(MOD_BUS)
        CopperGear.register()
        ElectricalArmor.register()
        ArcBlock.register()
        Eln.obj.loadAllElnModels()
        LampLists.translateLampTypes() // Lamp sockets list the translated lamp types, so before registration.
        // Same order as 1.7.10: items such as heating corps refer to the cable descriptors.
        SixNodeRegistration.registerSix()
        TransparentNodeRegistration.registerTransparent()
        ItemRegistration.registerItem()
        ElnRegistries.register(MOD_BUS)
        ElnMenus.MENUS.register(MOD_BUS)
        MOD_BUS.addListener(ElnDataGen::gatherData)
        if (FMLEnvironment.dist == Dist.CLIENT) ElnClient.init(MOD_BUS)
        Eln.nodeManager = NodeManager("caca")
        Eln.nodeServer = NodeServer()
        Eln.serverEventListener = ServerEventListener()

        val bus = MinecraftForge.EVENT_BUS
        bus.addListener { event: LivingHurtEvent -> ElectricalArmor.absorbDamage(event) }
        if (!FMLEnvironment.production) bus.addListener(SmokeCommands::registerServer)
        bus.addListener { _: ServerAboutToStartEvent ->
            LightBlockEntity.observers.clear()
            WirelessSignalTxElement.channelMap.clear()
            LampSupplyElement.channelMap.clear()
            Eln.simulator.init()
            CraftingRecipes.registerMachineRecipes()
            // shortcut: the wind state starts fresh each server start; 1.7.10 kept it in a SaveConfig world data entry.
            Eln.wind = WindProcess()
            Eln.simulator.addSlowProcess(Eln.wind)
            // Charges and drains the electrical items players carry (portable batteries, tools, flashlights).
            Eln.itemEnergyInventoryProcess = ItemEnergyInventoryProcess()
            Eln.simulator.addSlowProcess(Eln.itemEnergyInventoryProcess)
        }
        bus.addListener { event: TickEvent.ServerTickEvent ->
            Eln.simulator.tick(event)
            Eln.nodeServer.tick(event)
        }
        bus.addListener(Eln.serverEventListener::tick)
        bus.addListener(Eln.serverEventListener::onEntityJoin)
        bus.addListener(Eln.serverEventListener::onWorldLoad)
        bus.addListener(Eln.serverEventListener::onWorldSave)
        bus.addListener(Eln.serverEventListener::onWorldUnload)
        bus.addListener { _: ServerStoppedEvent ->
            Eln.nodeManager.clear()
            Eln.ghostManager.clear()
            Eln.serverEventListener.clear()
            Eln.nodeServer.stop()
            Eln.simulator.stop()
            MetricsSubsystem.shutdown()
        }
        Eln.logger.info("Electrical Age loading")
    }
}
