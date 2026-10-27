package mods.eln.sixnode.resistor;

import mods.eln.compat.LegacyCompat;

import mods.eln.Eln;
import mods.eln.misc.*;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import mods.eln.client.GL11;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * Created by svein on 05/08/15.
 */
public class ResistorDescriptor extends SixNodeDescriptor {
    // PORT: IItemRenderer drawing is replaced by the generated item model.

    public final boolean isRheostat;
    public double thermalCoolLimit = -100;
    public double thermalWarmLimit = Eln.cableWarmLimit;
    public double thermalMaximalPowerDissipated = 1000;
    public double thermalNominalHeatTime = 120;
    public double thermalConductivityTao = Eln.cableThermalConductionTao;
    public double tempCoef;
    Obj3D.Obj3DPart ResistorBaseExtension, ResistorCore, ResistorTrack, ResistorWiper, Base, Cables;
    IFunction series;
    private Obj3D obj;

    public ResistorDescriptor(String name,
                              Obj3D obj,
                              IFunction series,
                              double tempCoef,
                              boolean isRheostat) {
        super(name, ResistorElement.class, ResistorRender.class);
        this.obj = obj;
        this.series = series;
        this.tempCoef = tempCoef;
        this.isRheostat = isRheostat;
        if (obj != null) {
            ResistorBaseExtension = obj.getPart("ResistorBaseExtention");
            ResistorCore = obj.getPart("ResistorCore");
            ResistorTrack = obj.getPart("ResistorTrack");
            ResistorWiper = obj.getPart("ResistorWiper");
            Base = obj.getPart("Base");
            Cables = obj.getPart("CapacitorCables");
        }
        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    public double getRsValue(Container inventory) {
        ItemStack core = LegacyCompat.getStackInSlot(inventory, ResistorContainer.coreId);

        if (core == null) return series.getValue(0);
        return series.getValue(core.getCount());
    }

    @Override
    public void setParent(net.minecraft.world.item.Item item, int damage) {
        super.setParent(item, damage);
    }

    void draw(float wiperPos) {
        //UtilsClient.disableCulling();
        //UtilsClient.disableTexture();
        //GL11.glRotatef(90, 1, 0, 0);

        if (null != Base) Base.draw();
        if (null != ResistorBaseExtension) ResistorBaseExtension.draw();
        if (null != ResistorCore) ResistorCore.draw();
        if (null != Cables) Cables.draw();

        if (isRheostat) {
            final float wiperSpread = 0.238f;
            wiperPos = (wiperPos - 0.5f) * wiperSpread * 2;
            ResistorTrack.draw();
            GL11.glTranslatef(0, 0, wiperPos);
            ResistorWiper.draw();
            // GL11.glTranslatef(-wiperPos, 0, 0);
        }
    }

    @Nullable
    @Override
    public LRDU getFrontFromPlace(@NotNull Direction side, @NotNull Player player) {
        return super.getFrontFromPlace(side, player).left();
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List<String> list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("It's a resistor"));
    }

    @Override
    public RealisticEnum addRealismContext(List<String> list) {
        super.addRealismContext(list);
        return RealisticEnum.REALISTIC;
    }
}
