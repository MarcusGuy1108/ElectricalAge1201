package mods.eln.transparentnode.electricalmachine;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.gui.GuiLabel;
import mods.eln.misc.*;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalStackMachineProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sound.SoundCommand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalMachineDescriptor extends TransparentNodeDescriptor {
    // PORT: IItemRenderer drawing is replaced by the generated item model.
    public RecipesList recipe = new RecipesList();

    final double nominalU;
    final double nominalP;
    private final ThermalLoadInitializer thermal;
    final ElectricalCableDescriptor cable;
    final int outStackCount;

    private final double resistorR;

    final double boosterEfficiency = 1.0 / 1.1;
    final double boosterSpeedUp = 1.25 / boosterEfficiency;

    SoundCommand endSound;
    String runningSound;

    private Object defaultHandle = null;

    ElectricalMachineDescriptor(String name, double nominalU, double nominalP, double maximalU,
                                ThermalLoadInitializer thermal, ElectricalCableDescriptor cable,
                                RecipesList recipe) {
        super(name, ElectricalMachineElement.class, ElectricalMachineRender.class);
        outStackCount = 4;
        this.nominalP = nominalP;
        this.nominalU = nominalU;
        this.cable = cable;
        this.thermal = thermal;
        resistorR = nominalU * nominalU / nominalP;
        this.recipe = recipe;

        voltageLevelColor = VoltageLevelColor.fromCable(cable);
    }

    public ElectricalMachineDescriptor setRunningSound(String runningSound) {
        this.runningSound = runningSound;
        return this;
    }

    public ElectricalMachineDescriptor setEndSound(SoundCommand endSound) {
        this.endSound = endSound;
        return this;
    }

    public float volumeForRunningSound(float processState, float powerFactor) {
        if (powerFactor >= 0.3)
            return 0.3f * powerFactor;
        else
            return 0f;
    }

    // PORT: recipe.addMachine(newItemStack()) (the machines a recipe viewer lists) returns with a recipe viewer
    // integration; setParent runs while the item is still being registered, too early for an item stack.

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List<String> list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Nominal voltage: %1$V", Utils.plotValue(nominalU)));
        list.add(tr("Nominal power: %1$W", Utils.plotValue(nominalP)));
    }

    public void applyTo(ElectricalLoad load) {
        cable.applyTo(load);
    }

    public void applyTo(Resistor resistor) {
        resistor.setResistance(resistorR);
    }

    public void applyTo(ElectricalStackMachineProcess machine) {
        machine.setResistorValue(resistorR);
    }

    public void applyTo(ThermalLoad load) {
        thermal.applyTo(load);
    }

    Object newDrawHandle() {
        return null;
    }

    void draw(ElectricalMachineRender render, Object handleO, ItemStack inEntity, ItemStack outEntity,
              float powerFactor, float processState) {
    }

    void refresh(float deltaT, ElectricalMachineRender render, Object handleO, ItemStack inEntity,
                 ItemStack outEntity, float powerFactor, float processState) {
    }

    public boolean powerLrdu(Direction side, Direction front) {
        return true;
    }

    public boolean drawCable() {
        return false;
    }

    CableRenderDescriptor getPowerCableRender() {
        return null;
    }

    private Object getDefaultHandle() {
        if (defaultHandle == null)
            defaultHandle = newDrawHandle();
        return defaultHandle;
    }

    // PORT: the recipe pages (wiki IPlugIn top/bottom) return with a recipe viewer integration.

}
