package mods.eln.sixnode.electricalentitysensor;

import mods.eln.item.EntitySensorFilterDescriptor;
import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import mods.eln.client.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalEntitySensorDescriptor extends SixNodeDescriptor {
    // PORT: IItemRenderer drawing is replaced by the generated item model.

    boolean useEntitySpeed = true;
    double speedFactor = 1 / 0.10;
    private Obj3DPart detector, haloMask;
    double maxRange;
    public float[] pinDistance;
    Obj3D obj;

    public ElectricalEntitySensorDescriptor(String name, Obj3D obj, double maxRange) {
        super(name, ElectricalEntitySensorElement.class, ElectricalEntitySensorRender.class);
        this.obj = obj;
        this.maxRange = maxRange;
        if (obj != null) {
            detector = obj.getPart("Detector");
            haloMask = obj.getPart("HaloMask");

            pinDistance = Utils.getSixNodePinDistance(detector);
        }

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    void draw(boolean state, EntitySensorFilterDescriptor filter) {
        if (detector != null) detector.draw();
        if (state) {
            if (filter == null) {
                GL11.glColor3f(1f, 1f, 0f);
            } else {
                filter.glColor();
            }
            UtilsClient.drawLight(haloMask);
        }
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List<String> list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Output voltage increases\nif entities are moving around.").split("\n"));
        list.add(tr("Range: %1$ blocks", (int) maxRange));
    }

    @Nullable
    @Override
    public LRDU getFrontFromPlace(@NotNull Direction side, @NotNull Player player) {
        return super.getFrontFromPlace(side, player).right();
    }
}
