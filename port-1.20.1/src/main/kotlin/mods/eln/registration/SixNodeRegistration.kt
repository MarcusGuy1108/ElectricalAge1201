package mods.eln.registration

import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.generic.GenericCreativeTab
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.i18n.I18N
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor

/**
 * Six-node descriptor registration, ported from 1.7.10 one family at a time. Descriptor ids (subId + (group shl 6))
 * are unchanged because node saves store them.
 */
object SixNodeRegistration {

    private fun <T : GenericItemBlockUsingDamageDescriptor> T.inTab(tab: GenericCreativeTab) = apply {
        setCreativeTab(tab)
    }

    private fun <T : GenericItemBlockUsingDamageDescriptor> T.power() = inTab(Eln.creativeTabPowerElectronics)
    private fun <T : GenericItemBlockUsingDamageDescriptor> T.signal() = inTab(Eln.creativeTabSignalProcessing)
    private fun <T : GenericItemBlockUsingDamageDescriptor> T.lighting() = inTab(Eln.creativeTabLighting)
    private fun <T : GenericItemBlockUsingDamageDescriptor> T.cables() = inTab(Eln.creativeTabCables)
    private fun <T : GenericItemBlockUsingDamageDescriptor> T.powerDistribution() = inTab(Eln.creativeTabPowerDistribution)
    private fun <T : GenericItemBlockUsingDamageDescriptor> T.tools() = inTab(Eln.creativeTabToolsArmor)
    private fun <T : GenericItemBlockUsingDamageDescriptor> T.materials() = inTab(Eln.creativeTabOresMaterials)
    private fun <T : GenericItemBlockUsingDamageDescriptor> T.machines() = inTab(Eln.creativeTabMachines)
    private fun <T : GenericItemBlockUsingDamageDescriptor> T.creative() = inTab(Eln.creativeTabCreative)
    private fun <T : GenericItemBlockUsingDamageDescriptor> T.other() = inTab(Eln.creativeTabOther)

