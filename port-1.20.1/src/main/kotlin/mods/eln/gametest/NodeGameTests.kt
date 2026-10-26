package mods.eln.gametest

import net.minecraft.resources.ResourceLocation
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.NodeBase
import mods.eln.node.NodeManager
import mods.eln.sim.ElectricalLoad
import mods.eln.sim.ThermalLoad
import mods.eln.sim.mna.component.Resistor
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.sim.nbt.NbtElectricalLoad
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import kotlin.math.abs

/**
 * Node with one electrical load on every face, so two of them side by side connect like a short cable.
 */
class TestNode : NodeBase() {
    val load = NbtElectricalLoad("load").also { it.setSerialResistance(SERIAL_RESISTANCE) }

    override val nodeUuid: String
        get() = UUID

    override fun initializeFromThat(front: Direction, entityLiving: LivingEntity?, itemStack: ItemStack?) {
        Eln.simulator.addElectricalLoad(load)
    }

    override fun initializeFromNBT() {
        Eln.simulator.addElectricalLoad(load)
    }

    override fun globalBoot() {
        connect()
    }

    override fun getSideConnectionMask(side: Direction, lrdu: LRDU): Int = maskElectricalPower
    override fun getThermalLoad(side: Direction, lrdu: LRDU, mask: Int): ThermalLoad? = null
    override fun getElectricalLoad(side: Direction, lrdu: LRDU, mask: Int): ElectricalLoad = load

    override fun readFromNBT(nbt: CompoundTag) {
        super.readFromNBT(nbt)
        load.readFromNBT(nbt, "")
    }

    override fun writeToNBT(nbt: CompoundTag) {
        super.writeToNBT(nbt)
        load.writeToNBT(nbt, "")
    }

    companion object {
        const val UUID = "eln:gametest_node"
        const val SERIAL_RESISTANCE = 1.0
    }
}

@GameTestHolder(Eln.MODID)
@PrefixGameTestTemplate(false)
object NodeGameTests {
    init {
        NodeManager.registerUuid(TestNode.UUID, TestNode::class.java)
    }

    private fun placeNode(helper: GameTestHelper, relative: BlockPos): TestNode {
        val pos = helper.absolutePos(relative)
        val node = TestNode()
        node.onBlockPlacedBy(Coordinate(pos.x, pos.y, pos.z, helper.level), Direction.XN, null, null)
        return node
    }

    private fun removeNode(node: TestNode) {
        node.onBreakBlock()
        Eln.simulator.removeElectricalLoad(node.load)
    }

    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun coordinateKeepsDimensionThroughNbt(helper: GameTestHelper) {
        val nbt = CompoundTag()
        Coordinate(1, -2, 3, Level.NETHER).writeToNBT(nbt, "c")
        helper.assertTrue(nbt.getString("cd") == "minecraft:the_nether", "dimension saved as ${nbt.getString("cd")}")
        helper.assertTrue(Coordinate(nbt, "c") == Coordinate(1, -2, 3, Level.NETHER), "coordinate did not round trip")
        val legacy = CompoundTag().also { it.putInt("cx", 4) }
        helper.assertTrue(Coordinate(legacy, "c").dimension == Level.OVERWORLD, "missing dimension should read as overworld")
        helper.succeed()
    }

    // A 10 V source on one node and a 10 ohm load on its neighbour: the connection must divide the voltage.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun adjacentNodesConnectElectrically(helper: GameTestHelper) {
        val source = placeNode(helper, BlockPos(1, 1, 1))
        val sink = placeNode(helper, BlockPos(2, 1, 1))
        val voltageSource = VoltageSource("source", source.load, null).setVoltage(10.0)
        val resistor = Resistor(sink.load, null).setResistance(10.0)
        Eln.simulator.addElectricalComponent(voltageSource)
        Eln.simulator.addElectricalComponent(resistor)
        try {
            source.connect()
            sink.connect()
            helper.assertTrue(sink.nodeConnectionList.size == 4, "expected one connection per LRDU, got ${sink.nodeConnectionList.size}")
            Eln.simulator.mna.step()
            // Four parallel connections of 2 * SERIAL_RESISTANCE each between the loads.
            val connection = 2 * TestNode.SERIAL_RESISTANCE / 4
            val expected = 10.0 * 10.0 / (10.0 + connection)
            helper.assertTrue(abs(sink.load.voltage - expected) < 1e-6, "sink at ${sink.load.voltage} V, expected $expected V")
            helper.succeed()
        } finally {
            Eln.simulator.removeElectricalComponent(voltageSource)
            Eln.simulator.removeElectricalComponent(resistor)
            removeNode(source)
            removeNode(sink)
        }
    }

    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun nodesSurviveSaveAndLoad(helper: GameTestHelper) {
        val node = placeNode(helper, BlockPos(1, 1, 1))
        node.load.voltage = 42.0
        val coordinate = Coordinate(node.coordinate)
        val nbt = CompoundTag()
        Eln.nodeManager.saveToNbt(nbt, helper.level.dimension())
        removeNode(node)
        helper.assertTrue(Eln.nodeManager.getNodeFromCoordonate(coordinate) == null, "node still registered after removal")

        val saved = CompoundTag()
        for (key in nbt.allKeys) {
            val tag = nbt.getCompound(key)
            if (Coordinate(tag, "c") == coordinate) saved.put(key, tag)
        }
        Eln.nodeManager.loadFromNbt(saved, helper.level.dimension())
        val loaded = Eln.nodeManager.getNodeFromCoordonate(coordinate)
        try {
            helper.assertTrue(loaded is TestNode, "loaded ${loaded?.javaClass?.simpleName}")
            helper.assertTrue(abs((loaded as TestNode).load.voltage - 42.0) < 1e-3, "voltage ${loaded.load.voltage} after reload")
            helper.succeed()
        } finally {
            (loaded as? TestNode)?.let { removeNode(it) }
        }
    }

    // A saved node that cannot be loaded (here an unknown type) must be written back by the next save, not dropped.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun unloadableNodesStayInTheSave(helper: GameTestHelper) {
        val dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation(Eln.MODID, "unloadable_node_test"))
        val tag = CompoundTag()
        tag.putString("tag", "no such node type")
        Coordinate(1, 2, 3, dimension).writeToNBT(tag, "c")
        tag.putInt("payload", 1234)
        val saved = CompoundTag()
        saved.put("n0", tag)
        try {
            Eln.nodeManager.loadFromNbt(saved, dimension)
            val written = CompoundTag()
            Eln.nodeManager.saveToNbt(written, dimension)
            helper.assertTrue(written.allKeys.size == 1, "saved ${written.allKeys.size} nodes")
            helper.assertTrue(written.getCompound(written.allKeys.first()).getInt("payload") == 1234, "node data changed")
            helper.succeed()
        } finally {
            Eln.nodeManager.unload(dimension)
        }
    }
}
