package mods.eln.gametest

import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraft.world.level.material.Fluids
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.common.capabilities.ForgeCapabilities
import mods.eln.mechanical.GasTurbineDescriptor
import mods.eln.node.transparent.TransparentNodeBlock
import mods.eln.ghost.GhostBlock
import mods.eln.Eln
import net.minecraft.world.item.Items
import net.minecraft.world.Container
import mods.eln.transparentnode.autominer.AutoMinerElement
import mods.eln.transparentnode.autominer.AutoMinerContainer
import mods.eln.sim.mna.component.VoltageSource
import mods.eln.misc.LRDU
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeEntity
import mods.eln.node.transparent.TransparentNodeItem
import mods.eln.sixnode.lampsocket.LampSocketContainer
import mods.eln.sixnode.lampsocket.LampSocketElement
import mods.eln.sixnode.lampsocket.LampSocketRender
import mods.eln.transparentnode.battery.BatteryElement
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate

@GameTestHolder(Eln.MODID)
@PrefixGameTestTemplate(false)
object TransparentNodeGameTests {
    // A fresh 12 V battery (positive terminal on the floor to its left, negative to its right) lights a 12 V lamp
    // through a cable. Batteries float, so a ground cable on the negative terminal closes the lamp's ground return.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun batteryPowersLamp(helper: GameTestHelper) {
        val batteryPos = BlockPos(1, 1, 1)
        helper.setBlock(batteryPos.below(), Blocks.STONE)
        val stack = Eln.transparentNodeItem.getDescriptor(16 shl 6)!!.newItemStack()
        val item = stack.item as TransparentNodeItem
        helper.assertTrue(item.placeBlockAt(stack, helper.makeMockPlayer(), helper.level, helper.absolutePos(batteryPos), Direction.YP), "battery not placed")
        val entity = helper.level.getBlockEntity(helper.absolutePos(batteryPos)) as? TransparentNodeEntity
        val battery = (entity?.node as? TransparentNode)?.element as? BatteryElement
        helper.assertTrue(battery != null, "no battery element")

        SixNodeGameTests.placeOnFloor(helper, batteryPos.offset(battery!!.front.right().toForge().normal), Eln.sixNodeItem.getDescriptor(2 shl 6)!!.newItemStack())
        val positive = battery.front.left().toForge().normal
        val cablePos = batteryPos.offset(positive)
        // Next to the cable, away from the battery, inside the 3x3 floor.
        val lampPos = if (positive.x != 0) cablePos.offset(0, 0, 1) else cablePos.offset(1, 0, 0)
        SixNodeGameTests.placeOnFloor(helper, cablePos, Eln.lowVoltageCableDescriptor!!.newItemStack())
        val lampNode = SixNodeGameTests.placeOnFloor(helper, lampPos, Eln.sixNodeItem.getDescriptor(64 shl 6)!!.newItemStack())
        val socket = lampNode.getElement(Direction.YN) as LampSocketElement
        socket.inventory.setItem(LampSocketContainer.LAMP_SLOT_ID, Eln.sharedItem.getDescriptor(4 shl 6)!!.newItemStack())
        socket.inventory.setItem(LampSocketContainer.CABLE_SLOT_ID, Eln.lowVoltageCableDescriptor!!.newItemStack())
        socket.poweredByLampSupply = false
        socket.inventoryChange(socket.inventory)

