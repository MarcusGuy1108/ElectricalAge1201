package mods.eln.gametest

import net.minecraft.world.level.block.Blocks
import net.minecraftforge.registries.ForgeRegistries
import mods.eln.ore.ElnOre
import net.minecraft.resources.ResourceLocation
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import mods.eln.Eln
import mods.eln.item.electricalitem.ElectricalPickaxe
import mods.eln.item.CopperGear
import mods.eln.generic.GenericItemUsingDamageDescriptor
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

    // The creative search index builds every tooltip with no player; a descriptor that needs one crashes the client.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun tooltipsWorkWithoutAPlayer(helper: GameTestHelper) {
        val failures = ArrayList<String>()
        fun check(name: String, tooltip: () -> Unit) {
            try {
                tooltip()
            } catch (e: Throwable) {
                // Tooltips only run on the client; one that reads client state cannot run on this test server.
                if (e.message?.contains("invalid dist") != true) failures.add("$name: $e")
            }
        }
        for (desc in Eln.sixNodeItem.subItemList.values) check(desc.name) { desc.addInformation(desc.newItemStack(), null, ArrayList(), false) }
        for (desc in Eln.transparentNodeItem.subItemList.values) check(desc.name) { desc.addInformation(desc.newItemStack(), null, ArrayList(), false) }
        for (desc in Eln.sharedItem.subItemList.values) check(desc.name) { desc.addInformation(desc.newItemStack(), null, ArrayList(), false) }
        for (desc in Eln.sharedItemStackOne.subItemList.values) check(desc.name) { desc.addInformation(desc.newItemStack(), null, ArrayList(), false) }
        helper.assertTrue(failures.isEmpty(), failures.joinToString("; "))
        helper.succeed()
    }

    // Ore world generation is data: the placed features and the biome modifier must load on the server.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun oreWorldGenLoads(helper: GameTestHelper) {
        val placed = helper.level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE)
        for (ore in ElnOre.values()) helper.assertTrue(placed.containsKey(ResourceLocation(Eln.MODID, ore.id)), "no placed feature ${ore.id}")
        val modifiers = helper.level.registryAccess().registryOrThrow(ForgeRegistries.Keys.BIOME_MODIFIERS)
        helper.assertTrue(modifiers.containsKey(ResourceLocation(Eln.MODID, "ores")), "no ore biome modifier")
        helper.succeed()
    }

    // A copper vein placed into solid stone turns some of it into Eln copper ore.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun oreVeinReplacesStone(helper: GameTestHelper) {
        val area = BlockPos.betweenClosed(BlockPos(0, 0, 0), BlockPos(2, 2, 2)).map { it.immutable() }
        for (pos in area) helper.setBlock(pos, Blocks.STONE)
        val vein = helper.level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
            .get(ResourceLocation(Eln.MODID, ElnOre.COPPER.id))!!
        // A vein spreads randomly and may miss this small cube, so it gets a few tries.
        val placed = (1..20).any {
            vein.place(helper.level, helper.level.chunkSource.generator, helper.level.random, helper.absolutePos(BlockPos(1, 1, 1)))
            area.any { helper.getBlockState(it).`is`(ElnOre.COPPER.block.get()) }
        }
        helper.assertTrue(placed, "no copper ore placed")
        helper.succeed()
    }

    // Crafting recipes are generated data; the machine recipe lists fill when the server starts.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun recipesLoad(helper: GameTestHelper) {
        val recipes = helper.level.recipeManager
        helper.assertTrue(recipes.byKey(ResourceLocation(Eln.MODID, "low_voltage_cable")).isPresent, "no low voltage cable recipe")
        helper.assertTrue(recipes.byKey(ResourceLocation(Eln.MODID, "smelting/copper_ingot")).isPresent, "no copper smelting recipe")
        helper.assertTrue(Eln.maceratorRecipes.getRecipe(ItemStack(ElnOre.COPPER.block.get())) != null, "no macerator recipe for copper ore")
        helper.succeed()
    }

    // The drill digs at full speed only while charged, pays for each block, and drops what it digs.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun miningDrillUsesEnergy(helper: GameTestHelper) {
        val drill = Eln.findItemStack("Portable Electrical Mining Drill", 1)
        val descriptor = GenericItemUsingDamageDescriptor.getDescriptor(drill) as ElectricalPickaxe
        val stone = Blocks.STONE.defaultBlockState()
        helper.assertTrue(drill.getDestroySpeed(stone) == descriptor.strengthOff, "empty drill should dig slowly")
        descriptor.setEnergy(drill, descriptor.energyStorage)
        helper.assertTrue(drill.getDestroySpeed(stone) == descriptor.strengthOn, "charged drill should dig fast")
        helper.assertTrue(drill.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState()), "drill should drop iron ore")
        helper.assertFalse(drill.isCorrectToolForDrops(Blocks.OAK_LOG.defaultBlockState()), "drill should not count as an axe")
        helper.assertTrue(ItemStack(CopperGear.get("copper_pickaxe")).isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState()), "copper pickaxe should be iron grade")
        helper.succeed()
    }
}
