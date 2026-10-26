package mods.eln.registration

import mods.eln.Eln
import mods.eln.Eln.instance
import mods.eln.Eln.transparentNodeItem
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.TR_NAME
import mods.eln.misc.FunctionTable
import mods.eln.misc.NominalVoltage
import mods.eln.misc.Utils.printFunction
import mods.eln.transparentnode.battery.BatteryDescriptor

/**
 * Transparent node (full block machine) registration, ported from 1.7.10 one family at a time. Descriptor ids
 * (subId + (group shl 6)) are unchanged because node saves store them.
 */
object TransparentNodeRegistration {
    fun registerTransparent() {
        Eln.transparentNodeItem.setCreativeTabForGroup(1, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(2, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(3, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(4, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(7, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(16, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(32, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(33, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(34, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(35, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(36, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(37, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(41, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(42, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(48, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(49, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(64, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(65, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(66, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(67, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(68, Eln.creativeTabLighting)
        Eln.transparentNodeItem.setCreativeTabForGroup(69, Eln.creativeTabLighting)
        Eln.transparentNodeItem.setCreativeTabForGroup(70, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(71, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(72, Eln.creativeTabMachines)
        Eln.transparentNodeItem.setCreativeTabForGroup(96, Eln.creativeTabPowerElectronics)
        Eln.transparentNodeItem.setCreativeTabForGroup(117, Eln.creativeTabSignalProcessing)
        Eln.transparentNodeItem.setCreativeTabForGroup(123, Eln.creativeTabPowerDistribution)

        // PORT: registerPowerComponent(1)
        // PORT: registerTransformer(2)
        // PORT: registerHeatFurnace(3)
        // PORT: registerTurbine(4)
        // PORT: registerElectricalAntenna(7)
        registerBattery(16)
        // PORT: registerElectricalFurnace(32)
        // PORT: registerMacerator(33)
        // PORT: registerArcFurnace(34)
        // PORT: registerCompressor(35)
        // PORT: registerMagnetizer(36)
        // PORT: registerPlateMachine(37)
        // PORT: registerEggIncubator(41)
        // PORT: registerAutoMiner(42)
        // PORT: registerSolarPanel(48)
        // PORT: registerWindTurbine(49)
        // PORT: registerThermalDissipatorPassiveAndActive(64)
        // PORT: registerTransparentNodeMisc(65)
        // PORT: registerTurret(66)
        // PORT: registerFuelGenerator(67)
        // PORT: registerFloodlight(68)
        // PORT: registerFestive(69)
        // PORT: registerFab(70)
        // PORT: registerRailroad(71)
        // PORT: registerWireProcessingMachines(72)
        // PORT: registerLargeRheostat() // 96, but from the wrong side.
        // PORT: registerNixieTube() // 117, but from the wrong side.
        // PORT: registerGridDevices(123)
    }

    private fun registerBattery(id: Int) {
        var subId: Int
        var name: String
        val batteryCapacityFactor = Eln.config.getDoubleOrElse("balance.storage.batteryCapacityFactor", 1.0)
        val stdBatteryHalfLife = Eln.config.getDoubleOrElse("runtime.items.batteries.standardHalfLifeTicks", 2.0)
        val heatTIme = 30.0
        val voltageFunctionTable = doubleArrayOf(0.000, 0.9, 1.0, 1.025, 1.04, 1.05, 2.0)
        val voltageFunction = FunctionTable(voltageFunctionTable, 6.0 / 5)

        printFunction(voltageFunction, -0.2, 1.2, 0.1)

        val stdDischargeTime = (60 * 8).toDouble()
        val stdU = NominalVoltage.V12
        val stdP: Double = instance.LVP() / 4
        val stdEfficiency = 1.0 - 2.0 / 50.0

        instance.batteryVoltageFunctionTable = voltageFunction
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Cost Oriented Battery")
            val desc = BatteryDescriptor(
                name, "BatteryBig", 0.5, true, true, voltageFunction, stdU,
                stdP * 1.2, 0.0, stdP, stdDischargeTime * batteryCapacityFactor, stdEfficiency, stdBatteryHalfLife,
                heatTIme, 60.0, -100.0
            )
            desc.setRenderSpec("lowcost")
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0)
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            name = TR_NAME(I18N.Type.NONE, "Capacity Oriented Battery")
            val desc = BatteryDescriptor(
                name, "BatteryBig", 0.5, true, true, voltageFunction,
                stdU, stdP / 2 * 1.2, 0.000, stdP / 2, stdDischargeTime * 8 * batteryCapacityFactor, stdEfficiency,
                stdBatteryHalfLife, heatTIme, 60.0, -100.0
            )
            desc.setRenderSpec("capacity")
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0)
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 2
            name = TR_NAME(I18N.Type.NONE, "Voltage Oriented Battery")
            val desc = BatteryDescriptor(
                name, "BatteryBig", 0.5, true, true, voltageFunction,
                NominalVoltage.V48, stdP * 1.2, 0.000, stdP, stdDischargeTime * batteryCapacityFactor, stdEfficiency,
                stdBatteryHalfLife, heatTIme, 60.0, -100.0
            )
            desc.setRenderSpec("highvoltage")
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0)
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 3
            name = TR_NAME(I18N.Type.NONE, "Current Oriented Battery")
            val desc = BatteryDescriptor(
                name, "BatteryBig", 0.5, true, true, voltageFunction, NominalVoltage.V24,
                stdP * 1.2 * 4, 0.000, stdP * 4, stdDischargeTime / 6 * batteryCapacityFactor, stdEfficiency,
                stdBatteryHalfLife, heatTIme, 60.0, -100.0
            )
            desc.setRenderSpec("current")
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0)
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 4
            name = TR_NAME(I18N.Type.NONE, "Life Oriented Battery")
            val desc = BatteryDescriptor(
                name, "BatteryBig", 0.5, true, false, voltageFunction,
                stdU, stdP * 1.2, 0.000, stdP, stdDischargeTime * batteryCapacityFactor, stdEfficiency,
                stdBatteryHalfLife * 8, heatTIme, 60.0, -100.0
            )
            desc.setRenderSpec("life")
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0)
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 5
            name = TR_NAME(I18N.Type.NONE, "Single-use Battery")
            val desc = BatteryDescriptor(
                name, "BatteryBig", 1.0, false, false, voltageFunction,
                stdU, stdP * 1.2 * 2, 0.000, stdP * 2, stdDischargeTime / 4 * batteryCapacityFactor, stdEfficiency,
                stdBatteryHalfLife * 8, heatTIme, 60.0, -100.0
            )
            desc.setRenderSpec("coal")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 6
            name = TR_NAME(I18N.Type.NONE, "Experimental Battery")
            val desc = BatteryDescriptor(
                name, "BatteryBig", 0.5, true, false, voltageFunction,
                NominalVoltage.V120, stdP * 1.2 * 8, 0.025, stdP * 8, stdDischargeTime / 4 * batteryCapacityFactor, stdEfficiency,
                stdBatteryHalfLife * 8, heatTIme, 60.0, -100.0
            )
            desc.setRenderSpec("highvoltage")
            desc.setCurrentDrop(desc.electricalU * 1.2, desc.electricalStdP * 1.0)
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }
}
