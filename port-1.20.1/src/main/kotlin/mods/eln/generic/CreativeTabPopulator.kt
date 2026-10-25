package mods.eln.generic

import net.minecraft.world.item.CreativeModeTab

/**
 * Keeps track of descriptor-driven item families so each creative tab can list its descriptors.
 */
object CreativeTabPopulator {
    private val blockItems = ArrayList<GenericItemBlockUsingDamage<*>>()
    private val genericItems = ArrayList<GenericItemUsingDamage<*>>()

    fun register(item: GenericItemBlockUsingDamage<*>) {
        if (!blockItems.contains(item)) blockItems.add(item)
    }

    fun register(item: GenericItemUsingDamage<*>) {
        if (!genericItems.contains(item)) genericItems.add(item)
    }

    // PORT: moveRegulatorChipsAfterDcDcConverters returns with the regulator chips and DC/DC converters.
    fun addEntries(tab: GenericCreativeTab, output: CreativeModeTab.Output) {
        for (item in blockItems) {
            item.getSubItems(tab, output)
        }
        for (item in genericItems) {
            item.getSubItems(tab, output)
        }
    }
}
