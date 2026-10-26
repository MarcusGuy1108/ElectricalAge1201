package mods.eln.item

import mods.eln.Eln
import mods.eln.generic.CreativeTabPopulator
import mods.eln.registry.ElnRegistries
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorMaterials
import net.minecraft.world.item.AxeItem
import net.minecraft.world.item.HoeItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.PickaxeItem
import net.minecraft.world.item.ShovelItem
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.Tiers
import net.minecraftforge.registries.RegistryObject

/**
 * Copper armor and tools. 1.7.10 made them iron grade (the iron tool and armor materials) with copper textures, so
 * they use the vanilla iron tier, iron armor material and iron attack values here.
 */
object CopperGear {
    /** Registry path to item, e.g. "copper_pickaxe"; [ids] is in creative tab order. */
    val items = LinkedHashMap<String, RegistryObject<Item>>()
    val ids = listOf("copper_helmet", "copper_chestplate", "copper_leggings", "copper_boots", "copper_sword", "copper_pickaxe", "copper_axe", "copper_shovel", "copper_hoe")
    val tools = setOf("copper_sword", "copper_pickaxe", "copper_axe", "copper_shovel", "copper_hoe")

    @JvmStatic
    fun get(id: String): Item = items.getValue(id).get()

    fun register() {
        armor("copper_helmet", "Copper Helmet", ArmorItem.Type.HELMET)
        armor("copper_chestplate", "Copper Chestplate", ArmorItem.Type.CHESTPLATE)
        armor("copper_leggings", "Copper Leggings", ArmorItem.Type.LEGGINGS)
        armor("copper_boots", "Copper Boots", ArmorItem.Type.BOOTS)
        item("copper_sword") { key -> object : SwordItem(Tiers.IRON, 3, -2.4f, Properties()) { override fun getDescriptionId() = key } }
        item("copper_pickaxe") { key -> object : PickaxeItem(Tiers.IRON, 1, -2.8f, Properties()) { override fun getDescriptionId() = key } }
        item("copper_axe") { key -> object : AxeItem(Tiers.IRON, 6f, -3.1f, Properties()) { override fun getDescriptionId() = key } }
        item("copper_shovel") { key -> object : ShovelItem(Tiers.IRON, 1.5f, -3f, Properties()) { override fun getDescriptionId() = key } }
        item("copper_hoe") { key -> object : HoeItem(Tiers.IRON, -2, -1f, Properties()) { override fun getDescriptionId() = key } }
    }

    private fun armor(id: String, name: String, type: ArmorItem.Type) {
        items[id] = ElnRegistries.ITEMS.register(id) {
            object : ArmorItem(ArmorMaterials.IRON, type, Properties()) {
                override fun getDescriptionId() = legacyKey(name)
                override fun getArmorTexture(stack: ItemStack, entity: Entity, slot: EquipmentSlot, layer: String?): String =
                    "eln:textures/armor/copper_layer_" + (if (slot == EquipmentSlot.LEGS) 2 else 1) + ".png"
            }
        }
        CreativeTabPopulator.addStack({ Eln.creativeTabToolsArmor }) { ItemStack(get(id)) }
    }

    private fun item(id: String, factory: (String) -> Item) {
        val name = id.split('_').joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        items[id] = ElnRegistries.ITEMS.register(id) { factory(legacyKey(name)) }
        CreativeTabPopulator.addStack({ Eln.creativeTabToolsArmor }) { ItemStack(get(id)) }
    }

    // The 1.7.10 language files name them "item.Copper Helmet.name" and so on.
    private fun legacyKey(name: String) = "item.$name.name"
}
