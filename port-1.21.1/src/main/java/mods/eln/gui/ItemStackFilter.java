package mods.eln.gui;

import mods.eln.misc.OreDictionaryTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Slot filter on an item, or (for 1.7.10 ore dictionary names) on the matching item tag.
 */
public class ItemStackFilter implements IItemStackFilter {
    private final Item item;
    private final TagKey<Item> tag;

    // shortcut: 1.20.1 split damage subtypes into separate items, so the damage filter is ignored; callers that relied
    // on it (e.g. a dye color) need the specific item instead.
    public ItemStackFilter(Item item, int damageMask, int damageValue) {
        this(item);
    }

    public ItemStackFilter(Block block, int damageMask, int damageValue) {
        this(block.asItem());
    }

    public ItemStackFilter(Item item) {
        this.item = item;
        this.tag = null;
    }

    public ItemStackFilter(Block block) {
        this(block.asItem());
    }

    private ItemStackFilter(TagKey<Item> tag) {
        this.item = null;
        this.tag = tag;
    }

    public static ItemStackFilter[] OreDict(String name) {
        return new ItemStackFilter[]{new ItemStackFilter(ItemTags.create(OreDictionaryTags.tagFor(name)))};
    }

    @Override
    public boolean tryItemStack(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return false;
        return tag != null ? itemStack.is(tag) : itemStack.is(item);
    }
}