    fun registerSix() {
        Eln.sixNodeItem.setCreativeTabForGroup(2, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(3, Eln.creativeTabCreative)
        Eln.sixNodeItem.setCreativeTabForGroup(5, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(32, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(33, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(34, Eln.creativeTabCables)
        Eln.sixNodeItem.setCreativeTabForGroup(35, Eln.creativeTabCables)
        Eln.sixNodeItem.setCreativeTabForGroup(36, Eln.creativeTabCables)
        Eln.sixNodeItem.setCreativeTabForGroup(37, Eln.creativeTabCables)
        Eln.sixNodeItem.setCreativeTabForGroup(48, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(126, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(127, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(64, Eln.creativeTabLighting)
        Eln.sixNodeItem.setCreativeTabForGroup(67, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(65, Eln.creativeTabLighting)
        Eln.sixNodeItem.setCreativeTabForGroup(92, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(93, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(94, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(95, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(96, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(97, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(98, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(100, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(101, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(102, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(103, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(104, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(108, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(109, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(117, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(118, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(124, Eln.creativeTabSignalProcessing)
        Eln.sixNodeItem.setCreativeTabForGroup(66, Eln.creativeTabPowerElectronics)
        Eln.sixNodeItem.setCreativeTabForGroup(116, Eln.creativeTabMachines)
        Eln.sixNodeItem.setCreativeTabForGroup(125, Eln.creativeTabToolsArmor)
        // PORT: registerGround(2)
        // PORT: registerElectricalSource(3)
        registerElectricalCable(32)
        // PORT: registerCurrentCables(33)
        // PORT: registerUtilityCables(34, 35, 36, 37)
        // PORT: registerThermalCable(48)
        // PORT: registerCurrentRelays(126)
        // PORT: registerConduit(127)
        // PORT: registerLampSocket(64)
        // PORT: registerPowerSocket(67)
        // PORT: registerLampSupply(65)

        // PORT: registerWirelessSignal(92)
        // PORT: registerElectricalDataLogger(93)
        // PORT: registerElectricalRelay(94)
        // PORT: registerElectricalGateSource(95)
        // PORT: registerPassiveComponent(96)
        // PORT: registerSwitch(97)
        // PORT: registerElectricalManager(98)
        // PORT: registerElectricalSensor(100)
        // PORT: registerThermalSensor(101)
        // PORT: registerElectricalVuMeter(102)
        // PORT: registerElectricalAlarm(103)
        // PORT: registerElectricalEnvironmentalSensor(104)
        // PORT: registerElectricalRedstone(108)
        // PORT: registerElectricalGate(109)
        // PORT: registerSixNodeMisc(117)
        // PORT: registerLogicalGates(118)
        // PORT: registerAnalogChips(124)
        // PORT: registerBatteryCharger(66)

        // PORT: registerTreeResinCollector(116)
        // PORT: registerPortableNaN()
    }

    // PORT: 1.7.10 hides the classic cables in favour of utility cables; they stay visible until utility cables are ported.
    private fun showUntilUtilityCables(@Suppress("UNUSED_PARAMETER") desc: GenericItemBlockUsingDamageDescriptor) {
    }

    private fun registerElectricalCable(id: Int) {
        var subId: Int
        var name: String?

        var desc: ElectricalCableDescriptor
        run {
            subId = 0
            name = I18N.TR_NAME(I18N.Type.NONE, "Signal Cable")
            Eln.instance.stdCableRenderSignal = CableRenderDescriptor("eln", "sprites/cable.png", 0.95f, 0.95f)
            desc = ElectricalCableDescriptor(name, Eln.instance.stdCableRenderSignal, "For signal transmission.", true).signal()
            showUntilUtilityCables(desc)
            Eln.instance.signalCableDescriptor = desc
            desc.setPhysicalConstantLikeNormalCable(
                Eln.SVU, Eln.SVP, 0.02 / 50 * Eln.SVU, Eln.SVU * 1.3,
                Eln.SVP * 1.2, 0.5, Eln.cableWarmLimit, -100.0, Eln.cableHeatingTime, 1.0
            )
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 4
            name = I18N.TR_NAME(I18N.Type.NONE, "Low Voltage Cable")
            Eln.instance.stdCableRender50V = CableRenderDescriptor("eln", "sprites/cable.png", 1.95f, 0.95f)
            desc = ElectricalCableDescriptor(name, Eln.instance.stdCableRender50V, "For low voltage with high current.", false).power()
            Eln.instance.lowVoltageCableDescriptor = desc
            desc.setPhysicalConstantLikeNormalCable(
                Eln.LVU, Eln.instance.LVP(), 0.2 / 20 / 100, Eln.LVU * 1.3, Eln.instance.LVP() * 1.2, 20.0, Eln.cableWarmLimit,
                -100.0, Eln.cableHeatingTime, Eln.cableThermalConductionTao
            )
            showUntilUtilityCables(desc)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
            desc = ElectricalCableDescriptor(name, Eln.instance.stdCableRender50V, "For low voltage with high current.", false)
            desc.setPhysicalConstantLikeNormalCable(
                Eln.LVU, Eln.instance.LVP() / 4, 0.2 / 20 / 100, Eln.LVU * 1.3, Eln.instance.LVP() * 1.2, 20.0,
                Eln.cableWarmLimit, -100.0, Eln.cableHeatingTime, Eln.cableThermalConductionTao
            )
        }

        run {
            subId = 8
            name = I18N.TR_NAME(I18N.Type.NONE, "Medium Voltage Cable")
            Eln.instance.stdCableRender200V = CableRenderDescriptor("eln", "sprites/cable.png", 2.95f, 0.95f)
            desc = ElectricalCableDescriptor(name, Eln.instance.stdCableRender200V, "miaou", false).power()
            Eln.instance.meduimVoltageCableDescriptor = desc
            desc.setPhysicalConstantLikeNormalCable(
                Eln.MVU, Eln.instance.MVP(), 0.10 / 20 / 100, Eln.MVU * 1.3, Eln.instance.MVP() * 1.2, 30.0, Eln.cableWarmLimit,
                -100.0, Eln.cableHeatingTime, Eln.cableThermalConductionTao
            )
            showUntilUtilityCables(desc)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }
        run {
            subId = 12
            name = I18N.TR_NAME(I18N.Type.NONE, "High Voltage Cable")
            Eln.instance.stdCableRender800V = CableRenderDescriptor("eln", "sprites/cable.png", 3.95f, 1.95f)
            desc = ElectricalCableDescriptor(name, Eln.instance.stdCableRender800V, "miaou2", false).power()
            Eln.instance.highVoltageCableDescriptor = desc
            desc.setPhysicalConstantLikeNormalCable(
                Eln.HVU, Eln.instance.HVP(), 0.025 * 5 / 4 / 20 / 100, Eln.HVU * 1.3, Eln.instance.HVP() * 1.2, 40.0,
                Eln.cableWarmLimit, -100.0, Eln.cableHeatingTime, Eln.cableThermalConductionTao
            )
            showUntilUtilityCables(desc)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }


        run {
            subId = 16
            name = I18N.TR_NAME(I18N.Type.NONE, "Very High Voltage Cable")
            Eln.instance.stdCableRender3200V = CableRenderDescriptor("eln", "sprites/cableVHV.png", 3.95f, 1.95f)
            desc = ElectricalCableDescriptor(name, Eln.instance.stdCableRender3200V, "miaou2", false).power()
            Eln.instance.veryHighVoltageCableDescriptor = desc
            desc.setPhysicalConstantLikeNormalCable(
                Eln.VVU, Eln.instance.VVP(), 0.025 * 5 / 4 / 20 / 8 / 100, Eln.VVU * 1.3, Eln.instance.VVP() * 1.2, 40.0,
                Eln.cableWarmLimit, -100.0, Eln.cableHeatingTime, Eln.cableThermalConductionTao
            )
            showUntilUtilityCables(desc)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        run {
            subId = 24
            name = I18N.TR_NAME(I18N.Type.NONE, "Creative Cable")
            Eln.instance.stdCableRenderCreative = CableRenderDescriptor("eln", "sprites/cablecreative.png", 8.0f, 4.0f)
            desc = ElectricalCableDescriptor(
                name, Eln.instance.stdCableRenderCreative, "Experience the power of " +
                        "Microresistance", false
            ).creative()
            Eln.instance.creativeCableDescriptor = desc
            desc.setPhysicalConstantLikeNormalCable(
                Eln.VVU * 16, Eln.VVU * 16 * Eln.instance.VVP(), 1e-9,  //what!?
                Eln.VVU * 16 * 1.3, Eln.VVU * 16 * Eln.instance.VVP() * 1.2, 40.0, Eln.cableWarmLimit, -100.0, Eln.cableHeatingTime,
                Eln.cableThermalConductionTao
            )
            showUntilUtilityCables(desc)
            Eln.sixNodeItem.addDescriptor(subId + (id shl 6), desc)
        }

        // PORT: Signal Bus Cable (subId 20, ElectricalSignalBusCableElement) returns with the signal bus.
    }

}
