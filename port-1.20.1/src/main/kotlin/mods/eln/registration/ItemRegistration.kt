package mods.eln.registration

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
        // PORT: registerProtection(5)
        registerCombustionChamber(6)
        registerFerromagneticCore(7)
        registerIngot(8)
        registerDust(9)
        registerElectricalMotor(10)
        // PORT: registerSolarTracker(11)
        registerMeter(14)
        // PORT: registerElectricalDrill(15)
        // PORT: registerOreScanner(16)
        // PORT: registerMiningPipe(17)
        registerTreeResinAndRubber(64)
        registerRawCable(65)
        registerArc(69)
        // PORT: registerBrush(119)
        registerMiscItem(120)
        // PORT: registerElectricalTool(121)
        // PORT: registerPortableItem(122)
        // PORT: registerFuelBurnerItem(124)
        // PORT: registerTurbineBladeItems(18)
        registerBasicItems(126)
        // PORT: registerElectricMinecartItems(127)
        // PORT: registerWireToolItems(123)
        // PORT: registerWireMachineItems(118)
        // PORT: registerArmor()
        // PORT: registerTool()
        // PORT: registerOre()
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
        // PORT: Wireless Analyser returns with WirelessSignalAnalyserItemDescriptor.
        run {
            subId = 16
            completId = subId + (id shl 6)
            element =
                ConfigCopyToolDescriptor(I18N.TR_NAME(I18N.Type.NONE, "Config Copy Tool"))
            Eln.sharedItem.addElement(completId, element)
            Eln.configCopyToolElement = element
        }
        // PORT: Falstad Import Tool returns with FalstadImportToolDescriptor.
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

        // PORT: Machine Booster returns with MachineBoosterDescriptor.
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
        // PORT: Data Logger Print returns with DataLogsPrintDescriptor.

        run {
            subId = 33
            name = I18N.TR_NAME(I18N.Type.NONE, "Signal Antenna")
            val desc = GenericItemUsingDamageDescriptorWithComment(
                name,
                arrayOf()
            ).inOresAndMaterialsTab()
            Eln.sharedItem.addElement(subId + (id shl 6), desc)
        }

        // PORT: Player Filter returns with EntitySensorFilterDescriptor.
        // PORT: Monster Filter returns with EntitySensorFilterDescriptor.
        // PORT: Animal Filter returns with EntitySensorFilterDescriptor.

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

        // PORT: Dielectric returns with DielectricItem.

        // PORT: Casing returns with CaseItemDescriptor.
        // PORT: Iron Clutch Plate returns with ClutchPlateItem.
        // PORT: Clutch Pin returns with ClutchPinItem.
        // PORT: Gold Clutch Plate returns with ClutchPlateItem.
        // PORT: Copper Clutch Plate returns with ClutchPlateItem.
        // PORT: Lead Clutch Plate returns with ClutchPlateItem.
        // PORT: Coal Clutch Plate returns with ClutchPlateItem.
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
