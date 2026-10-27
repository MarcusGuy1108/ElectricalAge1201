package mods.eln.generic

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.CreativeModeTab

/**
 * Keeps track of descriptor-driven item families so each creative tab can list its descriptors.
 */
object CreativeTabPopulator {
    val blockItems = ArrayList<GenericItemBlockUsingDamage<*>>()
    val genericItems = ArrayList<GenericItemUsingDamage<*>>()

    fun register(item: GenericItemBlockUsingDamage<*>) {
        if (!blockItems.contains(item)) blockItems.add(item)
    }

    fun register(item: GenericItemUsingDamage<*>) {
        if (!genericItems.contains(item)) genericItems.add(item)
    }

    // Items outside the descriptor families (ore blocks); tab and stack are looked up when the tab is built.
    private val extraStacks = ArrayList<Pair<() -> GenericCreativeTab, () -> ItemStack>>()

    fun addStack(tab: () -> GenericCreativeTab, stack: () -> ItemStack) {
        extraStacks.add(tab to stack)
    }

    // PORT: moveRegulatorChipsAfterDcDcConverters returns with the regulator chips and DC/DC converters.
    fun addEntries(tab: GenericCreativeTab, output: CreativeModeTab.Output) {
        for (item in blockItems) {
            item.getSubItems(tab, output)
        }
        for (item in genericItems) {
            item.getSubItems(tab, output)
        }
        for ((stackTab, stack) in extraStacks) {
            if (stackTab() === tab) output.accept(stack())
        }
    }
}
