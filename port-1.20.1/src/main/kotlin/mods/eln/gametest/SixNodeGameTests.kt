package mods.eln.gametest

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
import mods.eln.sixnode.lampsocket.LampSocketElement
import mods.eln.sixnode.lampsocket.LampSocketContainer
import mods.eln.sixnode.electricalsource.ElectricalSourceElement
import mods.eln.node.NodeBlock
import mods.eln.Eln
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
}
