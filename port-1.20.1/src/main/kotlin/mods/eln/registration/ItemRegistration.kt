package mods.eln.registration

import mods.eln.item.FalstadImportToolDescriptor
import mods.eln.item.electricalitem.PortableOreScannerItem
import mods.eln.railroad.ElectricMinecartItem
import mods.eln.item.NopeWandDescriptor
import mods.eln.item.electricalitem.ElectricalAxe
import mods.eln.item.electricalitem.BatteryItem
import mods.eln.item.electricalitem.ElectricalPickaxe
import mods.eln.item.electricalitem.ElectricalFlashlightItem
import mods.eln.sixnode.wirelesssignal.WirelessSignalAnalyserItemDescriptor
import mods.eln.item.OverHeatingProtectionDescriptor
import mods.eln.item.OverVoltageProtectionDescriptor
import mods.eln.mechanical.ClutchPinItem
import mods.eln.mechanical.ClutchPlateItem
import mods.eln.item.SolarTrackerDescriptor
import mods.eln.item.CaseItemDescriptor
import mods.eln.item.DielectricItem
import mods.eln.item.MachineBoosterDescriptor
import mods.eln.item.ElectricalDrillDescriptor
import mods.eln.item.MiningPipeDescriptor
import mods.eln.item.OreScanner
import mods.eln.item.EntitySensorFilterDescriptor
import mods.eln.sixnode.electricaldatalogger.DataLogsPrintDescriptor
import mods.eln.item.BrushDescriptor
import mods.eln.Eln
import mods.eln.Eln.instance
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.generic.GenericItemUsingDamageDescriptorWithComment
import mods.eln.i18n.I18N
import mods.eln.i18n.I18N.TR_NAME
import mods.eln.i18n.I18N.tr
import mods.eln.item.*
import mods.eln.item.lampitem.LampDescriptor
import mods.eln.item.lampitem.LampLists
import mods.eln.item.lampitem.SpecificLampData
import mods.eln.item.regulator.IRegulatorDescriptor
import mods.eln.item.regulator.RegulatorAnalogDescriptor
import mods.eln.item.regulator.RegulatorOnOffDescriptor
import mods.eln.misc.NominalVoltage

// Ported subset of the 1.7.10 ItemRegistration; the other families return as their descriptors are ported.
object ItemRegistration {
    private fun mapSharedGroup(id: Int, tab: mods.eln.generic.GenericCreativeTab) {
        Eln.sharedItem.setCreativeTabForGroup(id, tab)
        // PORT: and Eln.sharedItemStackOne once single-stack items are ported.
    }

    private fun <T : GenericItemUsingDamageDescriptor> T.inOresAndMaterialsTab() = apply {
        creativeTab = Eln.creativeTabOresMaterials
    }

    private fun <T : GenericItemUsingDamageDescriptor> T.inToolsAndArmorTab() = apply {
        creativeTab = Eln.creativeTabToolsArmor
    }

    fun registerItem() {
        mapSharedGroup(1, Eln.creativeTabMachines)
        mapSharedGroup(3, Eln.creativeTabSignalProcessing)
        mapSharedGroup(4, Eln.creativeTabLighting)
        mapSharedGroup(5, Eln.creativeTabOresMaterials)
        mapSharedGroup(6, Eln.creativeTabOresMaterials)
        mapSharedGroup(7, Eln.creativeTabOresMaterials)
        mapSharedGroup(8, Eln.creativeTabOresMaterials)
        mapSharedGroup(9, Eln.creativeTabOresMaterials)
        mapSharedGroup(10, Eln.creativeTabOresMaterials)
        mapSharedGroup(11, Eln.creativeTabPowerElectronics)
        mapSharedGroup(14, Eln.creativeTabToolsArmor)
        mapSharedGroup(15, Eln.creativeTabMachines)
        mapSharedGroup(16, Eln.creativeTabMachines)
        mapSharedGroup(17, Eln.creativeTabMachines)
        mapSharedGroup(64, Eln.creativeTabOresMaterials)
        mapSharedGroup(65, Eln.creativeTabOresMaterials)
        mapSharedGroup(69, Eln.creativeTabOresMaterials)
        mapSharedGroup(119, Eln.creativeTabToolsArmor)
        mapSharedGroup(120, Eln.creativeTabMachines)
        mapSharedGroup(121, Eln.creativeTabToolsArmor)
        mapSharedGroup(122, Eln.creativeTabToolsArmor)
        mapSharedGroup(123, Eln.creativeTabToolsArmor)
        mapSharedGroup(124, Eln.creativeTabPowerElectronics)
        mapSharedGroup(18, Eln.creativeTabMachines)
        mapSharedGroup(126, Eln.creativeTabOresMaterials)
        mapSharedGroup(127, Eln.creativeTabToolsArmor)
        mapSharedGroup(118, Eln.creativeTabMachines)

        registerHeatingCorp(1)
        registerRegulatorItem(3)
        registerLampItem(4)
        registerProtection(5)
        registerCombustionChamber(6)
        registerFerromagneticCore(7)
        registerIngot(8)
        registerDust(9)
        registerElectricalMotor(10)
        registerSolarTracker(11)
        registerMeter(14)
        registerElectricalDrill(15)
        registerOreScanner(16)
        registerMiningPipe(17)
        registerTreeResinAndRubber(64)
        registerRawCable(65)
        registerArc(69)
        registerBrush(119)
        registerMiscItem(120)
        registerElectricalTool(121)
        registerPortableItem(122)
        registerFuelBurnerItem(124)
        registerTurbineBladeItems(18)
        registerBasicItems(126)
        registerElectricMinecartItems(127)
        // PORT: registerWireToolItems(123)
        // PORT: registerWireMachineItems(118)
        // Copper armor and tools are registered by CopperGear, the E-coal armor by ElectricalArmor, the ores by ElnOre.
    }

