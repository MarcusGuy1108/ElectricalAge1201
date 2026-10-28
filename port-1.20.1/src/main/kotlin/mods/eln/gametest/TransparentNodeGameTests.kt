package mods.eln.gametest

import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraft.world.level.material.Fluids
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.common.capabilities.ForgeCapabilities
import mods.eln.mechanical.GasTurbineDescriptor
import mods.eln.node.transparent.TransparentNodeBlock
import mods.eln.ghost.GhostBlock
import mods.eln.Eln
import mods.eln.transparentnode.EnergyExporterElement
import mods.eln.transparentnode.EnergyImporterElement
import mods.eln.sim.mna.component.Resistor
import mods.eln.railroad.EntityElectricMinecart
import mods.eln.railroad.OverheadLinesElement
import net.minecraft.world.entity.EntityType
import mods.eln.transparentnode.turret.TurretElement
import mods.eln.transparentnode.turret.TurretContainer
import mods.eln.transparentnode.floodlight.FloodlightElement
import mods.eln.transparentnode.floodlight.FloodlightContainer
import mods.eln.lightblock.LightBlock
import net.minecraft.world.item.ItemStack
import mods.eln.ore.ElnOre
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraft.world.InteractionHand
import mods.eln.gridnode.electricalpole.ElectricalPoleElement
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

    // Right-clicking two utility poles with very high voltage cable links them; the link carries power and uses up
    // one cable per block of distance.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun utilityPolesLinkWithCable(helper: GameTestHelper) {
        val poles = listOf(BlockPos(0, 1, 0), BlockPos(2, 1, 2)).map { pos ->
            val stack = Eln.findItemStack("Utility Pole", 1)
            helper.setBlock(pos.below(), Blocks.STONE)
            helper.assertTrue((stack.item as TransparentNodeItem).placeBlockAt(stack, helper.makeMockPlayer(), helper.level, helper.absolutePos(pos), Direction.YP), "pole not placed at $pos")
            ((helper.level.getBlockEntity(helper.absolutePos(pos)) as TransparentNodeEntity).node as TransparentNode).element as ElectricalPoleElement
        }
        val player = FakePlayerFactory.getMinecraft(helper.level)
        player.setItemInHand(InteractionHand.MAIN_HAND, Eln.veryHighVoltageCableDescriptor.newItemStack(16))
        for (pole in poles) pole.onBlockActivated(player, Direction.YP, 0f, 0f, 0f)
        helper.assertTrue(poles[0].gridLinkList.size == 1, "poles not linked")
        helper.assertTrue(player.mainHandItem.count == 13, "${player.mainHandItem.count} cables left, expected 13")
        // No load on the far pole: a resistor to ground straight on a pole's line-simplified load is not a circuit the
        // solver's line folding handles, and an open link shows the source voltage anyway.
        val source = VoltageSource("source", poles[0].electricalLoad, null).setVoltage(1000.0)
        Eln.simulator.addElectricalComponent(source)
        helper.succeedWhen {
            helper.assertTrue(poles[1].electricalLoad.voltage > 990.0, "far pole at ${poles[1].electricalLoad.voltage} V")
            Eln.simulator.removeElectricalComponent(source)
        }
    }

    // A hopper on top of a macerator feeds its input slot, through the item handler that carries the 1.7.10 side access.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun hopperFeedsMacerator(helper: GameTestHelper) {
        val pos = BlockPos(1, 1, 1)
        helper.setBlock(pos.below(), Blocks.STONE)
        val stack = Eln.findItemStack("48V Macerator", 1)
        helper.assertTrue((stack.item as TransparentNodeItem).placeBlockAt(stack, helper.makeMockPlayer(), helper.level, helper.absolutePos(pos), Direction.YP), "macerator not placed")
        val machine = ((helper.level.getBlockEntity(helper.absolutePos(pos)) as TransparentNodeEntity).node as TransparentNode).element!!
        helper.setBlock(pos.above(), Blocks.HOPPER)
        (helper.getBlockEntity(pos.above()) as Container).setItem(0, ItemStack(ElnOre.COPPER.block.get()))
        helper.succeedWhen {
            val inventory = machine.inventory!!
            helper.assertTrue((0 until inventory.containerSize).any { inventory.getItem(it).`is`(ElnOre.COPPER.block.get().asItem()) }, "no ore in the macerator")
        }
    }

    // A powered floodlight with two bulbs throws its beam as light blocks away from itself.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun floodlightThrowsBeam(helper: GameTestHelper) {
        val pos = BlockPos(1, 1, 1)
        helper.setBlock(pos.below(), Blocks.STONE)
        val stack = Eln.findItemStack("120V Basic Floodlight", 1)
        helper.assertTrue((stack.item as TransparentNodeItem).placeBlockAt(stack, helper.makeMockPlayer(), helper.level, helper.absolutePos(pos), Direction.YP), "floodlight not placed")
        val light = ((helper.level.getBlockEntity(helper.absolutePos(pos)) as TransparentNodeEntity).node as TransparentNode).element as FloodlightElement
        light.inventory.setItem(FloodlightContainer.LAMP_SLOT_1_ID, Eln.findItemStack("120V Incandescent Light Bulb", 1))
        light.inventory.setItem(FloodlightContainer.LAMP_SLOT_2_ID, Eln.findItemStack("120V Incandescent Light Bulb", 1))
        val source = VoltageSource("source", light.electricalLoad, null).setVoltage(120.0)
        Eln.simulator.addElectricalComponent(source)
        val origin = helper.absolutePos(pos)
        helper.succeedWhen {
            val lit = BlockPos.betweenClosed(origin.offset(-24, -4, -24), origin.offset(24, 24, 24))
                .count { it.distManhattan(origin) > 2 && helper.level.getBlockState(it).block is LightBlock }
            helper.assertTrue(lit > 0, "no light blocks from the beam, light value ${light.node!!.lightValue}")
            Eln.simulator.removeElectricalComponent(source)
        }
    }

    // A powered turret with a monster filter shoots a monster standing in front of it.
    @GameTest(template = "empty3x3x3", timeoutTicks = 400)
    @JvmStatic
    fun turretShootsMonster(helper: GameTestHelper) {
        val pos = BlockPos(1, 1, 1)
        helper.setBlock(pos.below(), Blocks.STONE)
        val stack = Eln.findItemStack("480V Defence Turret", 1)
        helper.assertTrue((stack.item as TransparentNodeItem).placeBlockAt(stack, helper.makeMockPlayer(), helper.level, helper.absolutePos(pos), Direction.YP), "turret not placed")
        val turret = ((helper.level.getBlockEntity(helper.absolutePos(pos)) as TransparentNodeEntity).node as TransparentNode).element as TurretElement
        turret.inventory!!.setItem(TurretContainer.filterId, Eln.findItemStack("Monster Filter", 1))
        val source = VoltageSource("source", turret.getElectricalLoad(turret.front.back(), LRDU.Down), null).setVoltage(800.0)
        Eln.simulator.addElectricalComponent(source)
        // Silverfish do not burn in daylight, so only the turret can hurt this one. It stands right in front of the
        // turret: further out, the test's own structure block can be in the line of fire.
        val monsterPos = pos.offset(turret.front.toForge().normal)
        helper.setBlock(monsterPos.below(), Blocks.STONE)
        val monster = helper.spawnWithNoFreeWill(EntityType.SILVERFISH, monsterPos)
        helper.succeedWhen {
            helper.assertTrue(monster.health < monster.maxHealth || !monster.isAlive, "monster unhurt at ${monster.health}, turret angle ${turret.turretAngle}")
            Eln.simulator.removeElectricalComponent(source)
        }
    }

    // The egg incubator is half a block high, as its 1.7.10 collision box was.
    @GameTest(template = "empty3x3x3")
    @JvmStatic
    fun eggIncubatorIsHalfHigh(helper: GameTestHelper) {
        val pos = BlockPos(1, 1, 1)
        helper.setBlock(pos.below(), Blocks.STONE)
        val stack = Eln.findItemStack("24V Egg Incubator", 1)
        helper.assertTrue((stack.item as TransparentNodeItem).placeBlockAt(stack, helper.makeMockPlayer(), helper.level, helper.absolutePos(pos), Direction.YP), "incubator not placed")
        val abs = helper.absolutePos(pos)
        val top = helper.level.getBlockState(abs).getCollisionShape(helper.level, abs).max(net.minecraft.core.Direction.Axis.Y)
        helper.assertTrue(top == 0.5, "collision top at $top")
        helper.succeed()
    }

    // An electric minecart on a rail under powered overhead lines charges its energy buffer from them.
    @GameTest(template = "empty3x3x3", timeoutTicks = 200)
    @JvmStatic
    fun electricMinecartChargesUnderOverheadLines(helper: GameTestHelper) {
        val rail = BlockPos(1, 1, 1)
        helper.setBlock(rail.below(), Blocks.STONE)
        helper.setBlock(rail, Blocks.RAIL)
        val linesPos = rail.above(2)
        val stack = Eln.findItemStack("Overhead Lines", 1)
        helper.assertTrue((stack.item as TransparentNodeItem).placeBlockAt(stack, helper.makeMockPlayer(), helper.level, helper.absolutePos(linesPos), Direction.YP), "overhead lines not placed")
        val lines = ((helper.level.getBlockEntity(helper.absolutePos(linesPos)) as TransparentNodeEntity).node as TransparentNode).element as OverheadLinesElement
        val source = VoltageSource("source", lines.electricalLoad, null).setVoltage(1000.0)
        Eln.simulator.addElectricalComponent(source)
        val railAbs = helper.absolutePos(rail)
        val cart = EntityElectricMinecart(helper.level, railAbs.x + 0.5, railAbs.y + 0.0625, railAbs.z + 0.5)
        helper.level.addFreshEntity(cart)
        helper.succeedWhen {
            helper.assertTrue(cart.energyBufferJoules > 0.0, "cart has ${cart.energyBufferJoules} J, lines at ${lines.electricalLoad.voltage} V")
            Eln.simulator.removeElectricalComponent(source)
            cart.discard()
        }
    }

    // A powered energy exporter fills its buffer and hands it out as Forge Energy.
    @GameTest(template = "empty3x3x3", timeoutTicks = 200)
    @JvmStatic
    fun energyExporterGivesForgeEnergy(helper: GameTestHelper) {
        val pos = BlockPos(1, 1, 1)
        helper.setBlock(pos.below(), Blocks.STONE)
        val stack = Eln.findItemStack("Energy Exporter", 1)
        helper.assertTrue((stack.item as TransparentNodeItem).placeBlockAt(stack, helper.makeMockPlayer(), helper.level, helper.absolutePos(pos), Direction.YP), "exporter not placed")
        val entity = helper.level.getBlockEntity(helper.absolutePos(pos)) as TransparentNodeEntity
        val exporter = (entity.node as TransparentNode).element as EnergyExporterElement
        exporter.selectedOhms = 100.0
        val source = VoltageSource("source", exporter.load, null).setVoltage(200.0)
        Eln.simulator.addElectricalComponent(source)
        val energy = entity.getCapability(ForgeCapabilities.ENERGY, null).resolve().get()
        helper.succeedWhen {
            // 200 V over 100 ohms is 400 W; a few ticks give a few hundred FE at the 4/3 FE per joule default.
            helper.assertTrue(energy.extractEnergy(1000, false) > 0, "no FE, buffer ${exporter.energyBuffer} J at ${exporter.load.voltage} V")
            Eln.simulator.removeElectricalComponent(source)
        }
    }

    // An importer fed Forge Energy holds its output at the set voltage, and stops within seconds once the FE stops.
    @GameTest(template = "empty3x3x3", timeoutTicks = 400)
    @JvmStatic
    fun energyImporterSuppliesSetVoltage(helper: GameTestHelper) {
        val pos = BlockPos(1, 1, 1)
        helper.setBlock(pos.below(), Blocks.STONE)
        val stack = Eln.findItemStack("Energy Importer", 1)
        helper.assertTrue((stack.item as TransparentNodeItem).placeBlockAt(stack, helper.makeMockPlayer(), helper.level, helper.absolutePos(pos), Direction.YP), "importer not placed")
        val entity = helper.level.getBlockEntity(helper.absolutePos(pos)) as TransparentNodeEntity
        val importer = (entity.node as TransparentNode).element as EnergyImporterElement
        importer.selectedVoltage = 200.0
        val energy = entity.getCapability(ForgeCapabilities.ENERGY, null).resolve().get()
        var feeding = true
        // A supply pushing more FE than needed every tick, as another mod's cable would.
        helper.onEachTick { if (feeding) energy.receiveEnergy(10_000, false) }
        // 200 V into 100 ohms is 400 W.
        val lamp = Resistor(importer.load, null).setResistance(100.0)
        Eln.simulator.addElectricalComponent(lamp)
        helper.startSequence()
            .thenWaitUntil { helper.assertTrue(importer.load.voltage in 195.0..201.0, "output ${importer.load.voltage} V") }
            .thenExecute { feeding = false }
            // With the supply cut, the few seconds of reserve run out and the output drops.
            .thenWaitUntil { helper.assertTrue(importer.load.voltage < 1.0, "still ${importer.load.voltage} V with ${importer.energyBuffer} J left") }
            .thenExecute { Eln.simulator.removeElectricalComponent(lamp) }
            .thenSucceed()
    }
}
