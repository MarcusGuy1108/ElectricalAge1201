package mods.eln.generic

import mods.eln.registry.ElnRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraftforge.registries.RegistryObject

/**
 * 1.20.1 creative tabs are registry entries; this keeps the 1.7.10 constructor shape (label + icon) and lists the
 * descriptors that [CreativeTabPopulator] assigned to it. The title uses the legacy "itemGroup.<label>" lang key.
 */
class GenericCreativeTab(val label: String, var icon: () -> ItemStack) {
    val tab: RegistryObject<CreativeModeTab> = ElnRegistries.CREATIVE_TABS.register(label.lowercase()) {
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.$label"))
            .icon { icon() }
            .displayItems { _, output -> CreativeTabPopulator.addEntries(this, output) }
            .build()
    }
}
