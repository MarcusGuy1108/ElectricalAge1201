package mods.eln.generic

import mods.eln.Eln
import mods.eln.registry.ElnRegistries
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.Hashtable

/**
 * Registrar for a family of [GenericItemUsingDamageDescriptor]s: one registered item per descriptor (built by
 * [itemFactory]), keeping the 1.7.10 numeric ids that saves store. See [GenericItemBlockUsingDamage].
 */
class GenericItemUsingDamage<Descriptor : GenericItemUsingDamageDescriptor>(
    private val itemFactory: (Descriptor) -> Item = { GenericItem(it) }
) {
    val subItemList = Hashtable<Int, Descriptor>()
    private val orderList = ArrayList<Int>()
    private val creativeTabByGroup = HashMap<Int, GenericCreativeTab>()
    var defaultElement: Descriptor? = null

    init {
        CreativeTabPopulator.register(this)
    }

    fun addElement(damage: Int, descriptor: Descriptor) {
        check(!subItemList.containsKey(damage)) { "Descriptor id $damage is already used by ${subItemList[damage]?.name}" }
        subItemList[damage] = descriptor
        orderList.add(damage)
        if (descriptor.creativeTab == null) creativeTabByGroup[damage shr 6]?.let { descriptor.creativeTab = it }
        ElnRegistries.ITEMS.register(ElnRegistries.itemName(descriptor.name, damage)) {
            itemFactory(descriptor).also { descriptor.setParent(it, damage) }
        }
    }

    // 1.7.10 kept these legacy descriptors loadable but out of the creative list.
    fun addWithoutRegistry(damage: Int, descriptor: Descriptor) {
        addElement(damage, descriptor)
        orderList.remove(damage)
    }

    fun getDescriptor(damage: Int): Descriptor? = subItemList[damage]

    @Suppress("UNCHECKED_CAST")
    fun getDescriptor(itemStack: ItemStack?): Descriptor? {
        val descriptor = GenericItemUsingDamageDescriptor.getDescriptor(itemStack) ?: return defaultElement
        return if (subItemList[descriptor.parentItemDamage] === descriptor) descriptor as Descriptor else defaultElement
    }

    fun getSubItems(tab: GenericCreativeTab?, output: CreativeModeTab.Output) {
        for (id in orderList) {
            val descriptor = subItemList[id] ?: continue
            if (descriptor.isHidden()) continue
            val descriptorTab = descriptor.creativeTab ?: Eln.creativeTabOther
            if (tab == null || tab === descriptorTab) output.accept(descriptor.newItemStack(1))
        }
    }

    fun setCreativeTabForGroup(group: Int, tab: GenericCreativeTab) {
        creativeTabByGroup[group] = tab
    }
}
