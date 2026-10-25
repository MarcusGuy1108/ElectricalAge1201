package mods.eln.gametest

import mods.eln.Eln
import mods.eln.misc.Direction
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeEntity
import mods.eln.node.six.SixNodeItem
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sixnode.electricalcable.ElectricalCableElement
import net.minecraft.core.BlockPos
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
        helper.setBlock(relative.below(), Blocks.STONE)
        val stack = Eln.lowVoltageCableDescriptor!!.newItemStack()
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
