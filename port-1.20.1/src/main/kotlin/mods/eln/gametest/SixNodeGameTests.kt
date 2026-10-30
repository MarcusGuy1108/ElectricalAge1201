package mods.eln.gametest

import mods.eln.falstad.FalstadImporter
import mods.eln.environment.RoomThermalManager
import mods.eln.sixnode.electricalbreaker.ElectricalBreakerElement
import mods.eln.sixnode.logicgate.LogicGateElement
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import mods.eln.sixnode.electricalswitch.ElectricalSwitchElement
import mods.eln.misc.LRDU
import net.minecraft.world.InteractionHand
import java.io.DataOutputStream
import java.io.DataInputStream
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import net.minecraft.world.item.ItemStack
import mods.eln.sixnode.lampsocket.LampSocketRender
import mods.eln.sixnode.wirelesssignal.source.WirelessSignalSourceElement
import mods.eln.item.ElectricalFuseDescriptor
import mods.eln.sixnode.lampsocket.LampSocketDescriptor
import mods.eln.item.lampitem.LampItemSlot
import mods.eln.misc.Utils
import net.minecraft.world.SimpleContainer
import mods.eln.sixnode.lampsocket.LampSocketElement
import mods.eln.sixnode.lampsocket.LampSocketContainer
import mods.eln.sixnode.electricalsource.ElectricalSourceElement
import mods.eln.node.NodeBlock
import mods.eln.Eln
import mods.eln.generic.GenericItemUsingDamageDescriptor
import net.minecraftforge.common.util.FakePlayerFactory
import mods.eln.misc.Direction
import mods.eln.node.NodeManager
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeEntity
import mods.eln.node.six.SixNodeItem
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sixnode.electricalcable.ElectricalCableElement
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import kotlin.math.abs

@GameTestHolder(Eln.MODID)
@PrefixGameTestTemplate(false)
object SixNodeGameTests {
    private fun placeCable(helper: GameTestHelper, relative: BlockPos): SixNode {
        return placeOnFloor(helper, relative, Eln.lowVoltageCableDescriptor!!.newItemStack())
    }

    internal fun placeOnFloor(helper: GameTestHelper, relative: BlockPos, stack: ItemStack): SixNode {
        helper.setBlock(relative.below(), Blocks.STONE)
        val item = stack.item as SixNodeItem
        val pos = helper.absolutePos(relative)
        helper.assertTrue(item.placeBlockAt(stack, helper.makeMockPlayer(), helper.level, pos, Direction.YN), "cable not placed at $relative")
        val entity = helper.level.getBlockEntity(pos) as? SixNodeEntity
        helper.assertTrue(entity != null, "no six-node block entity at $relative")
        return entity!!.node as SixNode
    }

    // Two low voltage cables on stone, fed 10 V at one end. The load hangs off a separate (machine-like) load, as in game:
    // a grounded resistor directly on a cable load would be merged into a line with a ground end, which Eln never builds.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun placedCablesConnectAndConduct(helper: GameTestHelper) {
        val first = placeCable(helper, BlockPos(0, 1, 1))
        val second = placeCable(helper, BlockPos(1, 1, 1))
        val firstCable = first.getElement(Direction.YN) as? ElectricalCableElement
        val secondCable = second.getElement(Direction.YN) as? ElectricalCableElement
        helper.assertTrue(firstCable != null && secondCable != null, "cable elements missing")
        helper.assertTrue(second.nodeConnectionList.isNotEmpty(), "adjacent cables did not connect")

