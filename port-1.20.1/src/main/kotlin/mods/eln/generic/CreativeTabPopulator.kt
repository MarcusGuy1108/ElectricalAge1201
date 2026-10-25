package mods.eln.generic

import net.minecraft.world.item.CreativeModeTab

/**
 * Keeps track of descriptor-driven item families so each creative tab can list its descriptors.
 */
object CreativeTabPopulator {
    private val blockItems = ArrayList<GenericItemBlockUsingDamage<*>>()

    fun register(item: GenericItemBlockUsingDamage<*>) {
        if (!blockItems.contains(item)) blockItems.add(item)
    }

    // PORT: GenericItemUsingDamage families and moveRegulatorChipsAfterDcDcConverters return with those items.
    fun addEntries(tab: GenericCreativeTab, output: CreativeModeTab.Output) {
        for (item in blockItems) {
            item.getSubItems(tab, output)
        }
    }
}
