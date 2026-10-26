package mods.eln.item;

import mods.eln.misc.Utils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Moves items between the player's inventory and a node slot to reach a desired count (config copy tool, GUI
 * buttons). 1.20.1 menus resend changed slots on their own, so the 1.7.10 manual slot packets are gone.
 */
public abstract class ItemMovingHelper {
    public abstract boolean acceptsStack(ItemStack stack);
    public abstract ItemStack newStackOfSize(int items);

    // PORT: utility cable spools (trimmed to length on insertion) return with UtilityCableDescriptor.
    public void move(Inventory src, Container dst, int dstSlot, int desired) {
        boolean dstChanged = false;
        if (src.player instanceof ServerPlayer && Utils.isCreative((ServerPlayer) src.player)) {
            dst.setItem(dstSlot, desired == 0 ? ItemStack.EMPTY : newStackOfSize(desired));
            dst.setChanged();
            return;
        }
        int now = dst.getItem(dstSlot).getCount();
        Utils.println(String.format("IMH.m: now %d, desired %d", now, desired));
        if (now < desired) {
            int diff = desired - now;
            for (int idx = 0; idx < src.getContainerSize(); idx++) {
                ItemStack invStack = src.getItem(idx);
                if (invStack.isEmpty()) continue;
                if (!acceptsStack(invStack)) continue;
                int move = Math.min(invStack.getCount(), diff);
                diff -= move;
                invStack.shrink(move);
                src.setItem(idx, invStack.isEmpty() ? ItemStack.EMPTY : invStack);
                if (diff <= 0) break;
            }
            int moved = (desired - now) - diff;
            Utils.println(String.format("IMH.m: moved %d into node", moved));
            if (moved > 0) {
                dst.setItem(dstSlot, newStackOfSize(now + moved));
                dstChanged = true;
            }
        } else {
            int diff = now - desired;
            Utils.println(String.format("IMH.m: moving %d items", diff));
            if (diff > 0) {
                if (src.add(newStackOfSize(diff))) {
                    dst.setItem(dstSlot, desired == 0 ? ItemStack.EMPTY : newStackOfSize(desired));
                    dstChanged = true;
                    Utils.println("IMH.m: move succeeded");
                } else {
                    Utils.println("IMH.m: move failed!");
                }
            }
        }
        src.setChanged();
        if (dstChanged) {
            dst.setChanged();
        }
    }
}