        val machine = ElectricalLoad()
        val source = VoltageSource("source", firstCable!!.electricalLoad, null).setVoltage(10.0)
        val feed = Resistor(secondCable!!.electricalLoad, machine).setResistance(10.0)
        val drain = Resistor(machine, null).setResistance(10.0)
        Eln.simulator.addElectricalLoad(machine)
        for (component in listOf(source, feed, drain)) Eln.simulator.addElectricalComponent(component)
        try {
            Eln.simulator.mna.step()
            val voltage = machine.voltage
            // The cables are milliohms, so the two 10 ohm resistors split the 10 V in half.
            helper.assertTrue(abs(voltage - 5.0) < 0.05, "machine load at $voltage V")
            helper.succeed()
        } finally {
            for (component in listOf(source, feed, drain)) Eln.simulator.removeElectricalComponent(component)
            Eln.simulator.removeElectricalLoad(machine)
        }
    }

    // Nodes are rebuilt from the world save by their uuid; a missing registration silently dropped every cable on load.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun cableSurvivesSaveAndLoad(helper: GameTestHelper) {
        val node = placeCable(helper, BlockPos(1, 1, 1))
        val tag = CompoundTag()
        node.writeToNBT(tag)
        val nodeClass = NodeManager.UUIDToClass[node.nodeUuid]
        helper.assertTrue(nodeClass != null, "no node class registered for uuid ${node.nodeUuid}")
        val loaded = nodeClass!!.getConstructor().newInstance() as SixNode
        loaded.readFromNBT(tag)
        helper.assertTrue(loaded.getElement(Direction.YN) is ElectricalCableElement, "cable element not restored")
        helper.succeed()
    }

    // The 1.7.10 lighting circuit: a 12 V source feeds a lamp socket through a cable; the socket's 12 V bulb lights it.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun sourcePowersLampSocket(helper: GameTestHelper) {
        val sourceNode = placeOnFloor(helper, BlockPos(0, 1, 1), Eln.sixNodeItem.getDescriptor(3 shl 6)!!.newItemStack())
        placeCable(helper, BlockPos(1, 1, 1))
        val lampNode = placeOnFloor(helper, BlockPos(2, 1, 1), Eln.sixNodeItem.getDescriptor(64 shl 6)!!.newItemStack())
        val source = sourceNode.getElement(Direction.YN) as ElectricalSourceElement
        val socket = lampNode.getElement(Direction.YN) as LampSocketElement

        // What the source GUI sends when 12 is typed in.
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).apply {
            writeByte(ElectricalSourceElement.setVoltageId)
            writeFloat(12f)
        }
        source.networkUnserialize(DataInputStream(ByteArrayInputStream(bytes.toByteArray())))

        // What the lamp socket GUI does: bulb and cable in their slots, "Powered by cable".
        socket.inventory.setItem(LampSocketContainer.LAMP_SLOT_ID, Eln.sharedItem.getDescriptor(4 shl 6)!!.newItemStack())
        socket.inventory.setItem(LampSocketContainer.CABLE_SLOT_ID, Eln.lowVoltageCableDescriptor!!.newItemStack())
        socket.poweredByLampSupply = false
        socket.inventoryChange(socket.inventory)

        helper.succeedWhen {
            helper.assertTrue(lampNode.lightValue >= LampSocketRender.MIN_LIGHT_ON_VALUE, "lamp light ${lampNode.lightValue}, ${socket.electricalLoad.voltage} V")
            helper.assertTrue(helper.getBlockState(BlockPos(2, 1, 1)).getValue(NodeBlock.LIGHT) == lampNode.lightValue, "block light not synced")
        }
    }

    // Sets up the source -> element -> lamp row of sourcePowersLampSocket with [middle] in the middle; returns the lamp.
    private fun sourceThroughToLamp(helper: GameTestHelper, middle: ItemStack): Pair<SixNode, SixNode> {
        val sourceNode = placeOnFloor(helper, BlockPos(0, 1, 1), Eln.sixNodeItem.getDescriptor(3 shl 6)!!.newItemStack())
        val middleNode = placeOnFloor(helper, BlockPos(1, 1, 1), middle)
        val lampNode = placeOnFloor(helper, BlockPos(2, 1, 1), Eln.sixNodeItem.getDescriptor(64 shl 6)!!.newItemStack())
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).apply {
            writeByte(ElectricalSourceElement.setVoltageId)
            writeFloat(12f)
        }
        (sourceNode.getElement(Direction.YN) as ElectricalSourceElement).networkUnserialize(DataInputStream(ByteArrayInputStream(bytes.toByteArray())))
        val socket = lampNode.getElement(Direction.YN) as LampSocketElement
        socket.inventory.setItem(LampSocketContainer.LAMP_SLOT_ID, Eln.sharedItem.getDescriptor(4 shl 6)!!.newItemStack())
        socket.inventory.setItem(LampSocketContainer.CABLE_SLOT_ID, Eln.lowVoltageCableDescriptor.newItemStack())
        socket.poweredByLampSupply = false
        socket.inventoryChange(socket.inventory)
        return Pair(middleNode, lampNode)
    }

    // A single-conductor utility cable carries the source to the lamp like a classic cable.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun utilityCableCarriesPower(helper: GameTestHelper) {
        val utility = Eln.sixNodeItem.descriptors.filterIsInstance<UtilityCableDescriptor>()
            .first { it.insulated && !it.melted && it.conductorCount == 1 && it.totalConductorAreaMm2 > 2.0 }
        val (_, lampNode) = sourceThroughToLamp(helper, utility.newItemStack())
        helper.succeedWhen {
            helper.assertTrue(lampNode.lightValue >= LampSocketRender.MIN_LIGHT_ON_VALUE, "lamp light ${lampNode.lightValue} through ${utility.name}")
        }
    }

    // A breaker passes current along its front axis once closed, like the switch.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun breakerControlsLamp(helper: GameTestHelper) {
        val (breakerNode, lampNode) = sourceThroughToLamp(helper, Eln.sixNodeItem.getDescriptor(98 shl 6)!!.newItemStack())
        val breaker = breakerNode.getElement(Direction.YN) as ElectricalBreakerElement
        breaker.front = LRDU.values().first { Direction.YN.applyLRDU(it) == Direction.XN }
        breakerNode.reconnect()
        helper.startSequence()
            .thenExecuteAfter(20) { helper.assertTrue(lampNode.lightValue == 0, "lamp lit through an open breaker") }
            .thenExecute { breaker.setSwitchState(true) }
            .thenWaitUntil { helper.assertTrue(lampNode.lightValue >= LampSocketRender.MIN_LIGHT_ON_VALUE, "lamp light ${lampNode.lightValue} with the breaker closed") }
            .thenSucceed()
    }

    // An unconnected NOT gate drives its output high and an unconnected AND gate keeps it low.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun logicGatesDriveTheirOutput(helper: GameTestHelper) {
        val not = placeOnFloor(helper, BlockPos(0, 1, 1), Eln.sixNodeItem.getDescriptor(118 shl 6)!!.newItemStack()).getElement(Direction.YN) as LogicGateElement
        val and = placeOnFloor(helper, BlockPos(2, 1, 1), Eln.sixNodeItem.getDescriptor((118 shl 6) + 1)!!.newItemStack()).getElement(Direction.YN) as LogicGateElement
        helper.succeedWhen {
            val notOut = not.getElectricalLoad(not.front, 0)!!.voltage
            val andOut = and.getElectricalLoad(and.front, 0)!!.voltage
            helper.assertTrue(notOut > Eln.SVU * 0.9 && andOut < Eln.SVU * 0.1, "NOT output $notOut V, AND output $andOut V")
        }
    }

    // A switch only passes current along its front axis, so it is turned to face the source, then closed.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun switchControlsLamp(helper: GameTestHelper) {
        val (switchNode, lampNode) = sourceThroughToLamp(helper, Eln.sixNodeItem.getDescriptor((97 shl 6) + 5)!!.newItemStack())
        val switch = switchNode.getElement(Direction.YN) as ElectricalSwitchElement
        switch.front = LRDU.values().first { Direction.YN.applyLRDU(it) == Direction.XN }
        switchNode.reconnect()
        helper.startSequence()
            .thenExecuteAfter(20) { helper.assertTrue(lampNode.lightValue == 0, "lamp lit through an open switch") }
            .thenExecute { switch.setSwitchState(true) }
            .thenWaitUntil { helper.assertTrue(lampNode.lightValue >= LampSocketRender.MIN_LIGHT_ON_VALUE, "lamp light ${lampNode.lightValue} with the switch closed") }
            .thenSucceed()
    }

    // Right-clicking a node with the multimeter reads it out instead of opening a GUI or doing nothing.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun multimeterReadsCable(helper: GameTestHelper) {
        val cable = placeCable(helper, BlockPos(1, 1, 1))
        val player = helper.makeMockPlayer()
        player.setItemInHand(InteractionHand.MAIN_HAND, Eln.multiMeterElement!!.newItemStack())
        helper.assertTrue(cable.onBlockActivated(player, Direction.YN, 0.5f, 0.1f, 0.5f), "multimeter click not handled")
        helper.assertTrue(cable.multiMeterString(Direction.YN).isNotEmpty(), "empty multimeter reading")
        helper.succeed()
    }

    // Breaking the support block must remove the cable on top of it, as 1.7.10 did.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun cableDropsWhenSupportIsRemoved(helper: GameTestHelper) {
        placeCable(helper, BlockPos(1, 1, 1))
        helper.setBlock(BlockPos(1, 0, 1), Blocks.AIR)
        helper.succeedWhen {
            helper.assertBlockNotPresent(Eln.sixNodeBlock, BlockPos(1, 1, 1))
        }
    }

    // Clicking a node with the config copy tool pastes the tool's settings; shift-clicking copies the node's.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun configCopyToolCopiesSettings(helper: GameTestHelper) {
        val source = placeOnFloor(helper, BlockPos(1, 1, 1), Eln.sixNodeItem.getDescriptor(3 shl 6)!!.newItemStack())
        val player = FakePlayerFactory.getMinecraft(helper.level)
        val tool = Eln.configCopyToolElement!!.newItemStack()
        tool.getOrCreateTag().putDouble("voltage", 50.0)
        player.setItemInHand(InteractionHand.MAIN_HAND, tool)
        player.isShiftKeyDown = false
        source.onBlockActivated(player, Direction.YN, 0f, 0f, 0f)
        val copy = Eln.configCopyToolElement!!.newItemStack()
        player.setItemInHand(InteractionHand.MAIN_HAND, copy)
        player.isShiftKeyDown = true
        source.onBlockActivated(player, Direction.YN, 0f, 0f, 0f)
        player.isShiftKeyDown = false
        helper.assertTrue(copy.tag?.getDouble("voltage") == 50.0, "copied ${copy.tag}")
        helper.succeed()
    }

    // A cable on top of a block and one on its side face connect around the block's edge.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun cablesWrapAroundBlockEdge(helper: GameTestHelper) {
        helper.setBlock(BlockPos(1, 1, 1), Blocks.STONE)
        val top = placeOnFloor(helper, BlockPos(1, 2, 1), Eln.lowVoltageCableDescriptor!!.newItemStack())
        val stack = Eln.lowVoltageCableDescriptor!!.newItemStack()
        val sidePos = helper.absolutePos(BlockPos(2, 1, 1))
        helper.assertTrue((stack.item as SixNodeItem).placeBlockAt(stack, helper.makeMockPlayer(), helper.level, sidePos, Direction.XN), "side cable not placed")
        val side = (helper.level.getBlockEntity(sidePos) as SixNodeEntity).node as SixNode
        helper.assertTrue(top.nodeConnectionList.any { it.N1 === side || it.N2 === side }, "cables did not connect around the edge")
        helper.succeed()
    }

    // A wrench in the off hand and a block in the main hand hide a cable inside that block; breaking it once takes
    // the block back off and leaves the cable.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun wrenchHidesCableInBlock(helper: GameTestHelper) {
        val cable = placeOnFloor(helper, BlockPos(1, 1, 1), Eln.lowVoltageCableDescriptor!!.newItemStack())
        val at = helper.absolutePos(BlockPos(1, 1, 1))
        val player = FakePlayerFactory.get(helper.level, com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "eln_cache_block"))
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Blocks.STONE_BRICKS, 2))
        player.setItemInHand(InteractionHand.OFF_HAND, Eln.wrenchDescriptor!!.newItemStack())
        cable.onBlockActivated(player, Direction.YN, 0.5f, 0.1f, 0.5f)
        helper.assertTrue(cable.sixNodeCacheBlock.`is`(Blocks.STONE_BRICKS), "cache block ${cable.sixNodeCacheBlock}")
        helper.assertTrue(player.mainHandItem.count == 1, "${player.mainHandItem.count} bricks left")
        val state = helper.level.getBlockState(at)
        helper.assertTrue(state.getLightBlock(helper.level, at) == 15, "hidden cable lets light through")
        helper.assertTrue(state.getCollisionShape(helper.level, at).max(net.minecraft.core.Direction.Axis.Y) == 1.0, "hidden cable is not solid")
        helper.assertFalse(Eln.sixNodeBlock.onDestroyedByPlayer(state, helper.level, at, player, true, helper.level.getFluidState(at)), "block broken")
        helper.assertTrue(cable.sixNodeCacheBlock.isAir, "cache block still there")
        helper.assertTrue(cable.getSideEnable(Direction.YN), "cable gone")
        helper.assertTrue(helper.level.getBlockState(at).getLightBlock(helper.level, at) < 15, "light still blocked")
        helper.succeed()
    }

    // A hot thermal cable inside a closed stone room warms the room's air, which the thermometer then reads.
    @GameTest(template = "empty3x3x3", timeoutTicks = 200)
    @JvmStatic
    fun hotCableWarmsClosedRoom(helper: GameTestHelper) {
        // Stone shell x/z 0..4, y 1..5 around a 3x3x3 air room; z = -1 holds the test's structure block.
        for (x in 0..4) for (y in 1..5) for (z in 0..4) {
            val inside = x in 1..3 && y in 2..4 && z in 1..3
            helper.setBlock(BlockPos(x, y, z), if (inside) Blocks.AIR else Blocks.STONE)
        }
        val cable = placeOnFloor(helper, BlockPos(2, 2, 2), Eln.findItemStack("Copper Thermal Cable", 1))
        val load = cable.getElement(Direction.YN)!!.thermalLoadList.first()
        RoomThermalManager.detectRoomAt(helper.level, helper.absolutePos(BlockPos(2, 3, 2)))
        val roomAt = helper.absolutePos(BlockPos(2, 3, 2))
        helper.assertTrue(RoomThermalManager.getRoomAt(helper.level, roomAt.x, roomAt.y, roomAt.z) != null, "room not detected")
        helper.onEachTick { load.temperatureCelsius = 200.0 }
        helper.succeedWhen {
            val room = RoomThermalManager.getRoomAt(helper.level, roomAt.x, roomAt.y, roomAt.z)
            helper.assertTrue(room != null && room.temperatureCelsius > 0.0, "room at ${room?.temperatureCelsius} C over ambient")
        }
    }

    // The Falstad import tool builds the RC charging example from docs/examples on flat ground next to the player.
    // Own batch: the build spreads well past the test's template.
    @GameTest(template = "empty3x3x3", batch = "falstad")
    @JvmStatic
    fun falstadImportBuildsCircuit(helper: GameTestHelper) {
        for (x in -4..16) for (z in -4..16) {
            helper.setBlock(BlockPos(x, 0, z), Blocks.STONE)
            for (y in 1..3) helper.setBlock(BlockPos(x, y, z), Blocks.AIR)
        }
        // The importer reports through chat; keep its messages for the failure text.
        val messages = ArrayList<String>()
        val player = object : net.minecraftforge.common.util.FakePlayer(helper.level, com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "eln_falstad")) {
            override fun sendSystemMessage(component: net.minecraft.network.chat.Component) {
                messages.add(component.string)
            }
        }
        val at = helper.absolutePos(BlockPos(6, 1, 6))
        player.setPos(at.x + 0.5, at.y.toDouble(), at.z + 0.5)
        FalstadImporter.importNow(player, RC_CHARGING_NETLIST)
        val nodes = BlockPos.betweenClosed(helper.absolutePos(BlockPos(-4, 1, -4)), helper.absolutePos(BlockPos(16, 2, 16)))
            .count { helper.level.getBlockEntity(it) is SixNodeEntity }
        helper.assertTrue(nodes >= 6, "only $nodes six-node blocks placed: ${messages.joinToString(" | ")}")
        helper.succeed()
    }

    // The Nope Wand removes Eln nodes around a creative player without drops. Own batch: its 10-block reach would clear
    // the neighbouring tests' nodes too.
    @GameTest(template = "empty3x3x3", batch = "nope")
    @JvmStatic
    fun nopeWandRemovesNodes(helper: GameTestHelper) {
        placeOnFloor(helper, BlockPos(1, 1, 1), Eln.lowVoltageCableDescriptor!!.newItemStack())
        // Its own fake player: the shared one would carry creative mode into other tests.
        val player = FakePlayerFactory.get(helper.level, com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "eln_nope_wand"))
        val at = helper.absolutePos(BlockPos(1, 1, 1))
        player.setPos(at.x + 0.5, at.y.toDouble(), at.z + 0.5)
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE) // as in 1.7.10, survival players still get the parts back
        val wand = Eln.findItemStack("The Nope Wand", 1)
        GenericItemUsingDamageDescriptor.getDescriptor(wand)!!.onItemRightClick(wand, helper.level, player)
        helper.assertBlockNotPresent(Eln.sixNodeBlock, BlockPos(1, 1, 1))
        helper.assertTrue(helper.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java, net.minecraft.world.phys.AABB(at).inflate(3.0)).isEmpty(), "items dropped")
        helper.succeed()
    }

    // /eln is registered with Brigadier: "ls" lists the subcommands and "zonedestroy" removes nodes around an operator.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun elnCommandRuns(helper: GameTestHelper) {
        placeOnFloor(helper, BlockPos(1, 1, 1), Eln.lowVoltageCableDescriptor!!.newItemStack())
        val output = ArrayList<String>()
        val sink = object : net.minecraft.commands.CommandSource {
            override fun sendSystemMessage(component: net.minecraft.network.chat.Component) { output.add(component.string) }
            override fun acceptsSuccess() = true
            override fun acceptsFailure() = true
            override fun shouldInformAdmins() = false
        }
        val player = FakePlayerFactory.get(helper.level, com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "eln_console"))
        val at = helper.absolutePos(BlockPos(1, 1, 1))
        player.setPos(at.x + 0.5, at.y.toDouble(), at.z + 0.5)
        val source = player.createCommandSourceStack().withSource(sink).withPermission(4)
        val commands = helper.level.server.commands
        commands.performPrefixedCommand(source, "eln ls")
        helper.assertTrue(output.any { "zonedestroy" in it }, "ls listed: $output")
        commands.performPrefixedCommand(source, "eln zonedestroy 1")
        helper.assertBlockNotPresent(Eln.sixNodeBlock, BlockPos(1, 1, 1))
        helper.succeed()
    }

    private val RC_CHARGING_NETLIST = """
        ${'$'} 1 5.0E-6 10 50 5
        v 0 192 0 128 0 0 0 3.0 0
        w 0 128 64 128 0
        s 64 128 128 128 0 1
        r 128 128 256 128 0 20
        c 256 128 256 192 0 0.25 0
        w 256 128 320 128 0
        r 320 128 320 192 0 200
        w 320 192 256 192 0
        w 256 192 0 192 0
        g 0 192 0 192 0
        O 256 128 256 64 2
    """.trimIndent()

    // Fluorescent (energy saving) bulbs fit every lamp socket's bulb slot, not only the flat and fluorescent ones.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun fluorescentBulbFitsEveryLampSocket(helper: GameTestHelper) {
        val bulb = Eln.findItemStack("120V Fluorescent Light Bulb", 1)
        for (name in listOf("Classic", "Spot", "Robust", "Flat", "Simple", "Fluorescent", "Sconce", "Suspended", "Long Suspended")) {
            val socket = Utils.getItemObject(Eln.findItemStack("$name Lamp Socket", 1)) as LampSocketDescriptor
            val slot = LampItemSlot(SimpleContainer(1), 0, 0, 0, 1, socket.acceptedLampTypes)
            helper.assertTrue(slot.mayPlace(bulb), "$name Lamp Socket refuses a fluorescent bulb")
        }
        helper.succeed()
    }

    // The UK light switch is a wireless toggle: each click flips what it sends on its channel. The 13A UK plug fuse
    // settles exactly at its warm limit when 13 A flows.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun ukLightSwitchTogglesAndPlugFuseIsRated(helper: GameTestHelper) {
        val node = placeOnFloor(helper, BlockPos(1, 1, 1), Eln.findItemStack("UK Light Switch", 1))
        val switch = node.getElement(Direction.YN) as WirelessSignalSourceElement
        val player = FakePlayerFactory.getMinecraft(helper.level)
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)
        helper.assertTrue(switch.value < 0.5, "placed switched on")
        switch.onBlockActivated(player, Direction.YN, 0f, 0f, 0f)
        helper.assertTrue(switch.value > 0.5, "first click did not switch it on")
        switch.onBlockActivated(player, Direction.YN, 0f, 0f, 0f)
        helper.assertTrue(switch.value < 0.5, "second click did not switch it off")
        val fuse = Utils.getItemObject(Eln.findItemStack("13A UK Plug Fuse", 1)) as ElectricalFuseDescriptor
        val steadyCelsius = 13.0 * 13.0 * fuse.resistanceOhms() * fuse.thermalRp()
        helper.assertTrue(abs(steadyCelsius - fuse.warmLimitCelsius()) < 1e-6, "13 A settles at $steadyCelsius, limit ${fuse.warmLimitCelsius()}")
        helper.assertTrue(!Eln.findItemStack("240V Type G Socket", 1).isEmpty, "no Type G socket")
        helper.succeed()
    }
}
