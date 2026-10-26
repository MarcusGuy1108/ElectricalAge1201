package mods.eln.transparentnode.autominer;

import mods.eln.compat.LegacyCompat;

import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.ElectricalDrillDescriptor;
import mods.eln.item.MiningPipeDescriptor;
import mods.eln.lightblock.LightBlockEntity;
import mods.eln.misc.Coordinate;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;

public class AutoMinerSlowProcess implements IProcess, INBTTReady {

    private final AutoMinerElement miner;

    int pipeLength = 0;

    private double energyCounter = 0;
    private double energyTarget = 0;

    private boolean oneJobDone = true;
    boolean silkTouch = false;

    enum jobType {none, done, full, chestFull, ore, pipeAdd, pipeRemove}

    jobType job = jobType.none;
    private jobType oldJob = jobType.none;
    private final Coordinate jobCoord = new Coordinate();
    private int blinkCounter = 0;

    private int drillCount = 1;

    private ArrayList<ItemStack> itemsToDrop = new ArrayList<ItemStack>(4);

    public AutoMinerSlowProcess(AutoMinerElement autoMiner) {
        this.miner = autoMiner;
    }

    void toggleSilkTouch() {
        silkTouch = !silkTouch;
    }

    private boolean isReadyToDrill() {
        ElectricalDrillDescriptor drill = (ElectricalDrillDescriptor) GenericItemUsingDamageDescriptor.getDescriptor(
            LegacyCompat.getStackInSlot(miner.getInventory(), AutoMinerContainer.electricalDrillSlotId), ElectricalDrillDescriptor.class);
        if (drill == null) return false;
        return isStorageReady();
    }

    private boolean isStorageReady() {
        Container i = getDropInventory();
        if (i == null) return false;
        for (int idx = 0; idx < i.getContainerSize(); idx++) {
            if (LegacyCompat.getStackInSlot(i, idx) == null)
                return true;
        }
        return false;
    }

    @Override
    public void process(double time) {
        ElectricalDrillDescriptor drill = (ElectricalDrillDescriptor) GenericItemUsingDamageDescriptor.getDescriptor(
            LegacyCompat.getStackInSlot(miner.getInventory(), AutoMinerContainer.electricalDrillSlotId), ElectricalDrillDescriptor.class);

        if (++blinkCounter >= 9) {
            blinkCounter = 0;
            if ((miner.inPowerLoad.getVoltage() / miner.descriptor.nominalVoltage - 0.5) * 3 > Math.random()) {
                miner.setPowerOk(true);
                LightBlockEntity.addLight(miner.lightCoordinate, 12, 11);
            } else {
                miner.setPowerOk(false);
            }
        }

        energyCounter += miner.powerResistor.getPower() * time;

        if (job != jobType.none && job != jobType.full && job != jobType.chestFull && job != jobType.done) {
            if (energyCounter >= energyTarget || (job == jobType.ore && !isReadyToDrill()) || !miner.powerOk) {
                setupJob();
            }

            if (energyCounter >= energyTarget) {
                oneJobDone = true;
                switch (job) {
                    case ore:
                        drillCount++;

                        Level world = jobCoord.world();
                        BlockPos pos = jobCoord.getBlockPos();
                        BlockState state = world.getBlockState(pos);
                        if (silkTouch) {
                            ItemStack silk = new ItemStack(state.getBlock());
                            if (!silk.isEmpty()) itemsToDrop.add(silk);
                        } else if (world instanceof ServerLevel) {
                            itemsToDrop.addAll(Block.getDrops(state, (ServerLevel) world, pos, world.getBlockEntity(pos)));
                        }

                        // Use cobblestone instead of air, everywhere except the mining shaft.
                        // This is so mobs won't spawn excessively.
                        int xDist = jobCoord.x - miner.node.coordinate.x, zDist = jobCoord.z - miner.node.coordinate.z;
                        if (xDist * xDist + zDist * zDist > 25) {
                            world.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
                        } else {
                            world.removeBlock(pos, false);
                        }

                        energyCounter -= energyTarget;
                        setupJob();
                        break;
                    case pipeAdd:
                        // miner.pushLog("Pipe " + (pipeLength + 1) + " added");
                        Eln.ghostManager.createGhost(jobCoord, miner.node.coordinate, jobCoord.y);
                        LegacyCompat.decrStackSize(miner.getInventory(), AutoMinerContainer.MiningPipeSlotId, 1);

                        pipeLength++;
                        miner.needPublish();

                        energyCounter -= energyTarget;
                        setupJob();
                        break;
                    case pipeRemove:
                        // miner.pushLog("Pipe " + pipeLength + " removed");
                        Eln.ghostManager.removeGhostAndBlock(jobCoord);
                        if (LegacyCompat.getStackInSlot(miner.getInventory(), AutoMinerContainer.MiningPipeSlotId) == null) {
                            LegacyCompat.setInventorySlotContents(miner.getInventory(), AutoMinerContainer.MiningPipeSlotId, Eln.miningPipeDescriptor.newItemStack(1));
                            miner.getInventory().setChanged();
                        } else {
                            LegacyCompat.decrStackSize(miner.getInventory(), AutoMinerContainer.MiningPipeSlotId, -1);
                        }

                        pipeLength--;
                        miner.needPublish();

                        energyCounter -= energyTarget;
                        setupJob();
                        break;
                    default:
                        break;
                }
            }
        } else {
            setupJob();
        }

        switch (job) {
            default:
                miner.powerResistor.highImpedance();
                break;
            case ore:
                if (drill == null) {
                    miner.powerResistor.highImpedance();
                } else {
                    double p = drill.nominalPower;
                    if (silkTouch) p *= 3;
                    miner.powerResistor.setResistance(Math.pow(miner.descriptor.nominalVoltage, 2.0) / p);
                }
                break;
            case pipeAdd:
                miner.powerResistor.setResistance(miner.descriptor.pipeOperationRp);
                break;
            case pipeRemove:
                miner.powerResistor.setResistance(miner.descriptor.pipeOperationRp);
                break;
        }

        if (oldJob != job) {
            miner.needPublish();
        }

        if (oneJobDone || oldJob != job) {
            switch (job) {
                case chestFull:
                    miner.pushLog("* Storage full!");
                    break;
                case done:
                    miner.pushLog("- SLEEP");
                    break;
                case full:
                    miner.pushLog("* Pipe stack full!");
                    break;
                case none:
                    miner.pushLog("* Waiting opcode.");
                    break;
                case ore:
                    miner.pushLog("- DRILL #" + drillCount);
                    break;
                case pipeAdd:
                    miner.pushLog("- ADD PIPE #" + (pipeLength + 1));
                    break;
                case pipeRemove:
                    miner.pushLog("- REMOVE PIPE #" + (pipeLength));
                    break;
                default:
                    break;
            }
        }
        oneJobDone = false;
        oldJob = job;
    }
    private Container getDropInventory() {
        Container inventoryEntity = null;
        Coordinate outputLocation = new Coordinate(1, -1, 0, miner.world());
        outputLocation.applyTransformation(miner.front, miner.coordinate());
        if (outputLocation.getTileEntity() instanceof Container) {
            inventoryEntity = (Container) outputLocation.getTileEntity();
            BlockState inventoryState = miner.world().getBlockState(outputLocation.getBlockPos());
            if (inventoryState.getBlock() instanceof ChestBlock) {
                Container possibleDoubleInventoryEntity = ChestBlock.getContainer((ChestBlock) inventoryState.getBlock(), inventoryState, miner.world(), outputLocation.getBlockPos(), true);
                if (possibleDoubleInventoryEntity != null) {
                    inventoryEntity = possibleDoubleInventoryEntity;
                }
            }
        }
        return inventoryEntity;
    }

