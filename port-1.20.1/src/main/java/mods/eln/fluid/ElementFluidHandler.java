package mods.eln.fluid;

import mods.eln.misc.INBTTReady;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;

/**
 * Use one of these if you want your block to support Forge fluids!
 * <p>
 * See the steam turbine for an example. Exposed through the Forge fluid handler capability of the machine.
 */
public class ElementFluidHandler implements IFluidHandler, INBTTReady {
    private Fluid[] whitelist;
    private float fluid_heat_mb = 0;
    public FluidTank tank;

    /**
     * Stores fluids.
     *
     * @param tankSize Tank size, in mB.
     */
    public ElementFluidHandler(int tankSize) {
        tank = new FluidTank(tankSize);
    }

    public void setFilter(Fluid[] whitelist) {
        assert whitelist != null;
        this.whitelist = whitelist;
    }

    public float getHeatEnergyPerMilliBucket() {
        if (fluid_heat_mb == 0 && !tank.getFluid().isEmpty()) setHeatEnergyPerMilliBucket(tank.getFluid().getFluid());
        return fluid_heat_mb;
    }

    private void setHeatEnergyPerMilliBucket(Fluid fluid) {
        fluid_heat_mb = (float) FuelRegistry.INSTANCE.heatEnergyPerMilliBucket(fluid);
    }

    private boolean accepts(Fluid fluid) {
        if (whitelist == null) return true;
        for (Fluid allowed : whitelist) {
            if (allowed.isSame(fluid)) return true;
        }
        return false;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int index) {
        return tank.getFluid();
    }

    @Override
    public int getTankCapacity(int index) {
        return tank.getCapacity();
    }

    @Override
    public boolean isFluidValid(int index, @NotNull FluidStack stack) {
        if (tank.getFluidAmount() > 0) return tank.getFluid().isFluidEqual(stack);
        return accepts(stack.getFluid());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (tank.getFluidAmount() > 0) {
            // No change in type of fluid.
            return tank.fill(resource, action);
        } else if (accepts(resource.getFluid())) {
            // May have a different fluid.
            if (action.execute()) setHeatEnergyPerMilliBucket(resource.getFluid());
            return tank.fill(resource, action);
        }
        return 0;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isFluidEqual(tank.getFluid()))
            return tank.drain(resource.getAmount(), action);
        else
            return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        return tank.drain(maxDrain, action);
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        tank.readFromNBT(nbt.getCompound(str + "tank"));
        fluid_heat_mb = nbt.getFloat(str + "fhm");
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        CompoundTag t = new CompoundTag();
        tank.writeToNBT(t);
        nbt.put(str + "tank", t);
        nbt.putFloat(str + "fhm", fluid_heat_mb);
    }
}
