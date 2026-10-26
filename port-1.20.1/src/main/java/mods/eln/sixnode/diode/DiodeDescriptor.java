package mods.eln.sixnode.diode;

import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.RealisticEnum;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalLoadInitializer;
import mods.eln.sim.mna.component.ResistorSwitch;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import mods.eln.client.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class DiodeDescriptor extends SixNodeDescriptor {
    // PORT: IItemRenderer drawing is replaced by the generated item model.

    private Obj3DPart base;
    private Obj3DPart diodeCables;
    private Obj3DPart diodeCore;

    double stdI, stdU;
    ElectricalCableDescriptor cable;
    String descriptor;
    IFunction IfU;

    ThermalLoadInitializer thermal;

    public DiodeDescriptor(String name,
                           IFunction IfU,
                           double Imax,
                           double stdU, double stdI,
                           ThermalLoadInitializer thermal,
                           ElectricalCableDescriptor cable, Obj3D obj) {
        super(name, DiodeElement.class, DiodeRender.class);

        this.IfU = IfU;

        //double Umax = 0;
        //while(IfU.getValue(Umax) < Imax) Umax += 0.01;
        //double Pmax = Umax * IfU.getValue(Umax);
        this.cable = cable;
        this.thermal = thermal;
        thermal.setMaximalPower(stdU * stdI * 1.2);
        this.stdI = stdI;
        this.stdU = stdU;

        base = obj.getPart("Base");
        diodeCables = obj.getPart("DiodeCables");
        diodeCore = obj.getPart("DiodeCore");

        if (cable.signalWire) {
            voltageLevelColor = VoltageLevelColor.SignalVoltage;
        } else {
            voltageLevelColor = VoltageLevelColor.Neutral;
        }
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
    }

    /*	public void applyTo(DiodeProcess diode) {
            diode.IfU = IfU;
        }
        */
    public void applyTo(ThermalLoad load) {
        thermal.applyTo(load);
    }

    public void applyTo(ElectricalLoad load) {
        cable.applyTo(load);
    }

    public void applyTo(ResistorSwitch resistorSwitch) {
        resistorSwitch.setResistance(stdU / stdI);
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List<String> list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Electrical current can only\nflow through the diode\nfrom anode to cathode").split("\\\n"));
    }

    @Override
    public RealisticEnum addRealismContext(List<String> list) {
        super.addRealismContext(list);
        list.add(tr("Works, with the caveat that it's delayed a sim tick"));
        return RealisticEnum.IDEAL;
    }

    void draw() {
        if (base != null) base.draw();
        if (diodeCables != null) diodeCables.draw();
        if (diodeCore != null) diodeCore.draw();
    }
}