    private boolean drop(ItemStack stack) {
        return Utils.tryPutStackInInventory(stack, getDropInventory());
    }

    private boolean isMinable(BlockState state) {
        return !state.isAir() && !(state.getBlock() instanceof LiquidBlock)
            && !state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK);
    }

    private void setupJob() {
        ElectricalDrillDescriptor drill = (ElectricalDrillDescriptor) GenericItemUsingDamageDescriptor.getDescriptor(
            LegacyCompat.getStackInSlot(miner.getInventory(), AutoMinerContainer.electricalDrillSlotId), ElectricalDrillDescriptor.class);
        // OreScanner scanner = (OreScanner) ElectricalDrillDescriptor.getDescriptor(LegacyCompat.getStackInSlot(miner.inventory, AutoMinerContainer.OreScannerSlotId));
        MiningPipeDescriptor pipe = (MiningPipeDescriptor) GenericItemUsingDamageDescriptor.getDescriptor(
            LegacyCompat.getStackInSlot(miner.getInventory(), AutoMinerContainer.MiningPipeSlotId), MiningPipeDescriptor.class);

        int scannerRadius = Eln.config.getIntOrElse("machines.autominer.maxRangeBlocks", 10);
        double scannerEnergy = 0;

        jobCoord.dimension = miner.node.coordinate.dimension;
        jobCoord.x = miner.node.coordinate.x;
        jobCoord.y = miner.node.coordinate.y - pipeLength;
        jobCoord.z = miner.node.coordinate.z;

        // Attempt to drop items. This might not be successful.
        while (itemsToDrop.size() > 0) {
            int index = itemsToDrop.size() - 1;
            if (drop(itemsToDrop.get(index))) {
                itemsToDrop.remove(index);
            }
        }

        boolean jobFind = false;
        if (!miner.node.coordinate.world().isLoaded(miner.node.coordinate.getBlockPos())) {
            setJob(jobType.none);
        } else if (!miner.powerOk) {
            setJob(jobType.none);
        } else if (drill == null) {
            if (jobCoord.y != miner.node.coordinate.y) {
                ItemStack pipeStack = LegacyCompat.getStackInSlot(miner.getInventory(), AutoMinerContainer.MiningPipeSlotId);
                if (pipeStack == null || (pipeStack.getCount() != pipeStack.getMaxStackSize() && pipeStack.getCount() != miner.getInventory().getMaxStackSize())) {
                    jobFind = true;
                    setJob(jobType.pipeRemove);
                } else {
                    jobFind = true;
                    setJob(jobType.full);
                }
            }
        } else if (!isStorageReady() || itemsToDrop.size() != 0) {
            setJob(jobType.chestFull);
            jobFind = true;
        } else if (pipe != null) {
            if (jobCoord.y < miner.node.coordinate.y - 2) {
                int depth = (miner.node.coordinate.y - jobCoord.y);
                double miningRay = depth / 10.0 + 0.1;
                miningRay = Math.min(miningRay, 2);
                if (depth < scannerRadius) scannerRadius = depth + 1;
                miningRay = Math.min(miningRay, scannerRadius - 2);
                for (jobCoord.z = miner.node.coordinate.z - scannerRadius; jobCoord.z <= miner.node.coordinate.z + scannerRadius; jobCoord.z++) {
                    for (jobCoord.x = miner.node.coordinate.x - scannerRadius; jobCoord.x <= miner.node.coordinate.x + scannerRadius; jobCoord.x++) {
                        double dx = jobCoord.x - miner.node.coordinate.x;
                        double dy = 0;
                        double dz = jobCoord.z - miner.node.coordinate.z;
                        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        BlockState state = jobCoord.world().getBlockState(jobCoord.getBlockPos());
                        if (state.is(Tags.Blocks.ORES) || (distance > 0.1 && distance < miningRay && isMinable(state))) {
                            jobFind = true;
                            setJob(jobType.ore);
                            break;
                        }
                    }
                    if (jobFind) break;
                }
            }

            if (!jobFind) {
                // 1.7.10 stopped at y = 3; worlds now start at the minimum build height.
                if (jobCoord.y < jobCoord.world().getMinBuildHeight() + 3) {
                    jobFind = true;
                    setJob(jobType.done);
                } else {
                    jobCoord.x = miner.node.coordinate.x;
                    jobCoord.y--;
                    jobCoord.z = miner.node.coordinate.z;

                    BlockState state = jobCoord.world().getBlockState(jobCoord.getBlockPos());
                    if (!state.isAir() && !(state.getBlock() instanceof LiquidBlock)) {
                        if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) {
                            jobFind = true;
                            setJob(jobType.ore);
                        } else {
                            jobFind = true;
                            setJob(jobType.done);
                        }
                    } else {
                        jobFind = true;
                        setJob(jobType.pipeAdd);
                    }
                }
            }
        }
        if (!jobFind) setJob(jobType.none);

        switch (job) {
            case ore:
                energyTarget = drill.OperationEnergy + scannerEnergy;
                // Copied from Mekanism. Note that the power demand is tripled, so in effect this doubles time.
                if (silkTouch) energyTarget *= 6;
                break;
            case pipeAdd:
                energyTarget = miner.descriptor.pipeOperationEnergy;
                break;
            case pipeRemove:
                energyTarget = miner.descriptor.pipeOperationEnergy;
                break;
            default:
                energyTarget = 0;
                break;
        }
    }

    private void setJob(jobType job) {
        if (job != this.job) {
            miner.needPublish();
            energyCounter = 0;
        }
        this.job = job;
    }

    // The ore check (1.7.10 BlockOre, Eln ores and the ore color map) is the forge:ores tag.

    public void onBreakElement() {
        destroyPipe();
    }

    private void destroyPipe() {
        dropPipe();
        Eln.ghostManager.removeGhostAndBlockWithObserverAndNotUuid(miner.node.coordinate, miner.descriptor.getGhostGroupUuid());
        pipeLength = 0;
        miner.needPublish();
    }

    private void dropPipe() {
        Coordinate coord = new Coordinate(miner.node.coordinate);
        for (coord.y = miner.node.coordinate.y - 1; coord.y >= miner.node.coordinate.y - pipeLength; coord.y--) {
            Block.popResource(coord.world(), coord.getBlockPos(), Eln.miningPipeDescriptor.newItemStack(1));
        }
    }

    void ghostDestroyed() {
        destroyPipe();
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        pipeLength = nbt.getInt(str + "AMSP" + "pipeLength");
        drillCount = nbt.getInt(str + "AMSP" + "drillCount");
        if (drillCount == 0) drillCount++;
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        nbt.putInt(str + "AMSP" + "pipeLength", pipeLength);
        nbt.putInt(str + "AMSP" + "drillCount", drillCount);
    }
}