    private fun registerHeatingCorp(id: Int) {
        var subId: Int
        var completId: Int

        var element: HeatingCorpElement
        run {
            subId = 0
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "Small 48V Copper Heating Element"),
                NominalVoltage.V48,
                150.0,
                190.0,
                instance.lowVoltageCableDescriptor
            )
            element.setDefaultIcon("small50vcopperheatingelement")
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "48V Copper Heating Element"),
                NominalVoltage.V48,
                250.0,
                320.0,
                instance.lowVoltageCableDescriptor
            )
            element.setDefaultIcon("50vcopperheatingelement")
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 2
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "Small 240V Copper Heating Element"),
                NominalVoltage.V240,
                400.0,
                500.0,
                instance.meduimVoltageCableDescriptor
            )
            element.setDefaultIcon("small200vcopperheatingelement")
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 3
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "240V Copper Heating Element"),
                NominalVoltage.V240,
                600.0,
                750.0,
                instance.highVoltageCableDescriptor
            )
            element.setDefaultIcon("200vcopperheatingelement")
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 4
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "Small 48V Iron Heating Element"),
                NominalVoltage.V48,
                180.0,
                225.0,
                instance.lowVoltageCableDescriptor
            )
            element.setDefaultIcon("small50vironheatingelement")
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 5
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "48V Iron Heating Element"),
                NominalVoltage.V48,
                375.0,
                480.0,
                instance.lowVoltageCableDescriptor
            )
            element.setDefaultIcon("50vironheatingelement")
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 6
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "Small 240V Iron Heating Element"),
                NominalVoltage.V240,
                600.0,
                750.0,
                instance.meduimVoltageCableDescriptor
            )
            element.setDefaultIcon("small200vironheatingelement")
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 7
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "240V Iron Heating Element"),
                NominalVoltage.V240,
                900.0,
                1050.0,
                instance.highVoltageCableDescriptor
            )
            element.setDefaultIcon("200vironheatingelement")
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 8
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "Small 48V Tungsten Heating Element"),
                NominalVoltage.V48,
                240.0,
                300.0,
                instance.lowVoltageCableDescriptor
            )
            element.setDefaultIcon("small50vtungstenheatingelement")
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 9
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "48V Tungsten Heating Element"),
                NominalVoltage.V48,
                500.0,
                640.0,
                instance.lowVoltageCableDescriptor
            )
            element.setDefaultIcon("50vtungstenheatingelement")
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 10
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "Small 240V Tungsten Heating Element"),
                NominalVoltage.V240,
                800.0,
                1000.0,
                instance.meduimVoltageCableDescriptor
            )
            element.setDefaultIcon("small200vtungstenheatingelement")
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 11
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "240V Tungsten Heating Element"),
                NominalVoltage.V240,
                1200.0,
                1500.0,
                instance.highVoltageCableDescriptor
            )
            element.setDefaultIcon("200vtungstenheatingelement")
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 12
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "Small 480V Tungsten Heating Element"),
                NominalVoltage.V480,
                3600.0,
                4800.0,
                instance.veryHighVoltageCableDescriptor
            )
            element.setDefaultIcon("small800vtungstenheatingelement")
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 13
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "480V Tungsten Heating Element"),
                NominalVoltage.V480,
                4812.0,
                6015.0,
                instance.veryHighVoltageCableDescriptor
            )
            element.setDefaultIcon("800vtungstenheatingelement")
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 14
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "Small 480V Heavy Tungsten Heating Element"),
                NominalVoltage.V480,
                4000.0,
                6000.0,
                instance.veryHighVoltageCableDescriptor
            )
            element.setDefaultIcon("small3.2kvtungstenheatingelement")
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 15
            completId = subId + (id shl 6)
            element = HeatingCorpElement(
                I18N.TR_NAME(I18N.Type.NONE, "480V Heavy Tungsten Heating Element"),
                NominalVoltage.V480,
                12000.0,
                15000.0,
                instance.veryHighVoltageCableDescriptor
            )
            element.setDefaultIcon("3.2kvtungstenheatingelement")
            Eln.sharedItem.addElement(completId, element)
        }
    }

    private fun registerRegulatorItem(id: Int) {
        var subId: Int
        var completId: Int
        var element: IRegulatorDescriptor
        run {
            subId = 0
            completId = subId + (id shl 6)
            element = RegulatorOnOffDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "On/OFF Regulator 1 Percent"), "onoffregulator",
                0.01
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            element = RegulatorOnOffDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "On/OFF Regulator 10 Percent"),
                "onoffregulator",
                0.1
            )
            Eln.sharedItem.addElement(completId, element)
        }

        run {
            subId = 8
            completId = subId + (id shl 6)
            element = RegulatorAnalogDescriptor(
                I18N.TR_NAME(
                    I18N.Type.NONE,
                    "Analogic Regulator"
                ), "Analogicregulator"
            )
            Eln.sharedItem.addElement(completId, element)
        }
    }

    private fun registerLampItem(id: Int) {
        var subId: Int
        var completeId: Int
        var descriptor: LampDescriptor

        run {
            subId = 0
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "12V Incandescent Light Bulb"), "incandescentironlamp",
                SpecificLampData(LampLists.getLampData("incandescent")!!, NominalVoltage.V12)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }

        run {
            subId = 1
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "120V Incandescent Light Bulb"), "incandescentironlamp",
                SpecificLampData(LampLists.getLampData("incandescent")!!, NominalVoltage.V120)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }

        run {
            subId = 2
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "240V Incandescent Light Bulb"), "incandescentironlamp",
                SpecificLampData(LampLists.getLampData("incandescent")!!, NominalVoltage.V240)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }

        run {
            subId = 4
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "12V Carbon Light Bulb"), "incandescentcarbonlamp",
                SpecificLampData(LampLists.getLampData("carbon")!!, NominalVoltage.V12)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }

        run {
            subId = 5
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "120V Carbon Light Bulb"), "incandescentcarbonlamp",
                SpecificLampData(LampLists.getLampData("carbon")!!, NominalVoltage.V120)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }

        run {
            subId = 6
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "240V Carbon Light Bulb"), "incandescentcarbonlamp",
                SpecificLampData(LampLists.getLampData("carbon")!!, NominalVoltage.V240)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }

        run {
            subId = 16
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "12V Fluorescent Light Bulb"), "fluorescentlamp",
                SpecificLampData(LampLists.getLampData("fluorescent")!!, NominalVoltage.V12)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }

        run {
            subId = 17
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "120V Fluorescent Light Bulb"), "fluorescentlamp",
                SpecificLampData(LampLists.getLampData("fluorescent")!!, NominalVoltage.V120)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }

        run {
            subId = 18
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "240V Fluorescent Light Bulb"), "fluorescentlamp",
                SpecificLampData(LampLists.getLampData("fluorescent")!!, NominalVoltage.V240)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }

        run {
            subId = 31
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "12V Farming Light Bulb"), "farminglamp",
                SpecificLampData(LampLists.getLampData("farming")!!, NominalVoltage.V12)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }

        run {
            subId = 32
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "120V Farming Light Bulb"), "farminglamp",
                SpecificLampData(LampLists.getLampData("farming")!!, NominalVoltage.V120)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }

        run {
            subId = 36
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "240V Farming Light Bulb"), "farminglamp",
                SpecificLampData(LampLists.getLampData("farming")!!, NominalVoltage.V240)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }

        run {
            subId = 39
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "12V LED Light Bulb"), "ledlamp",
                SpecificLampData(LampLists.getLampData("led")!!, NominalVoltage.V12)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }

        run {
            subId = 37
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "120V LED Light Bulb"), "ledlamp",
                SpecificLampData(LampLists.getLampData("led")!!, NominalVoltage.V120)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }

        run {
            subId = 38
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "240V LED Light Bulb"), "ledlamp",
                SpecificLampData(LampLists.getLampData("led")!!, NominalVoltage.V240)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }

        run {
            subId = 43
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "12V Halogen Light Bulb"), "halogenlamp",
                SpecificLampData(LampLists.getLampData("halogen")!!, NominalVoltage.V12)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }

        run {
            subId = 44
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "120V Halogen Light Bulb"), "halogenlamp",
                SpecificLampData(LampLists.getLampData("halogen")!!, NominalVoltage.V120)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }

        run {
            subId = 45
            completeId = subId + (id shl 6)
            descriptor = LampDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "240V Halogen Light Bulb"), "halogenlamp",
                SpecificLampData(LampLists.getLampData("halogen")!!, NominalVoltage.V240)
            )
            Eln.sharedItem.addElement(completeId, descriptor)
        }
    }

    private fun registerCombustionChamber(id: Int) {
        var subId: Int
        var completId: Int
        run {
            subId = 0
            completId = subId + (id shl 6)
            val element = CombustionChamber(I18N.TR_NAME(I18N.Type.NONE, "Combustion Chamber"))
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            val element = ThermalIsolatorElement(
                I18N.TR_NAME(I18N.Type.NONE, "Thermal Insulation"),
                0.5,
                500.0
            )
            Eln.sharedItem.addElement(completId, element)
        }
    }

    private fun registerFerromagneticCore(id: Int) {
        var subId: Int
        var completId: Int

        var element: FerromagneticCoreDescriptor
        run {
            subId = 0
            completId = subId + (id shl 6)
            element = FerromagneticCoreDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Cheap Ferromagnetic Core"), Eln.obj.getObj(
                    "feromagneticcorea"
                ), 100.0
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            element = FerromagneticCoreDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Average Ferromagnetic Core"), Eln.obj.getObj(
                    "feromagneticcorea"
                ), 50.0
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 2
            completId = subId + (id shl 6)
            element = FerromagneticCoreDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Optimal Ferromagnetic Core"), Eln.obj.getObj(
                    "feromagneticcorea"
                ), 1.0
            )
            Eln.sharedItem.addElement(completId, element)
        }
    }

    private fun registerIngot(id: Int) {
        var subId: Int
        var completId: Int
        var name: String?

        var element: GenericItemUsingDamageDescriptorWithComment

        run {
            subId = 1
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Copper Ingot")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(completId, element)
            instance.copperIngot = element
            Eln.addToOre("ingotCopper", element)
        }

        run {
            subId = 4
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Lead Ingot")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(completId, element)
            instance.plumbIngot = element
            Eln.addToOre("ingotLead", element)
        }

        run {
            subId = 5
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Tungsten Ingot")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(completId, element)
            instance.tungstenIngot = element
            Eln.addToOre(Eln.config.getStringOrElse("runtime.dictionary.tungstenIngot", "ingotElnTungsten"), element)
        }

        run {
            subId = 6
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Ferrite Ingot")
            element = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf(tr("useless"), tr("Really useless"))
            )
            Eln.sharedItem.addElement(completId, element)
            Eln.addToOre("ingotFerrite", element)
        }

        run {
            subId = 7
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Alloy Ingot")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(completId, element)
            Eln.addToOre("ingotAlloy", element)
        }

        run {
            subId = 8
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Mercury")
            element =
                GenericItemUsingDamageDescriptorWithComment(name, arrayOf(tr("useless"), tr("miaou")))
            Eln.sharedItem.addElement(completId, element)
            Eln.addToOre("quicksilver", element)
        }
    }

    private fun registerDust(id: Int) {
        var currentId = id
        // TODO: Breaking change! We should really not be modifying ID. When we re-assign these to be subId, it will break existing registered items
        var subId: Int
        var completId: Int
        var name: String?
        var element: GenericItemUsingDamageDescriptorWithComment

        run {
            subId = 1
            completId = subId + (currentId shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Copper Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.dustCopper = element
            Eln.sharedItem.addElement(completId, element)
            Eln.addToOre("dustCopper", element)
        }
        run {
            subId = 2
            completId = subId + (currentId shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Iron Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.dustCopper = element
            Eln.sharedItem.addElement(completId, element)
            Eln.addToOre("dustIron", element)
        }
        run {
            subId = 3
            completId = subId + (currentId shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Lapis Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.dustCopper = element
            Eln.sharedItem.addElement(completId, element)
            Eln.addToOre("dustLapis", element)
        }
        run {
            subId = 4
            completId = subId + (currentId shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Diamond Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.dustCopper = element
            Eln.sharedItem.addElement(completId, element)
            Eln.addToOre("dustDiamond", element)
        }

        run {
            currentId = 5
            name = I18N.TR_NAME(I18N.Type.NONE, "Lead Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(currentId, element)
            Eln.addToOre("dustLead", element)
        }
        run {
            currentId = 6
            name = I18N.TR_NAME(I18N.Type.NONE, "Tungsten Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(currentId, element)
            Eln.addToOre(Eln.config.getStringOrElse("runtime.dictionary.tungstenDust", "dustElnTungsten"), element)
        }

        run {
            currentId = 7
            name = I18N.TR_NAME(I18N.Type.NONE, "Gold Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(currentId, element)
            Eln.addToOre("dustGold", element)
        }

        run {
            currentId = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "Coal Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(currentId, element)
            Eln.addToOre("dustCoal", element)
        }
        run {
            currentId = 9
            name = I18N.TR_NAME(I18N.Type.NONE, "Alloy Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(currentId, element)
            Eln.addToOre("dustAlloy", element)
        }

        run {
            currentId = 10
            name = I18N.TR_NAME(I18N.Type.NONE, "Cinnabar Dust")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(currentId, element)
            Eln.addToOre("dustCinnabar", element)
        }
    }

    private fun registerElectricalMotor(id: Int) {
        var subId: Int
        var completId: Int
        var name: String?
        var element: GenericItemUsingDamageDescriptorWithComment

        run {
            subId = 0
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Electrical Motor")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Advanced Electrical Motor")
            element = GenericItemUsingDamageDescriptorWithComment(name, arrayOf())
            Eln.sharedItem.addElement(completId, element)
        }
    }

    private fun registerMeter(id: Int) {
        var subId: Int
        var completId: Int

        var element: GenericItemUsingDamageDescriptor
        run {
            subId = 0
            completId = subId + (id shl 6)
            element =
                GenericItemUsingDamageDescriptor(I18N.TR_NAME(I18N.Type.NONE, "MultiMeter"))
            Eln.sharedItem.addElement(completId, element)
            Eln.multiMeterElement = element
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            element = ThermometerDescriptor(
                I18N.TR_NAME(
                    I18N.Type.NONE,
                    "Thermometer"
                )
            )
            Eln.sharedItem.addElement(completId, element)
            Eln.thermometerElement = element
        }
        run {
            subId = 2
            completId = subId + (id shl 6)
            element =
                GenericItemUsingDamageDescriptor(I18N.TR_NAME(I18N.Type.NONE, "AllMeter"))
            Eln.sharedItem.addElement(completId, element)
            Eln.allMeterElement = element
        }
        run {
            subId = 8
            completId = subId + (id shl 6)
            element = WirelessSignalAnalyserItemDescriptor(I18N.TR_NAME(I18N.Type.NONE, "Wireless Analyser"))
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 16
            completId = subId + (id shl 6)
            element =
                ConfigCopyToolDescriptor(I18N.TR_NAME(I18N.Type.NONE, "Config Copy Tool"))
            Eln.sharedItem.addElement(completId, element)
            Eln.configCopyToolElement = element
        }
        run {
            subId = 17
            completId = subId + (id shl 6)
            element = FalstadImportToolDescriptor(I18N.TR_NAME(I18N.Type.NONE, "Falstad Import Tool"))
            element.creativeTab = Eln.creativeTabToolsArmor
            Eln.sharedItem.addElement(completId, element)
        }
    }

    private fun registerTreeResinAndRubber(id: Int) {
        var subId: Int
        var completId: Int
        var name: String

        run {
            subId = 0
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Tree Resin")
            val descriptor = TreeResin(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Eln.treeResin = descriptor
            Eln.addToOre("materialResin", descriptor)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Rubber")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Eln.addToOre("itemRubber", descriptor)
        }
    }

    private fun registerRawCable(id: Int) {
        var subId: Int
        var completId: Int
        var name: String

        run {
            subId = 0
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Copper Cable")
            instance.copperCableDescriptor = CopperCableDescriptor(name)
            Eln.sharedItem.addElement(completId, instance.copperCableDescriptor!!)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Iron Cable")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
        }
        run {
            subId = 2
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Tungsten Cable")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
        }
    }

    private fun registerArc(id: Int) {
        var subId: Int
        var completId: Int
        var name: String

        run {
            subId = 0
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Graphite Rod")
            val graphiteDescriptor = GraphiteDescriptor(name)
            Eln.sharedItem.addElement(completId, graphiteDescriptor)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "2x Graphite Rods")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
        }
        run {
            subId = 2
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "3x Graphite Rods")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
        }
        run {
            subId = 3
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "4x Graphite Rods")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
        }
        run {
            subId = 4
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Synthetic Diamond")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
        }
        run {
            subId = 5
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "unreleasedium")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
        }
        run {
            subId = 6
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Arc Clay Ingot")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Eln.addToOre("ingotAluminum", descriptor)
            Eln.addToOre("ingotAluminium", descriptor)
        }
        run {
            subId = 7
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Arc Metal Ingot")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Eln.addToOre("ingotSteel", descriptor)
        }
        run {
            subId = 8
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Inert Canister")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
        }
        /*{
        GenericItemUsingDamageDescriptor descriptor;
        subId = 9;
        completId = subId + (id << 6);
        name = TR_NAME(Type.NONE, "T1 Transmission Cable");

        descriptor = new GenericItemUsingDamageDescriptor(name);
        sharedItem.addElement(completId, descriptor);
        Data.addResource(descriptor.newItemStack());
    }
    {
        GenericItemUsingDamageDescriptor descriptor;
        subId = 10;
        completId = subId + (id << 6);
        name = TR_NAME(Type.NONE, "T2 Transmission Cable");

        descriptor = new GenericItemUsingDamageDescriptor(name);
        sharedItem.addElement(completId, descriptor);
        Data.addResource(descriptor.newItemStack());
    }*/
        run {
            subId = 11
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Canister of Water")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
        }
        run {
            subId = 12
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Canister of Arc Water")
            val descriptor = GenericItemUsingDamageDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
        }
    }

    private fun registerBrush(id: Int) {
        var subId: Int

        var name: String
        val subNames = arrayOf(
            I18N.TR_NAME(I18N.Type.NONE, "Black Brush"),
            I18N.TR_NAME(I18N.Type.NONE, "Red Brush"),
            I18N.TR_NAME(
                I18N.Type.NONE,
                "Green Brush"
            ),
            I18N.TR_NAME(I18N.Type.NONE, "Brown Brush"),
            I18N.TR_NAME(I18N.Type.NONE, "Blue Brush"),
            I18N.TR_NAME(
                I18N.Type.NONE,
                "Purple Brush"
            ),
            I18N.TR_NAME(I18N.Type.NONE, "Cyan Brush"),
            I18N.TR_NAME(I18N.Type.NONE, "Silver Brush"),
            I18N.TR_NAME(
                I18N.Type.NONE,
                "Gray Brush"
            ),
            I18N.TR_NAME(I18N.Type.NONE, "Pink Brush"),
            I18N.TR_NAME(I18N.Type.NONE, "Lime Brush"),
            I18N.TR_NAME(
                I18N.Type.NONE, "Yellow" +
                        " Brush"
            ),
            I18N.TR_NAME(I18N.Type.NONE, "Light Blue Brush"),
            I18N.TR_NAME(I18N.Type.NONE, "Magenta Brush"),
            I18N.TR_NAME(
                I18N.Type.NONE,
                "Orange Brush"
            ),
            I18N.TR_NAME(I18N.Type.NONE, "White Brush")
        )
        for (idx in 0..15) {
            subId = idx
            name = subNames[idx]
            val desc = BrushDescriptor(name)
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Eln.whiteDesc = desc
            Eln.brushSubNames = subNames.toList()
        }
    }

    private fun registerSolarTracker(id: Int) {
        var subId: Int
        var completId: Int

        var element: SolarTrackerDescriptor
        run {
            subId = 0
            completId = subId + (id shl 6)
            element = SolarTrackerDescriptor(I18N.TR_NAME(I18N.Type.NONE, "Solar Tracker"))
            Eln.sharedItem.addElement(completId, element)
        }
    }

    private fun registerTurbineBladeItems(id: Int) {
        // sharedItemStackOne and not sharedItem because blades store durability in NBT, stacking would corrupt it.
        Eln.sharedItemStackOne.addElement(
            0 + (id shl 6), mods.eln.item.TurbineBladeDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Iron Turbine Blade"),
                tierName = "iron",
                temperatureResistance = 0.8,
                corrosionResistance = 0.8,
                tierDescription = tr("Suitable for mild, clean fuels")
            )
        )
        Eln.sharedItemStackOne.addElement(
            1 + (id shl 6), mods.eln.item.TurbineBladeDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Steel Turbine Blade"),
                tierName = "steel",
                temperatureResistance = 1.2,
                corrosionResistance = 1.2,
                tierDescription = tr("Handles moderate conditions")
            )
        )
        Eln.sharedItemStackOne.addElement(
            2 + (id shl 6), mods.eln.item.TurbineBladeDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Alloy Turbine Blade"),
                tierName = "alloy",
                temperatureResistance = 2.0,
                corrosionResistance = 2.0,
                tierDescription = tr("Suitable for most fuel types")
            )
        )
        Eln.sharedItemStackOne.addElement(
            3 + (id shl 6), mods.eln.item.TurbineBladeDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Tungsten Turbine Blade"),
                tierName = "tungsten",
                temperatureResistance = 3.5,
                corrosionResistance = 3.5,
                tierDescription = tr("Handles extreme conditions")
            )
        )
    }

    private fun registerFuelBurnerItem(id: Int) {
        val fuelHeatFurnacePowerFactor = Eln.config.getDoubleOrElse("balance.generators.fuelHeatFurnacePowerFactor", 1.0)
        Eln.sharedItemStackOne.addElement(
            0 + (id shl 6), FuelBurnerDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Small Fuel Burner"),
                5000 * fuelHeatFurnacePowerFactor, 2, 1.6f
            )
        )
        Eln.sharedItemStackOne.addElement(
            1 + (id shl 6), FuelBurnerDescriptor(
                I18N.TR_NAME(
                    I18N.Type.NONE,
                    "Medium Fuel Burner"
                ), 10000 * fuelHeatFurnacePowerFactor, 1, 1.4f
            )
        )
        Eln.sharedItemStackOne.addElement(
            2 + (id shl 6), FuelBurnerDescriptor(
                I18N.TR_NAME(I18N.Type.NONE, "Big Fuel Burner"),
                25000 * fuelHeatFurnacePowerFactor, 0, 1f
            )
        )
    }

    private fun registerProtection(id: Int) {
        var subId: Int
        var completId: Int

        run {
            subId = 0
            completId = subId + (id shl 6)
            val element = OverHeatingProtectionDescriptor(
                I18N.TR_NAME(
                    I18N.Type.NONE,
                    "Overheating Protection"
                )
            )
            Eln.sharedItem.addElement(completId, element)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            val element = OverVoltageProtectionDescriptor(
                I18N.TR_NAME(
                    I18N.Type.NONE,
                    "Overvoltage Protection"
                )
            )
            Eln.sharedItem.addElement(completId, element)
        }
    }

    private fun registerElectricalDrill(id: Int) {
        var subId: Int
        var completId: Int
        var name: String?

        var descriptor: ElectricalDrillDescriptor
        run {
            subId = 0
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Cheap Electrical Drill")
            descriptor = ElectricalDrillDescriptor(name, 8.0, 4000.0)
            Eln.sharedItem.addElement(completId, descriptor)
        }
        run {
            subId = 1
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Average Electrical Drill")
            descriptor = ElectricalDrillDescriptor(name, 5.0, 5000.0)
            Eln.sharedItem.addElement(completId, descriptor)
        }
        run {
            subId = 2
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Fast Electrical Drill")
            descriptor = ElectricalDrillDescriptor(name, 3.0, 6000.0)
            Eln.sharedItem.addElement(completId, descriptor)
        }
        run {
            subId = 3
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Turbo Electrical Drill")

            descriptor = ElectricalDrillDescriptor(name, 1.0, 10000.0)
            Eln.sharedItem.addElement(completId, descriptor)
        }
        run {
            subId = 4
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Irresponsible Electrical Drill")
            descriptor = ElectricalDrillDescriptor(name, 0.1, 20000.0)
            Eln.sharedItem.addElement(completId, descriptor)
        }
    }

    private fun registerOreScanner(id: Int) {
        var subId: Int
        var completId: Int
        var name: String?

        var descriptor: OreScanner
        run {
            subId = 0
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Ore Scanner")
            descriptor = OreScanner(name)
            Eln.sharedItem.addElement(completId, descriptor)
        }
    }

    private fun registerMiningPipe(id: Int) {
        var subId: Int
        var completId: Int
        var name: String?

        var descriptor: MiningPipeDescriptor
        run {
            subId = 0
            completId = subId + (id shl 6)
            name = I18N.TR_NAME(I18N.Type.NONE, "Mining Pipe")
            descriptor = MiningPipeDescriptor(name)
            Eln.sharedItem.addElement(completId, descriptor)
            Eln.miningPipeDescriptor = descriptor
        }
    }

    private fun registerElectricalTool(id: Int) {
        var subId: Int
        var name: String
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Small Flashlight")
            val desc = ElectricalFlashlightItem(name, 10, 6, 20.0, 12, 8, 50.0, 6000.0, 100.0)
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Improved Flashlight")
            val desc = ElectricalFlashlightItem(name, 15, 8, 20.0, 15, 12, 50.0, 24000.0, 400.0)
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }

        run {
            subId = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "Portable Electrical Mining Drill")
            // About the power of Efficiency 4
            val desc = ElectricalPickaxe(name, 22f, 1f, 120000.0, 200.0, 10000.0)
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }

        run {
            subId = 12
            name = I18N.TR_NAME(I18N.Type.NONE, "Portable Electrical Axe")
            val desc = ElectricalAxe(name, 22f, 1f, 40000.0, 200.0, 10000.0)
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 16
            name = I18N.TR_NAME(I18N.Type.NONE, "The Nope Wand")
            val desc = NopeWandDescriptor(name)
            desc.creativeTab = Eln.creativeTabToolsArmor
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }
    }

    private fun registerPortableItem(id: Int) {
        var subId: Int
        var name: String
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Portable Battery")
            val desc = BatteryItem(name, 40000.0, 125.0, 250.0, 2)
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }

        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Portable Battery Pack")
            val desc = BatteryItem(name, 160000.0, 500.0, 1000.0, 2)
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }

        run {
            subId = 16
            name = I18N.TR_NAME(I18N.Type.NONE, "Portable Condensator")
            val desc = BatteryItem(name, 4000.0, 2000.0, 2000.0, 1)
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 17
            name = I18N.TR_NAME(I18N.Type.NONE, "Portable Condensator Pack")
            val desc = BatteryItem(name, 16000.0, 8000.0, 8000.0, 1)
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }

        run {
            subId = 32
            name = I18N.TR_NAME(I18N.Type.NONE, "X-Ray Scanner")
            val desc = PortableOreScannerItem(
                name, Eln.obj.getObj("XRayScanner")!!, 100000.0, 400.0,
                300.0,
                Eln.config.getDoubleOrElse("runtime.tools.xrayScanner.rangeBlocks.clamped", 5.0).toFloat(),
                (Math.PI / 2).toFloat(),
                32,
                20
            )
            Eln.sharedItemStackOne.addElement(subId + (id shl 6), desc)
        }
    }

    private fun registerMiscItem(id: Int) {
        var subId: Int
        var name: String?
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Cheap Chip")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Eln.addToOre(Eln.config.getStringOrElse("runtime.dictionary.cheapChip", "circuitElnBasic"), desc)
        }
        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Advanced Chip")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Eln.addToOre(Eln.config.getStringOrElse("runtime.dictionary.advancedChip", "circuitElnAdvanced"), desc)
        }
        run {
            subId = 2
            name = I18N.TR_NAME(I18N.Type.NONE, "Machine Block")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Eln.addToOre("casingMachine", desc)
        }
        run {
            subId = 3
            name = I18N.TR_NAME(I18N.Type.NONE, "Electrical Probe Chip")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 4
            name = I18N.TR_NAME(I18N.Type.NONE, "Thermal Probe Chip")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }

        run {
            subId = 6
            name = I18N.TR_NAME(I18N.Type.NONE, "Copper Plate")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Eln.plateCopper = desc
            Eln.addToOre("plateCopper", desc)
        }
        run {
            subId = 7
            name = I18N.TR_NAME(I18N.Type.NONE, "Iron Plate")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Eln.addToOre("plateIron", desc)
        }
        run {
            subId = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "Gold Plate")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Eln.addToOre("plateGold", desc)
        }
        run {
            subId = 9
            name = I18N.TR_NAME(I18N.Type.NONE, "Lead Plate")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Eln.addToOre("plateLead", desc)
        }
        run {
            subId = 10
            name = I18N.TR_NAME(I18N.Type.NONE, "Silicon Plate")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Eln.addToOre("plateSilicon", desc)
        }

        run {
            subId = 11
            name = I18N.TR_NAME(I18N.Type.NONE, "Alloy Plate")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Eln.addToOre("plateAlloy", desc)
        }
        run {
            subId = 12
            name = I18N.TR_NAME(I18N.Type.NONE, "Coal Plate")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Eln.addToOre("plateCoal", desc)
        }

        run {
            subId = 16
            name = I18N.TR_NAME(I18N.Type.NONE, "Silicon Dust")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Eln.addToOre("dustSilicon", desc)
        }
        run {
            subId = 17
            name = I18N.TR_NAME(I18N.Type.NONE, "Silicon Ingot")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Eln.addToOre("ingotSilicon", desc)
        }

        run {
            subId = 22
            name = I18N.TR_NAME(I18N.Type.NONE, "Machine Booster")
            val desc = MachineBoosterDescriptor(name)
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 23
            val desc =
                GenericItemUsingDamageDescriptorWithComment(
                    I18N.TR_NAME(I18N.Type.NONE, "Advanced Machine Block"),
                    arrayOf()
                ).inOresAndMaterialsTab() // TODO: Description.
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Eln.addToOre("casingMachineAdvanced", desc)
        }
        run {
            subId = 28
            name = I18N.TR_NAME(I18N.Type.NONE, "Basic Magnet")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 29
            name = I18N.TR_NAME(I18N.Type.NONE, "Advanced Magnet")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 32
            name = I18N.TR_NAME(I18N.Type.NONE, "Data Logger Print")
            val desc = DataLogsPrintDescriptor(name)
            Eln.dataLogsPrintDescriptor = desc
            desc.setDefaultIcon("empty-texture")
            Eln.sharedItem.addWithoutRegistry(subId + (id shl 6), desc)
        }

        run {
            subId = 33
            name = I18N.TR_NAME(I18N.Type.NONE, "Signal Antenna")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }

        run {
            subId = 40
            name = I18N.TR_NAME(I18N.Type.NONE, "Player Filter")
            val desc = EntitySensorFilterDescriptor(name, net.minecraft.world.entity.player.Player::class.java, 0f, 1f, 0f)
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 41
            name = I18N.TR_NAME(I18N.Type.NONE, "Monster Filter")
            val desc = EntitySensorFilterDescriptor(name, net.minecraft.world.entity.monster.Enemy::class.java, 1f, 0f, 0f)
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 42
            name = I18N.TR_NAME(I18N.Type.NONE, "Animal Filter")
            val desc = EntitySensorFilterDescriptor(name, net.minecraft.world.entity.animal.Animal::class.java, .3f, .3f, 1f)
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }

        run {
            subId = 48
            name = I18N.TR_NAME(I18N.Type.NONE, "Wrench")
            val desc = GenericItemUsingDamageDescriptorWithComment(name,
                tr("Electrical age wrench,\nCan be used to turn\nsmall wall blocks")
                    .split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()).inToolsAndArmorTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
            Eln.wrenchDescriptor = desc
        }

        run {
            subId = 52
            name = I18N.TR_NAME(I18N.Type.NONE, "Dielectric")
            val desc = DielectricItem(name, Eln.LVU).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }

        val casingDesc = CaseItemDescriptor(I18N.TR_NAME(I18N.Type.NONE, "Casing")).inOresAndMaterialsTab()
        Eln.sharedItem.addElement(53 + (id shl 6), casingDesc)
        Eln.sharedItem.addElement(
            54 + (id shl 6), ClutchPlateItem(
                I18N.TR_NAME(I18N.Type.NONE, "Iron Clutch Plate"), 5120f, 640f, 640f, 160f,
                0.0001f, false
            ).inOresAndMaterialsTab()
        )
        Eln.sharedItem.addElement(55 + (id shl 6), ClutchPinItem(I18N.TR_NAME(I18N.Type.NONE, "Clutch Pin")).inOresAndMaterialsTab())
        Eln.sharedItem.addElement(
            56 + (id shl 6), ClutchPlateItem(
                I18N.TR_NAME(I18N.Type.NONE, "Gold Clutch Plate"), 10240f, 2048f, 1024f, 512f,
                0.001f, false
            ).inOresAndMaterialsTab()
        )
        Eln.sharedItem.addElement(
            57 + (id shl 6), ClutchPlateItem(
                I18N.TR_NAME(I18N.Type.NONE, "Copper Clutch Plate"), 8192f, 4096f, 1024f, 512f,
                0.0003f, false
            ).inOresAndMaterialsTab()
        )
        Eln.sharedItem.addElement(
            58 + (id shl 6), ClutchPlateItem(
                I18N.TR_NAME(I18N.Type.NONE, "Lead Clutch Plate"), 15360f, 1024f, 1536f, 768f,
                0.0015f, false
            ).inOresAndMaterialsTab()
        )
        Eln.sharedItem.addElement(
            59 + (id shl 6), ClutchPlateItem(
                I18N.TR_NAME(I18N.Type.NONE, "Coal Clutch Plate"), 1024f, 128f, 128f, 32f, 0.1f,
                true
            ).inOresAndMaterialsTab()
        )
        run {
            subId = 60
            name = I18N.TR_NAME(I18N.Type.NONE, "Motor Cowling")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 61
            name = I18N.TR_NAME(I18N.Type.NONE, "Heavy Winding")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 62
            name = I18N.TR_NAME(I18N.Type.NONE, "Reinforced Shaft")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
        run {
            subId = 63
            name = I18N.TR_NAME(I18N.Type.NONE, "Turbine Blade Set")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }
    }

    private fun registerElectricMinecartItems(id: Int) {
        val subId = 0
        val name = TR_NAME(I18N.Type.NONE, "Electric Minecart")
        Eln.sharedItem.addElement(subId + (id shl 6), ElectricMinecartItem(name))
    }

    private fun registerBasicItems(id: Int) {
        var subId: Int
        var name: String
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Silicon Wafer")
            Eln.siliconWafer = SiliconWafer(name)
            Eln.sharedItem.addElement(subId + (id shl 6), Eln.siliconWafer)
            Eln.addToOre(Eln.dictSiliconWafer, Eln.siliconWafer)
        }
        run {
            subId = 1
            name = I18N.TR_NAME(I18N.Type.NONE, "Transistor")
            Eln.transistor = Transistor(name)
            Eln.sharedItem.addElement(subId + (id shl 6), Eln.transistor)
            Eln.addToOre(Eln.dictTransistor, Eln.transistor)
        }
        run {
            subId = 2
            name = I18N.TR_NAME(I18N.Type.NONE, "NTC Thermistor")
            Eln.thermistor = Thermistor(name)
            Eln.sharedItem.addElement(subId + (id shl 6), Eln.thermistor)
            Eln.addToOre(Eln.dictThermistor, Eln.thermistor)
        }
        run {
            subId = 3
            name = I18N.TR_NAME(I18N.Type.NONE, "Nibble Memory Chip")
            Eln.nibbleMemory = NibbleMemory(name)
            Eln.sharedItem.addElement(subId + (id shl 6), Eln.nibbleMemory)
            Eln.addToOre(Eln.dictNibbleMemory, Eln.nibbleMemory)
        }
        run {
            subId = 4
            name = I18N.TR_NAME(I18N.Type.NONE, "Arithmetic Logic Unit")
            Eln.alu = ArithmeticLogicUnit(name)
            Eln.sharedItem.addElement(subId + (id shl 6), Eln.alu)
            Eln.addToOre(Eln.dictALU, Eln.alu)
        }
    }
}
