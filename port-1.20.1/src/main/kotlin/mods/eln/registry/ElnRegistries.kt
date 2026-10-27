package mods.eln.registry

import mods.eln.Eln
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.CreativeModeTab
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries

object ElnRegistries {
    @JvmField val BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Eln.MODID)
    @JvmField val ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Eln.MODID)
    @JvmField val BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Eln.MODID)
    @JvmField val ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Eln.MODID)
    @JvmField val CREATIVE_TABS: DeferredRegister<CreativeModeTab> = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Eln.MODID)

    private val usedItemNames = HashSet<String>()

    fun register(bus: IEventBus) {
        BLOCKS.register(bus)
        ITEMS.register(bus)
        BLOCK_ENTITY_TYPES.register(bus)
        ENTITY_TYPES.register(bus)
        CREATIVE_TABS.register(bus)
    }

    /**
     * Registry path for an item named after a descriptor, e.g. "Long Suspended Lamp Socket (No Swing)" becomes
     * "long_suspended_lamp_socket_no_swing". [id] disambiguates descriptors that share a name.
     */
    @JvmStatic
    fun itemName(descriptorName: String, id: Int): String {
        val base = descriptorName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifEmpty { "item" }
        val name = if (usedItemNames.add(base)) base else "${base}_$id"
        check(name == base || usedItemNames.add(name)) { "Duplicate Electrical Age item name $name" }
        return name
    }
}
