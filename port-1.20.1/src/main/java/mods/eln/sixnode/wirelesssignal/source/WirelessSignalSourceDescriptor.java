package mods.eln.sixnode.wirelesssignal.source;

import mods.eln.misc.RealisticEnum;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceRenderObj;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import mods.eln.client.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class WirelessSignalSourceDescriptor extends SixNodeDescriptor {
    // PORT: IItemRenderer drawing is replaced by the generated item model.

    int range;
    public boolean autoReset;
    ElectricalGateSourceRenderObj render;

    public WirelessSignalSourceDescriptor(String name,
                                          ElectricalGateSourceRenderObj render,
                                          int range, boolean autoReset) {
        super(name, WirelessSignalSourceElement.class, WirelessSignalSourceRender.class);
        this.range = range;
        this.autoReset = autoReset;
        this.render = render;

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List<String> list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        if (autoReset) {
            Collections.addAll(list, tr("Acts like a\npush button.").split("\n"));
        } else {
            Collections.addAll(list, tr("Acts like a\ntoggle switch.").split("\n"));
        }
    }

    void draw(float factor, float distance, BlockEntity e) {
        render.draw(factor, distance, e);
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
    }

    @Override
    public RealisticEnum addRealismContext(List<String> list) {
        super.addRealismContext(list);
        list.add(tr("It should require power to transmit realistically"));
        return RealisticEnum.IDEAL;
    }
}
