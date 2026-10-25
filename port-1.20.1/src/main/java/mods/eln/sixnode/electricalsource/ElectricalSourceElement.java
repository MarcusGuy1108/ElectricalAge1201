package mods.eln.sixnode.electricalsource;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// PORT: the red/white/ground conductors only connect through utility cables, which are not ported yet; until then
// every side is the single black conductor. The config copy tool (IConfigurable) returns with that tool.
public class ElectricalSourceElement extends SixNodeElement {

    NbtElectricalLoad electricalLoad = new NbtElectricalLoad("electricalLoad");
    NbtElectricalLoad electricalLoadRed = new NbtElectricalLoad("electricalLoadRed");
    NbtElectricalLoad electricalLoadWhite = new NbtElectricalLoad("electricalLoadWhite");
    NbtElectricalLoad electricalLoadGround = new NbtElectricalLoad("electricalLoadGround");
    VoltageSource voltageSource = new VoltageSource("voltSrc", electricalLoad, null);
    VoltageSource voltageSourceRed = new VoltageSource("voltSrcRed", electricalLoadRed, null);
    VoltageSource voltageSourceWhite = new VoltageSource("voltSrcWhite", electricalLoadWhite, null);
    VoltageSource voltageSourceGround = new VoltageSource("voltSrcGround", electricalLoadGround, null);

    public static final int setVoltageId = 1;

    public ElectricalSourceElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        electricalLoadList.add(electricalLoad);
        electricalLoadList.add(electricalLoadRed);
        electricalLoadList.add(electricalLoadWhite);
        electricalLoadList.add(electricalLoadGround);
        electricalComponentList.add(voltageSource);
        electricalComponentList.add(voltageSourceRed);
        electricalComponentList.add(voltageSourceWhite);
        electricalComponentList.add(voltageSourceGround);
    }

    @Override
    public void readFromNBT(@NotNull CompoundTag nbt) {
        super.readFromNBT(nbt);
        setConfiguredVoltage(nbt.getDouble("voltage"));
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putDouble("voltage", voltageSource.getVoltage());
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        return getLoadForColor(lrdu, extractRequestedColor(mask, lrdu));
    }

    @Nullable
    @Override
    public ThermalLoad getThermalLoad(@NotNull LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (((ElectricalSourceDescriptor) sixNodeElementDescriptor).isSignalSource()) {
            return NodeBase.maskElectricalOutputGate;
        } else {
            int color = getPrimaryHotColor(lrdu);
            return NodeBase.maskElectricalPower + (color << NodeBase.maskColorShift) + (1 << NodeBase.maskColorCareShift);
        }
    }

    @Override
    public String multiMeterString() {
        return Utils.plotUIP(Math.max(electricalLoad.getVoltage(), electricalLoadRed.getVoltage()), getDisplayedCurrent());
    }

    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Voltage"), Utils.plotVolt("", Math.max(electricalLoad.getVoltage(), electricalLoadRed.getVoltage())));
        info.put(I18N.tr("Current"), Utils.plotAmpere("", getDisplayedCurrent()));
        if (Eln.config.getBooleanOrElse("ui.waila.easyMode", false)) {
            info.put(I18N.tr("Power"), Utils.plotPower("", Math.max(Math.abs(electricalLoad.getVoltage() * voltageSource.getCurrent()), Math.abs(electricalLoadRed.getVoltage() * voltageSourceRed.getCurrent()))));
        }
        return info;
    }

    @NotNull
    @Override
    public String thermoMeterString() {
        return "";
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat((float) voltageSource.getVoltage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        Eln.applySmallRs(electricalLoad);
        Eln.applySmallRs(electricalLoadRed);
        Eln.applySmallRs(electricalLoadWhite);
        Eln.applySmallRs(electricalLoadGround);
        setConfiguredVoltage(voltageSource.getVoltage());
    }

    @Override
    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        return onBlockActivatedRotate(entityPlayer);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            switch (stream.readByte()) {
                case setVoltageId:
                    setConfiguredVoltage(stream.readFloat());
                    needPublish();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    private void setConfiguredVoltage(double voltage) {
        voltageSource.setVoltage(voltage);
        voltageSourceRed.setVoltage(-voltage);
        voltageSourceWhite.setVoltage(0.0);
        voltageSourceGround.setVoltage(0.0);
    }

    private double getDisplayedCurrent() {
        return Math.max(Math.abs(voltageSource.getCurrent()), Math.abs(voltageSourceRed.getCurrent()));
    }

    private ElectricalLoad getLoadForColor(LRDU lrdu, int color) {
        return electricalLoad;
    }

    private int extractRequestedColor(int mask, LRDU lrdu) {
        if ((mask & NodeBase.maskColorCareData) == 0) {
            return getPrimaryHotColor(lrdu);
        }
        return (mask >> NodeBase.maskColorShift) & 0xF;
    }

    private int getPrimaryHotColor(LRDU lrdu) {
        return 0;
    }
}
