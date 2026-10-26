package mods.eln.generic

import mods.eln.Eln
import mods.eln.registry.ElnRegistries
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.Hashtable

/**
 * 1.7.10 had one item per descriptor family with the descriptor in the damage value. 1.20.1 has no damage-based
 * subtypes, so this keeps the same API (addDescriptor/getDescriptor by id) but registers one item per descriptor
 * built by [itemFactory]. The numeric ids stay because node saves and packets store them.
 */
class GenericItemBlockUsingDamage<Descriptor : GenericItemBlockUsingDamageDescriptor>(
    private val itemFactory: (Descriptor) -> Item
) {
    val subItemList = Hashtable<Int, Descriptor>()
    val orderList = ArrayList<Int>()
    val descriptors = ArrayList<Descriptor>()
    private val creativeTabByGroup = HashMap<Int, GenericCreativeTab>()

    var defaultElement: Descriptor? = null

    init {
        CreativeTabPopulator.register(this)
    }

    fun addDescriptor(damage: Int, descriptor: Descriptor) {
        check(!subItemList.containsKey(damage)) { "Descriptor id $damage is already used by ${subItemList[damage]?.name}" }
        subItemList[damage] = descriptor
        orderList.add(damage)
        descriptors.add(descriptor)
        applyDefaultTab(damage, descriptor)
        ElnRegistries.ITEMS.register(ElnRegistries.itemName(descriptor.name, damage)) {
            itemFactory(descriptor).also { descriptor.setParent(it, damage) }
        }
    }

    // 1.7.10 kept these legacy descriptors loadable but out of the creative list.
    fun addWithoutRegistry(damage: Int, descriptor: Descriptor) {
        addDescriptor(damage, descriptor)
        orderList.remove(damage)
    }

    fun getDescriptor(damage: Int): Descriptor? {
        return subItemList[damage]
    }

    @Suppress("UNCHECKED_CAST")
    fun getDescriptor(itemStack: ItemStack?): Descriptor? {
        if (itemStack == null || itemStack.isEmpty) return defaultElement
        val descriptor = (itemStack.item as? IDescriptorItem)?.descriptor ?: return defaultElement
        return if (subItemList[descriptor.parentItemDamage] === descriptor) descriptor as Descriptor else defaultElement
    }

    fun getSubItems(tab: GenericCreativeTab?, output: CreativeModeTab.Output) {
        for (id in orderList) {
            val descriptor = subItemList[id]
            if (descriptor == null || descriptor.isHidden) continue
            // 1.7.10 listed descriptors without a tab under creativeTabOther.
            val descriptorTab = descriptor.creativeTab ?: Eln.creativeTabOther
            if (tab == null || tab === descriptorTab) {
                output.accept(descriptor.newCreativeTabStack())
            }
        }
    }

    private fun applyDefaultTab(damage: Int, descriptor: Descriptor) {
        if (descriptor.creativeTab != null) return
        creativeTabByGroup[damage shr 6]?.let { descriptor.setCreativeTab(it) }
    }

    fun setCreativeTabForGroup(group: Int, tab: GenericCreativeTab) {
        creativeTabByGroup[group] = tab
    }
}
