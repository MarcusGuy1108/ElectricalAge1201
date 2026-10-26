package mods.eln.sixnode.electricalbreaker;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sixnode.genericcable.GenericCableDescriptor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import mods.eln.client.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalBreakerDescriptor extends SixNodeDescriptor {
    // PORT: IItemRenderer drawing is replaced by the generated item model.

    private Obj3D obj;
    private Obj3DPart main;
    private Obj3DPart lever;
    private Obj3DPart led;

    float alphaOff, alphaOn, speed;
    public double currentLimit;
    public float[] pinDistance;
    private CableRenderDescriptor ratedCableRender;

    public ElectricalBreakerDescriptor(String name, Obj3D obj) {
        this(name, obj, Double.POSITIVE_INFINITY);
    }

    public ElectricalBreakerDescriptor(String name, Obj3D obj, double currentLimit) {
        super(name, ElectricalBreakerElement.class, ElectricalBreakerRender.class);
        this.obj = obj;
        this.currentLimit = currentLimit;
        if (obj != null) {
            main = obj.getPart("case");
            lever = obj.getPart("lever");

            if (lever != null) {
                speed = lever.getFloat("speed");
                alphaOff = lever.getFloat("alphaOff");
                alphaOn = lever.getFloat("alphaOn");
            }
            pinDistance = Utils.getSixNodePinDistance(main);
        }

        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    public CableRenderDescriptor getRatedCableRender() {
        if (ratedCableRender == null) {
            GenericItemBlockUsingDamageDescriptor cable = GenericItemBlockUsingDamageDescriptor.getByName(ratedCableNameForCurrentLimit());
            if (cable instanceof GenericCableDescriptor) {
                ratedCableRender = ((GenericCableDescriptor) cable).render;
            }
        }
        return ratedCableRender;
    }

    private String ratedCableNameForCurrentLimit() {
        if (!Double.isFinite(currentLimit)) return "Copper 4/0 AWG Cable 1000V";
        if (currentLimit <= 15.0) return "Copper 14 AWG Cable 600V";
        if (currentLimit <= 20.0) return "Copper 12 AWG Cable 600V";
        if (currentLimit <= 30.0) return "Copper 10 AWG Cable 600V";
        if (currentLimit <= 40.0) return "Copper 8 AWG Cable 600V";
        if (currentLimit <= 60.0) return "Copper 6 AWG Cable 600V";
        if (currentLimit <= 100.0) return "Copper 2 AWG Cable 600V";
        if (currentLimit <= 125.0) return "Copper 1/0 AWG Cable 1000V";
        if (currentLimit <= 200.0) return "Copper 2/0 AWG Cable 1000V";
        return "Copper 4/0 AWG Cable 1000V";
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
    }

    // PORT: the current limit overlay on the item icon (IItemRenderer) returns with item decorations.

    public void draw(float on, float distance) {
        if (main != null) main.draw();
        if (lever != null) {
            lever.draw(on * (alphaOn - alphaOff) + alphaOff, 0, 1, 0);
        }
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List<String> list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, (tr("Protects electrical components\nOpens contact if:\n  - Voltage exceeds a certain level\n  - Current exceeds the cable limit").split("\n")));
        if (Double.isFinite(currentLimit)) {
            list.add(tr("Breaker rating: %1$A", mods.eln.misc.Utils.plotValue(currentLimit)));
        }
    }

    @Nullable
    @Override
    public LRDU getFrontFromPlace(@NotNull Direction side, @NotNull Player player) {
        return super.getFrontFromPlace(side, player).inverse();
    }
}
