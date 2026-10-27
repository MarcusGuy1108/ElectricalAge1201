package mods.eln.transparentnode.electricalantennatx;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.transparent.TransparentNode.FrontType;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import mods.eln.client.GL11;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalAntennaTxDescriptor extends TransparentNodeDescriptor {
    // PORT: IItemRenderer drawing is replaced by the generated item model.

    Obj3D obj;
    Obj3DPart main;

    int rangeMax;
    double electricalPowerRatioEffStart, electricalPowerRatioEffEnd;
    double electricalPowerRatioLostOffset, electricalPowerRatioLostPerBlock;
    double electricalNominalVoltage, electricalNominalPower;
    double electricalMaximalVoltage, electricalMaximalPower;
    double electricalNominalInputR;
    ElectricalCableDescriptor cable;

    public ElectricalAntennaTxDescriptor(String name, Obj3D obj,
                                         int rangeMax,
                                         double electricalPowerRatioEffStart, double electricalPowerRatioEffEnd,
                                         double electricalNominalVoltage, double electricalNominalPower,
                                         double electricalMaximalVoltage, double electricalMaximalPower,
                                         ElectricalCableDescriptor cable) {
        super(name, ElectricalAntennaTxElement.class, ElectricalAntennaTxRender.class);
        this.rangeMax = rangeMax;
        this.electricalNominalVoltage = electricalNominalVoltage;
        this.electricalNominalPower = electricalNominalPower;
        this.electricalMaximalVoltage = electricalMaximalVoltage;
        this.electricalMaximalPower = electricalMaximalPower;
        this.electricalPowerRatioEffStart = electricalPowerRatioEffStart;
        this.electricalPowerRatioEffEnd = electricalPowerRatioEffEnd;
        this.cable = cable;

        electricalPowerRatioLostOffset = 1.0 - electricalPowerRatioEffStart;
        electricalPowerRatioLostPerBlock = (electricalPowerRatioEffStart - electricalPowerRatioEffEnd) / rangeMax;

        electricalNominalInputR = electricalNominalVoltage * electricalNominalVoltage / electricalNominalPower;

        this.obj = obj;
        if (obj != null) main = obj.getPart("main");

        setDefaultIcon("electricalantennatx");
        voltageLevelColor = VoltageLevelColor.fromVoltage(electricalNominalVoltage);
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
    }

    @Override
    public FrontType getFrontType() {
        return FrontType.BlockSideInv;
    }

    @Override
    public boolean mustHaveWallFrontInverse() {
        return true;
    }

    @Override
    public boolean mustHaveFloor() {
        return false;
    }

    public void draw() {
        GL11.glDisable(GL11.GL_CULL_FACE);
        if (main != null) main.draw();
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List<String> list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Wireless energy transmitter."));
        list.add(tr("Nominal usage:"));
        list.add("  " + tr("Voltage: %1$V", Utils.plotValue(electricalNominalVoltage)));
        list.add("  " + tr("Power: %1$W", Utils.plotValue(electricalNominalPower)));
        list.add("  " + tr("Range: %1$ blocks", rangeMax));
        list.add("  " + tr("Efficiency: %1$% up to %2$%", Utils.plotValue(electricalPowerRatioEffEnd * 100),
            Utils.plotValue(electricalPowerRatioEffStart * 100)));
    }
}
