package mods.eln.sixnode.groundcable;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GroundCableElement extends SixNodeElement {
    static final int GROUND_CONDUCTOR_COLOR = 2;

    NbtElectricalLoad electricalLoad = new NbtElectricalLoad("electricalLoad");
    VoltageSource ground = new VoltageSource("ground", electricalLoad, null);
    //ElectricalSourceRefGroundProcess groundProcess = new ElectricalSourceRefGroundProcess(electricalLoad, 0);

    int color = 0;
    int colorCare = 0;

    SixNodeElementInventory inventory = new SixNodeElementInventory(1, 64, this);

    public GroundCableElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        electricalLoadList.add(electricalLoad);
        electricalComponentList.add(ground);
        ground.setVoltage(0);
    }

    @Override
    public void readFromNBT(@NotNull CompoundTag nbt) {
        super.readFromNBT(nbt);
        byte b = nbt.getByte("color");
        color = b & 0xF;
        colorCare = (b >> 4) & 1;
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putByte("color", (byte) (color + (colorCare << 4)));
    }

    @Override
    public Container getInventory() {
        return inventory;
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        return electricalLoad;
    }

    @Nullable
    @Override
    public ThermalLoad getThermalLoad(@NotNull LRDU lrdu, int mask) {
        return null;
    }

    // PORT: utility cables choose their ground conductor here again once they are ported.
    @Override
    public int getConnectionMask(LRDU lrdu) {
        return defaultConnectionMask();
    }

    int defaultConnectionMask() {
        return NodeBase.maskElectricalPower + (color << NodeBase.maskColorShift) + (colorCare << NodeBase.maskColorCareShift);
    }

    @Override
    public String multiMeterString() {
        return Utils.plotVolt("U:", electricalLoad.getVoltage()) + Utils.plotAmpere("I:", electricalLoad.getCurrent());
    }

    @NotNull
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Current"), Utils.plotAmpere("", electricalLoad.getCurrent()));
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
            stream.writeByte(color << 4);
            Utils.serialiseItemStack(stream, inventory.getItem(GroundCableContainer.cableSlotId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        Eln.applySmallRs(electricalLoad);
    }

    @Override
    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        if (Utils.isPlayerUsingWrench(entityPlayer)) {
            colorCare = colorCare ^ 1;
            Utils.addChatMessage(entityPlayer, "Wire color care " + colorCare);
            sixNode.reconnect();
        }
        // PORT: painting the wire color with a brush returns with BrushDescriptor.
        return false;
    }

    @Override
    public void inventoryChanged() {
        super.inventoryChanged();
        reconnect();
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Nullable
    @Override
    public AbstractContainerMenu newContainer(@NotNull Direction side, @NotNull Player player) {
        return new GroundCableContainer(player, inventory);
    }
}
