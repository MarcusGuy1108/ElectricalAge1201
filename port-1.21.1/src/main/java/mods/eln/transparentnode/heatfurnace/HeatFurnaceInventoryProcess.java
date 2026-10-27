package mods.eln.transparentnode.heatfurnace;

import mods.eln.compat.LegacyCompat;

import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

public class HeatFurnaceInventoryProcess implements IProcess, INBTTReady {

    HeatFurnaceElement furnace;
    //double combustibleEnergyMax = 40000 * 2;
    double combustibleBuffer = 0;

    public HeatFurnaceInventoryProcess(HeatFurnaceElement furnace) {
        this.furnace = furnace;
    }

    @Override
    public void process(double time) {
        ItemStack combustibleStack = LegacyCompat.getStackInSlot(furnace.inventory, HeatFurnaceContainer.combustibleId);
        ItemStack combustionChamberStack = LegacyCompat.getStackInSlot(furnace.inventory, HeatFurnaceContainer.combustrionChamberId);
        ItemStack isolatorChamberStack = LegacyCompat.getStackInSlot(furnace.inventory, HeatFurnaceContainer.isolatorId);

        double isolationFactor = 1;
        if (isolatorChamberStack != null) {
            ThermalIsolatorElement iso = (ThermalIsolatorElement) GenericItemUsingDamageDescriptor.getDescriptor(
                isolatorChamberStack, ThermalIsolatorElement.class);
            if (iso == null) {
                isolationFactor = 1;
            } else if (furnace.thermalLoad.temperatureCelsius > iso.getTmax()) {
                LegacyCompat.decrStackSize(furnace.inventory, HeatFurnaceContainer.isolatorId, 1);
            } else {
                isolationFactor = iso.getConductionFactor();
            }
        }
        furnace.thermalLoad.setRp(furnace.descriptor.thermal.Rp / isolationFactor);

        int combustionChamberNbr = 0;
        if (combustionChamberStack != null) {
            combustionChamberNbr = combustionChamberStack.getCount();
        }
        furnace.furnaceProcess.nominalPower = furnace.descriptor.nominalPower + furnace.descriptor.combustionChamberPower * combustionChamberNbr;

        if (furnace.getTakeFuel()) {
            if (Eln.config.getBooleanOrElse("machines.heatFurnace.consumeFuel", false)) {
                combustibleBuffer = furnace.furnaceProcess.nominalCombustibleEnergy;
            } else if (combustibleStack != null) {
                double itemEnergy = Utils.getItemEnergie(combustibleStack);
                if (itemEnergy != 0) {
                    if (furnace.furnaceProcess.combustibleEnergy + combustibleBuffer < furnace.furnaceProcess.nominalCombustibleEnergy) {
                        //	furnace.furnaceProcess.combustibleEnergy += itemEnergy;
                        combustibleBuffer += itemEnergy;
                        // 1.20.1 items name their leftover (the empty bucket of a lava bucket) themselves; read it
                        // before the decrement, as an emptied stack reports itself as air.
                        ItemStack remainder = combustibleStack.getCraftingRemainingItem();
                        LegacyCompat.decrStackSize(furnace.inventory, HeatFurnaceContainer.combustibleId, 1);
                        if (!remainder.isEmpty()) {
                            LegacyCompat.setInventorySlotContents(furnace.inventory, HeatFurnaceContainer.combustibleId, remainder);
                            furnace.inventory.setChanged();
                        }
                    }
                }
            }
        }

        if (furnace.furnaceProcess.combustibleEnergy + combustibleBuffer < furnace.furnaceProcess.nominalCombustibleEnergy) {
            furnace.furnaceProcess.combustibleEnergy += combustibleBuffer;
            combustibleBuffer = 0;
        } else {
            double delta = furnace.furnaceProcess.nominalCombustibleEnergy - furnace.furnaceProcess.combustibleEnergy;
            furnace.furnaceProcess.combustibleEnergy += delta;
            combustibleBuffer -= delta;
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        combustibleBuffer = nbt.getDouble(str + "HFIP" + "combustribleBuffer");
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        nbt.putDouble(str + "HFIP" + "combustribleBuffer", combustibleBuffer);
    }
}