        helper.succeedWhen {
            helper.assertTrue(lampNode.lightValue >= LampSocketRender.MIN_LIGHT_ON_VALUE, "lamp light ${lampNode.lightValue}, ${socket.electricalLoad.voltage} V, battery ${battery.batteryProcess.u} V front=${battery.front} batteryConn=${battery.node!!.nodeConnectionList.size} lampConn=${lampNode.nodeConnectionList.size} cable=$cablePos lamp=$lampPos")
        }
    }

    // The 2x3 solar panel fills the rest of its footprint with ghost blocks; breaking one removes the whole panel.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun solarPanelGhostBlocks(helper: GameTestHelper) {
        val pos = BlockPos(1, 1, 1)
        for (x in 0..2) for (z in 0..2) helper.setBlock(BlockPos(x, 0, z), Blocks.STONE)
        val stack = Eln.transparentNodeItem.getDescriptor((48 shl 6) + 3)!!.newItemStack()
        val item = stack.item as TransparentNodeItem
        helper.assertTrue(item.placeBlockAt(stack, helper.makeMockPlayer(), helper.level, helper.absolutePos(pos), Direction.YP), "panel not placed")
        val ghosts = BlockPos.betweenClosed(BlockPos(0, 1, 0), BlockPos(2, 1, 2)).map { it.immutable() }
            .filter { helper.getBlockState(it).block is GhostBlock }
        helper.assertTrue(ghosts.size == 5, "${ghosts.size} ghost blocks")
        helper.destroyBlock(ghosts.first())
        helper.succeedWhen {
            helper.assertBlockNotPresent(TransparentNodeBlock.BLOCK.get(), pos)
            helper.assertTrue(ghosts.none { helper.getBlockState(it).block is GhostBlock }, "ghost blocks left behind")
        }
    }

    // Machines with a tank expose it to pipes through the Forge fluid capability; a gas turbine only takes fuels.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun gasTurbineTankRefusesWater(helper: GameTestHelper) {
        val pos = BlockPos(1, 1, 1)
        helper.setBlock(pos.below(), Blocks.STONE)
        val desc = Eln.transparentNodeItem.descriptors.filterIsInstance<GasTurbineDescriptor>().first { it.ghostGroup == null }
        val stack = desc.newItemStack()
        helper.assertTrue((stack.item as TransparentNodeItem).placeBlockAt(stack, helper.makeMockPlayer(), helper.level, helper.absolutePos(pos), Direction.YP), "turbine not placed")
        val tank = helper.level.getBlockEntity(helper.absolutePos(pos))!!.getCapability(ForgeCapabilities.FLUID_HANDLER, null).resolve().orElse(null)
        helper.assertTrue(tank != null, "no fluid capability")
        helper.assertTrue(tank!!.fill(FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "water went into the tank")
        helper.succeed()
    }

    // A powered auto miner with a drill and mining pipes digs the block under it and puts the drops in the chest
    // at its output.
    @GameTest(template = "empty3x3x3", timeoutTicks = 400)
    @JvmStatic
    fun autoMinerDigsIntoChest(helper: GameTestHelper) {
        val pos = helper.absolutePos(BlockPos(1, 1, 1))
        val stack = Eln.findItemStack("Auto Miner", 1)
        helper.assertTrue((stack.item as TransparentNodeItem).placeBlockAt(stack, helper.makeMockPlayer(), helper.level, pos, Direction.YP), "auto miner not placed")
        // The miner sits two blocks in front of and one above the clicked block (its spawn delta).
        val miner = BlockPos.betweenClosed(pos.offset(-3, 0, -3), pos.offset(3, 2, 3))
            .firstNotNullOf { ((helper.level.getBlockEntity(it) as? TransparentNodeEntity)?.node as? TransparentNode)?.element as? AutoMinerElement }
        helper.level.setBlockAndUpdate(miner.node!!.coordinate.blockPos.below(), Blocks.STONE.defaultBlockState())
        val output = Coordinate(1, -1, 0, helper.level.dimension())
        output.applyTransformation(miner.front, miner.node!!.coordinate)
        helper.level.setBlockAndUpdate(output.blockPos, Blocks.CHEST.defaultBlockState())
        miner.inventory!!.setItem(AutoMinerContainer.electricalDrillSlotId, Eln.findItemStack("Turbo Electrical Drill", 1))
        miner.inventory!!.setItem(AutoMinerContainer.MiningPipeSlotId, Eln.findItemStack("Mining Pipe", 8))
        val source = VoltageSource("source", miner.getElectricalLoad(Direction.XN, LRDU.Down), null)
            .setVoltage(Eln.highVoltageCableDescriptor.electricalNominalVoltage)
        Eln.simulator.addElectricalComponent(source)
        helper.succeedWhen {
            val chest = helper.level.getBlockEntity(output.blockPos) as Container
            helper.assertTrue((0 until chest.containerSize).any { chest.getItem(it).`is`(Items.COBBLESTONE) }, "no cobblestone in the chest")
            Eln.simulator.removeElectricalComponent(source)
        }
    }
}
