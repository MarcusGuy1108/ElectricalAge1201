package mods.eln.sixnode.wirelesssignal.tx;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import mods.eln.client.GL11;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class WirelessSignalTxDescriptor extends SixNodeDescriptor {
    // PORT: IItemRenderer drawing is replaced by the generated item model.

    private Obj3D obj;
    Obj3DPart main;

    int range;

    public WirelessSignalTxDescriptor(String name,
                                      Obj3D obj,
                                      int range) {
        super(name, WirelessSignalTxElement.class, WirelessSignalTxRender.class);
        this.range = range;
        this.obj = obj;
        if (obj != null) main = obj.getPart("main");

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    public void draw() {
        if (main != null) main.draw();
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
    }

    @Nullable
    @Override
    public LRDU getFrontFromPlace(@NotNull Direction side, @NotNull Player player) {
        return super.getFrontFromPlace(side, player).inverse();
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List<String> list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Sends signal voltage on selected wireless signal channel"));
    }

    @Override
    public RealisticEnum addRealismContext(List<String> list) {
        super.addRealismContext(list);
        list.add(tr("It should require power to transmit realistically"));
        return RealisticEnum.IDEAL;
    }
}
