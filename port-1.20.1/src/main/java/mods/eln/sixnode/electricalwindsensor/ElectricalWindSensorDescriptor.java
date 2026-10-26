package mods.eln.sixnode.electricalwindsensor;

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

public class ElectricalWindSensorDescriptor extends SixNodeDescriptor {
    // PORT: IItemRenderer drawing is replaced by the generated item model.

    private Obj3DPart baseWall, baseGround, anemometer;

    public float[] pinDistance;

    Obj3D obj;

    public double windMax;

    public ElectricalWindSensorDescriptor(
        String name,
        Obj3D obj,
        double windMax) {
        super(name, ElectricalWindSensorElement.class, ElectricalWindSensorRender.class);
        this.windMax = windMax;
        this.obj = obj;

        if (obj != null) {
            baseWall = obj.getPart("base_wall");
            baseGround = obj.getPart("base_ground");
            anemometer = obj.getPart("anemometer");

            pinDistance = Utils.getSixNodePinDistance(baseWall);
        }

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    void draw(float alpha) {
        if (baseWall != null) baseWall.draw();
        if (anemometer != null) {
            UtilsClient.disableCulling();
            anemometer.draw(alpha, 0, 1, 0);
            UtilsClient.enableCulling();
        }
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List<String> list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Provides an electrical signal\ndependant on wind speed.").split("\n"));
        list.add(tr("Maximum wind speed is %1$m/s", Utils.plotValue(windMax)));
    }

    @Override
    public boolean canBePlacedOnSide(Player player, Direction side) {
        if (side.isY()) {
            Utils.addChatMessage(player, tr("You can't place this block on the floor or the ceiling"));
            return false;
        }
        return super.canBePlacedOnSide(player, side);
    }

    @Nullable
    @Override
    public LRDU getFrontFromPlace(@NotNull Direction side, @NotNull Player player) {
        return super.getFrontFromPlace(side, player).right();
    }
}
