package mods.eln.registration

import mods.eln.Eln
import mods.eln.i18n.I18N
import mods.eln.item.lampitem.LampDescriptor
import mods.eln.item.lampitem.LampLists
import mods.eln.item.lampitem.SpecificLampData
import mods.eln.misc.NominalVoltage

// Ported subset of the 1.7.10 ItemRegistration; the other families return as their descriptors are ported.
object ItemRegistration {
    fun registerItem() {
        Eln.sharedItem.setCreativeTabForGroup(4, Eln.creativeTabLighting)
        registerLampItem(4)
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
}
