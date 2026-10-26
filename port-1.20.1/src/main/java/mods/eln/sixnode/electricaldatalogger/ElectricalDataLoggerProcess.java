package mods.eln.sixnode.electricaldatalogger;

import mods.eln.compat.LegacyCompat;

import mods.eln.Eln;
import mods.eln.sim.IProcess;
import net.minecraft.world.item.ItemStack;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ElectricalDataLoggerProcess implements IProcess {

    ElectricalDataLoggerElement e;

    public ElectricalDataLoggerProcess(ElectricalDataLoggerElement e) {
        this.e = e;
    }

    @Override
    public void process(double time) {
        //Profiler p = new Profiler();
        //p.add("A");
        if (!e.pause) {
            e.timeToNextSample -= time;
            byte value = (byte) (e.inputGate.getNormalized() * 255.5 - 128);
            e.sampleStack += value;
            e.sampleStackNbr++;
        }
        //p.add("B");
        if (e.printToDo) {
            ItemStack paperStack = LegacyCompat.getStackInSlot(e.inventory, ElectricalDataLoggerContainer.paperSlotId);
            ItemStack printStack = LegacyCompat.getStackInSlot(e.inventory, ElectricalDataLoggerContainer.printSlotId);
            if (paperStack != null && printStack == null) {
                LegacyCompat.decrStackSize(e.inventory, ElectricalDataLoggerContainer.paperSlotId, 1);
                ItemStack print = Eln.instance.dataLogsPrintDescriptor.newItemStack(1);
                Eln.instance.dataLogsPrintDescriptor.initializeStack(print, e.logs);
                LegacyCompat.setInventorySlotContents(e.inventory, ElectricalDataLoggerContainer.printSlotId, print);
                e.inventory.setChanged();
            }
            e.printToDo = false;
        }
        //p.add("C");
        if (e.timeToNextSample <= 0.0) {
            e.timeToNextSample += e.logs.samplingPeriod;
            byte value = (byte) (e.sampleStack / e.sampleStackNbr);
            e.sampleStackReset();
            e.logs.write(value);

            ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
            DataOutputStream packet = new DataOutputStream(bos);

            e.preparePacketForClient(packet);

            try {
                packet.writeByte(e.toClientLogsAdd);
                packet.write(value);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            //p.add("D");
            e.sendPacketToAllClient(bos);
        }
        //p.stop();
        //Utils.println(p);
    }
}
