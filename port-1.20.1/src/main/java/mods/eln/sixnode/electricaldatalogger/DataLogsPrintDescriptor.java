package mods.eln.sixnode.electricaldatalogger;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.UtilsClient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import mods.eln.client.GL11;

public class DataLogsPrintDescriptor extends GenericItemUsingDamageDescriptor {
    // PORT: IItemRenderer drawing is replaced by the generated item model.

    public DataLogsPrintDescriptor(String name) {
        super(name);
    }

    public void initializeStack(ItemStack stack, DataLogs logs) {
        CompoundTag nbt = new CompoundTag();
        logs.writeToNBT(nbt, "");//.setByteArray("logs", logs.copyLog());
        stack.setTag(nbt);
    }

    public final static float margin = 0.05f;
    public final static ResourceLocation backgroundTexture = new ResourceLocation("eln", "sprites/paper.png");

}
