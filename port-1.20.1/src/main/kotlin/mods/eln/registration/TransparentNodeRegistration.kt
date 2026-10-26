package mods.eln.registration

import net.minecraft.world.level.Level
import mods.eln.transparentnode.*
import mods.eln.sixnode.electricalcable.UtilityCableDescriptor
import mods.eln.misc.Utils.coalEnergyReference
import mods.eln.misc.SeriesFunction.Companion.newE6
import mods.eln.misc.SeriesFunction.Companion.newE12
import mods.eln.ghost.GhostBlock
import mods.eln.ghost.GhostGroup
import mods.eln.misc.Coordinate
import mods.eln.misc.FunctionTableYProtect
import mods.eln.sim.ThermalLoadInitializer
import mods.eln.sim.ThermalLoadInitializerByPowerDrop
import mods.eln.transparentnode.heatfurnace.HeatFurnaceDescriptor
import mods.eln.transparentnode.powercapacitor.PowerCapacitorDescriptor
import mods.eln.transparentnode.powerinductor.PowerInductorDescriptor
import mods.eln.transparentnode.solarpanel.SolarPanelDescriptor
import mods.eln.transparentnode.thermaldissipatoractive.ThermalDissipatorActiveDescriptor
import mods.eln.transparentnode.thermaldissipatorpassive.ThermalDissipatorPassiveDescriptor
import mods.eln.transparentnode.turbine.TurbineDescriptor
import mods.eln.transparentnode.waterturbine.WaterTurbineDescriptor
import mods.eln.transparentnode.windturbine.WindTurbineDescriptor
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

        registerPowerComponent(1)
        registerTransformer(2)
        registerHeatFurnace(3)
        registerTurbine(4)
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
        registerSolarPanel(48)
        registerWindTurbine(49)
        registerThermalDissipatorPassiveAndActive(64)
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
    private fun registerPowerComponent(id: Int) {
        var subId: Int
        var name: String?

        run {
            subId = 16
            name = TR_NAME(I18N.Type.NONE, "Power inductor")
            val desc = PowerInductorDescriptor(name, null, newE12(-1.0))
            transparentNodeItem.addWithoutRegistry(subId + (id shl 6), desc)
        }

        run {
            subId = 20
            name = TR_NAME(I18N.Type.NONE, "Power capacitor")
            val desc = PowerCapacitorDescriptor(name, null, newE6(-2.0), 300.0)
            transparentNodeItem.addWithoutRegistry(subId + (id shl 6), desc)
        }
    }

    private fun registerTransformer(id: Int) {
        var subId: Int
        var name: String

        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Legacy DC-DC Converter")
            val desc = LegacyDcDcDescriptor(
                name, Eln.obj.getObj("transformator"), Eln.obj.getObj(
                    "feromagneticcorea"
                ), Eln.obj.getObj("transformatorCase"), 0.5f
            )
            transparentNodeItem.addWithoutRegistry(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            name = TR_NAME(I18N.Type.NONE, "Variable DC-DC Converter")
            val desc = VariableDcDcDescriptor(
                name, Eln.obj.getObj("variabledcdc"), Eln.obj.getObj(
                    "feromagneticcorea"
                ), Eln.obj.getObj("transformatorCase")
            )
            desc.setDefaultIcon("variabledc-dcconverter")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 2
            name = TR_NAME(I18N.Type.NONE, "DC-DC Converter")
            val desc = DcDcDescriptor(
                name, Eln.obj.getObj("dcdc"), Eln.obj.getObj(
                    "feromagneticcorea"
                ), Eln.obj.getObj("transformatorCase"), 0.5f
            )
            desc.setDefaultIcon("dc-dcconverter")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 3
            name = TR_NAME(I18N.Type.NONE, "One-way DC-DC Converter")
            val desc = OneWayDcDcDescriptor(
                name, Eln.obj.getObj("onewaydcdc"), Eln.obj.getObj(
                    "feromagneticcorea"
                ), Eln.obj.getObj("transformatorCase"), OneWayDcDcMode.FIXED,
                guiTexture = "onewaydcdc.png"
            )
            desc.setDefaultIcon("onewaydc-dcconverter")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 4
            name = TR_NAME(I18N.Type.NONE, "One-way Boost vDC/DC Converter")
            val desc = OneWayDcDcDescriptor(
                name, Eln.obj.getObj("onewayboostvdcdc"), Eln.obj.getObj(
                    "feromagneticcorea"
                ), Eln.obj.getObj("transformatorCase"), OneWayDcDcMode.BOOST,
                guiTexture = "onewayboostvdcdc.png"
            )
            desc.setDefaultIcon("onewayboostvdcdcconverter")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 5
            name = TR_NAME(I18N.Type.NONE, "One-way Buck vDC/DC Converter")
            val desc = OneWayDcDcDescriptor(
                name, Eln.obj.getObj("onewaybuckvdcdc"), Eln.obj.getObj(
                    "feromagneticcorea"
                ), Eln.obj.getObj("transformatorCase"), OneWayDcDcMode.BUCK,
                guiTexture = "onewaybuckvdcdc.png"
            )
            desc.setDefaultIcon("onewaybuckvdcdcconverter")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 6
            name = TR_NAME(I18N.Type.NONE, "One-way Boost/Buck vDC/DC Converter")
            val desc = OneWayDcDcDescriptor(
                name, Eln.obj.getObj("onewayboostbuckvdcdc"), Eln.obj.getObj(
                    "feromagneticcorea"
                ), Eln.obj.getObj("transformatorCase"), OneWayDcDcMode.BOOST_BUCK,
                guiTexture = "onewayboostbuckvdcdc.png"
            )
            desc.setDefaultIcon("onewayboostbuckvdcdcconverter")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        if (Eln.isDevelopmentRun) {
            run {
                subId = 7
                name = TR_NAME(I18N.Type.NONE, "Isolation Transformer")
                val desc = OneWayDcDcDescriptor(
                    name, Eln.obj.getObj("isolationdcdc"), Eln.obj.getObj(
                        "feromagneticcorea"
                    ), Eln.obj.getObj("transformatorCase"), OneWayDcDcMode.ISOLATION,
                    guiTexture = "isolationdcdc.png"
                )
                desc.setDefaultIcon("isolationtransformer")
                transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
            }
        }
    }

    private fun registerHeatFurnace(id: Int) {
        var subId: Int
        var name: String
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Stone Heat Furnace")
            val desc = HeatFurnaceDescriptor(
                name,
                "stonefurnace",
                4000.0,
                coalEnergyReference * 2 / 3,
                8,
                500.0,
                ThermalLoadInitializerByPowerDrop(780.0, -100.0, 10.0, 2.0)
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        // PORT: Fuel Heat Furnace returns with FuelHeatFurnaceDescriptor.
    }

    private fun registerTurbine(id: Int) {
        var subId: Int
        var name: String?
        val heatTurbinePowerFactor = Eln.config.getDoubleOrElse("balance.generators.heatTurbinePowerFactor", 1.0)

        val TtoU = FunctionTable(doubleArrayOf(0.0, 0.1, 0.85, 1.0, 1.1, 1.15, 1.18, 1.19, 1.25), 8.0 / 5.0)
        val PoutToPin = FunctionTable(
            doubleArrayOf(0.0, 0.2, 0.4, 0.6, 0.8, 1.0, 1.3, 1.8, 2.7),
            8.0 / 5.0
        )

        run {
            subId = 1
            name = TR_NAME(I18N.Type.NONE, "48V Turbine")
            val RsFactor = 0.1
            val nominalU = NominalVoltage.V48
            val nominalP: Double = 1000 * heatTurbinePowerFactor // it was 300 before
            val nominalDeltaT = 250.0
            val desc =
                TurbineDescriptor(
                    name, "turbineb", instance.lowVoltageCableDescriptor.render,
                    TtoU.duplicate(nominalDeltaT, nominalU), PoutToPin.duplicate(nominalP, nominalP), nominalDeltaT,
                    nominalU, nominalP, nominalP / 40, instance.lowVoltageCableDescriptor.electricalRs * RsFactor, 25.0,
                    nominalDeltaT / 40, nominalP / (nominalU / 25), "eln:heat_turbine_50v"
                )
            desc.setDefaultIcon("50vturbine")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 8
            name = TR_NAME(I18N.Type.NONE, "240V Turbine")
            val RsFactor = 0.10
            val nominalU = NominalVoltage.V240
            val nominalP: Double = 2000 * heatTurbinePowerFactor
            val nominalDeltaT = 350.0
            val desc =
                TurbineDescriptor(
                    name, "turbinebblue", instance.meduimVoltageCableDescriptor.render,
                    TtoU.duplicate(nominalDeltaT, nominalU), PoutToPin.duplicate(nominalP, nominalP), nominalDeltaT,
                    nominalU, nominalP, nominalP / 40, instance.meduimVoltageCableDescriptor.electricalRs * RsFactor, 50.0,
                    nominalDeltaT / 40, nominalP / (nominalU / 25), "eln:heat_turbine_200v"
                )
            desc.setDefaultIcon("200vturbine")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        // PORT: Steam Turbine returns with SteamTurbineDescriptor.

        // PORT: Generator returns with GeneratorDescriptor.

        // PORT: Gas Turbine returns with GasTurbineDescriptor.

        // PORT: Joint returns with StraightJointDescriptor.

        // PORT: Joint hub returns with VerticalHubDescriptor.

        // PORT: Flywheel returns with FlywheelDescriptor.

        // PORT: Tachometer returns with TachometerDescriptor.

        // PORT: Shaft Motor returns with MotorDescriptor.

        // PORT: Clutch returns with ClutchDescriptor.

        // PORT: Fixed Shaft returns with FixedShaftDescriptor.

        // PORT: Radial Motor returns with RadialMotorDescriptor.
        // PORT: Crank Shaft returns with CrankableShaftDescriptor.
        // PORT: Rolling Shaft  returns with RollingShaftMachineDescriptor.

        // PORT: Large Steam Turbine returns with SteamTurbineDescriptor.

        // PORT: Large Gas Turbine returns with GasTurbineDescriptor.

        // PORT: Large Generator returns with GeneratorDescriptor.

        // PORT: Large Shaft Motor returns with MotorDescriptor.

        // PORT: Polarized Shaft Generator returns with GeneratorDescriptor.

        // PORT: Polarized Shaft Motor returns with MotorDescriptor.

        // PORT: Large Flywheel returns with FlywheelDescriptor.
    }

    private fun registerSolarPanel(id: Int) {
        var subId: Int
        var ghostGroup: GhostGroup
        var name: String?
        val solarPanelPowerFactor = Eln.config.getDoubleOrElse("balance.generators.solarPanelPowerFactor", 1.0)
        val smallSolarVoc = 37.0
        val smallSolarVmp = 30.0
        val smallSolarImp = 8.0 * solarPanelPowerFactor
        val smallSolarIsc = 8.6 * solarPanelPowerFactor
        val largeSeriesPanels = 3.0
        val largeParallelPanels = 2.0
        val largeSolarVoc = smallSolarVoc * largeSeriesPanels
        val largeSolarVmp = smallSolarVmp * largeSeriesPanels
        val largeSolarImp = smallSolarImp * largeParallelPanels
        val largeSolarIsc = smallSolarIsc * largeParallelPanels
        val smallSolarCable = UtilityCableDescriptor.allDescriptors()
            .first { it.name == "Copper 16 AWG Cable 300V" }
        val largeSolarCable = UtilityCableDescriptor.allDescriptors()
            .first { it.name == "Copper 12 AWG Cable 600V" }

        run {
            subId = 1
            name = TR_NAME(I18N.Type.NONE, "Small Solar Panel")
            ghostGroup = GhostGroup()
            val desc = SolarPanelDescriptor(
                name,
                Eln.obj.getObj("smallsolarpannel"),
                smallSolarCable.render,
                ghostGroup,
                0,
                1,
                0,
                null,
                smallSolarVoc,
                smallSolarVmp,
                smallSolarImp,
                smallSolarIsc,
                0.01,
                Math.PI / 2,
                Math.PI / 2
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 2
            name = TR_NAME(I18N.Type.NONE, "Small Rotating Solar Panel")
            ghostGroup = GhostGroup()
            val desc = SolarPanelDescriptor(
                name, Eln.obj.getObj("smallsolarpannelrot"),
                smallSolarCable.render, ghostGroup, 0, 1, 0, null, smallSolarVoc,
                smallSolarVmp, smallSolarImp, smallSolarIsc, 0.01, Math.PI / 4, Math.PI / 4 * 3
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 3
            name = TR_NAME(I18N.Type.NONE, "2x3 Solar Panel")
            val groundCoordinate = Coordinate(1, 0, 0, Level.OVERWORLD)
            ghostGroup = GhostGroup()
            ghostGroup.addRectangle(0, 1, 0, 0, -1, 1)
            ghostGroup.removeElement(0, 0, 0)
            val desc = SolarPanelDescriptor(
                name, Eln.obj.getObj("bigSolarPanel"),
                largeSolarCable.render, ghostGroup, 1, 1, 0, groundCoordinate, largeSolarVoc,
                largeSolarVmp, largeSolarImp, largeSolarIsc, 0.01, Math.PI / 2, Math.PI / 2
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 4
            name = TR_NAME(I18N.Type.NONE, "2x3 Rotating Solar Panel")
            val groundCoordinate = Coordinate(1, 0, 0, Level.OVERWORLD)
            ghostGroup = GhostGroup()
            ghostGroup.addRectangle(0, 1, 0, 0, -1, 1)
            ghostGroup.removeElement(0, 0, 0)
            val desc = SolarPanelDescriptor(
                name,
                Eln.obj.getObj("bigSolarPanelrot"),
                largeSolarCable.render,
                ghostGroup,
                1,
                1,
                1,
                groundCoordinate,
                largeSolarVoc,
                largeSolarVmp,
                largeSolarImp,
                largeSolarIsc,
                0.01,
                Math.PI / 8 * 3,
                Math.PI / 8 * 5
            )
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerWindTurbine(id: Int) {
        var subId: Int
        var name: String?
        val windTurbinePowerFactor = Eln.config.getDoubleOrElse("balance.generators.windTurbinePowerFactor", 1.0)
        val windTurbineCable = UtilityCableDescriptor.allDescriptors()
            .first { it.name == "Copper 14 AWG Cable 600V" }

        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Wind Turbine")

            val desc = WindTurbineDescriptor(
                name,
                Eln.obj.getObj("WindTurbineMini"),
                windTurbineCable,
                2500.0 * windTurbinePowerFactor,
                12.0,
                NominalVoltage.V240,
                NominalVoltage.V240 * 1.25,
                22.0,
                3,
                7,
                2,
                2,
                2,
                0.07,
                "eln:WINDTURBINE_BIG_SF",
                1f
            )

            val g = GhostGroup()
            g.addElement(0, 1, 0)
            g.addElement(0, 2, -1)
            g.addElement(0, 2, 1)
            g.addElement(0, 3, -1)
            g.addElement(0, 3, 1)
            g.addRectangle(0, 0, 1, 3, 0, 0)
            desc.setGhostGroup(g)
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        /*{ //TODO Work on the large wind turbine
        subId = 1;
        name = TR_NAME(Type.NONE, "Large Wind Turbine");

        WindTurbineDescriptor desc = new WindTurbineDescriptor(
            name, obj.getObj("WindTurbineMini"), // name,Obj3D obj,
            lowVoltageCableDescriptor,// ElectricalCableDescriptor
            // cable,
            PfW,// PfW
            160 * windTurbinePowerFactor, 10,// double nominalPower,double nominalWind,
            LVU * 1.18, 22,// double maxVoltage, double maxWind,
            3,// int offY,
            7, 2, 2,// int rayX,int rayY,int rayZ,
            2, 0.07,// int blockMalusMinCount,double blockMalus
            "eln:WINDTURBINE_BIG_SF", 1f // Use the wind turbine sound and play at normal volume (1 => 100%)
        );

        GhostGroup g = new GhostGroup();
        g.addElement(0, 1, 0);
        g.addElement(0, 2, -1);
        g.addElement(0, 2, 1);
        g.addElement(0, 3, -1);
        g.addElement(0, 3, 1);
        g.addRectangle(0, 0, 1, 3, 0, 0);
        desc.setGhostGroup(g);
        transparentNodeItem.addDescriptor(subId + (id << 6), desc);
    } */
        run {
            subId = 16
            name = TR_NAME(I18N.Type.NONE, "Water Turbine")
            val waterCoord = Coordinate(1, -1, 0, Level.OVERWORLD)
            val desc = WaterTurbineDescriptor(
                name,
                Eln.obj.getObj("SmallWaterWheel"),
                instance.lowVoltageCableDescriptor,
                30 * Eln.config.getDoubleOrElse("balance.generators.waterTurbinePowerFactor", 1.0),
                NominalVoltage.V24 * 1.18,
                waterCoord,
                "eln:water_turbine",
                1f
            )

            val g = GhostGroup()
            g.addRectangle(1, 1, 0, 1, -1, 1)
            desc.ghostGroup = g
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }

    private fun registerThermalDissipatorPassiveAndActive(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            name = TR_NAME(I18N.Type.NONE, "Small Passive Thermal Dissipator")
            val desc = ThermalDissipatorPassiveDescriptor(
                name, Eln.obj.getObj(
                    "passivethermaldissipatora"
                ), 200.0, -100.0, 250.0, 30.0, 10.0, 1.0
            )
            desc.setDefaultIcon("smallpassivethermaldissipator")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 32
            name = TR_NAME(I18N.Type.NONE, "24V Small Active Thermal Dissipator")
            val desc = ThermalDissipatorActiveDescriptor(
                name, Eln.obj.getObj(
                    "activethermaldissipatora"
                ), NominalVoltage.V24, 50.0, 800.0, instance.lowVoltageCableDescriptor, 130.0, -100.0, 200.0, 30.0, 10.0, 1.0
            )
            desc.setDefaultIcon("smallactivethermaldissipator")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 34
            name = TR_NAME(I18N.Type.NONE, "240V Active Thermal Dissipator")
            val desc = ThermalDissipatorActiveDescriptor(
                name, Eln.obj.getObj(
                    "200vactivethermaldissipatora"
                ), NominalVoltage.V240, 60.0, 1200.0, instance.meduimVoltageCableDescriptor, 130.0, -100.0, 200.0, 30.0,
                10.0, 1.0
            )
            desc.setDefaultIcon("200vactivethermaldissipator")
            transparentNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
    }
}
