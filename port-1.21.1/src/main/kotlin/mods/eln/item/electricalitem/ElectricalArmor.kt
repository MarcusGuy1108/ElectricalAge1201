package mods.eln.item.electricalitem

import mods.eln.compat.*
import net.minecraft.resources.ResourceLocation
import mods.eln.Eln
import mods.eln.generic.CreativeTabPopulator
import mods.eln.i18n.I18N.tr
import mods.eln.item.electricalinterface.IItemEnergyBattery
import mods.eln.registry.ElnRegistries
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.DamageTypeTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorMaterial
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.Level
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent
import mods.eln.registry.RegistryObject
import kotlin.math.max
import kotlin.math.min

/**
 * E-coal armor: absorbs damage with stored energy instead of armor points. 1.7.10 did this through Forge's
 * ISpecialArmor, which 1.20.1 removed; [absorbDamage] applies the same rule from a LivingHurtEvent. Each charged piece
 * absorbs up to [ratioMax] of the damage (the full set, 100%), scaled down while it holds less than [ratioMaxEnergy],
 * and pays [energyPerDamage] joules per damage point it absorbs.
 */
class ElectricalArmor(
    type: Type,
    private val legacyName: String,
    var energyStorage: Double,
    var chargePower: Double,
    var ratioMax: Double,
    var ratioMaxEnergy: Double,
    var energyPerDamage: Double
) : ArmorItem(MATERIAL, type, Properties().stacksTo(1)), IItemEnergyBattery {

    override fun getDescriptionId() = "item.$legacyName.name"

    override fun getArmorTexture(stack: ItemStack, entity: Entity, slot: EquipmentSlot, layer: ArmorMaterial.Layer, innerModel: Boolean): ResourceLocation =
        ResourceLocation.fromNamespaceAndPath("eln", "textures/armor/ecoal_layer_" + (if (slot == EquipmentSlot.LEGS) 2 else 1) + ".png")

    override fun isValidRepairItem(stack: ItemStack, repair: ItemStack) = false

    override fun appendHoverText(stack: ItemStack, context: TooltipContext, tooltip: MutableList<Component>, flag: TooltipFlag) {
        tooltip.add(Component.literal(tr("Charge power: %1\$W", chargePower.toInt())))
        tooltip.add(Component.literal(tr("Stored energy: %1\$J (%2$%)", getEnergy(stack), (getEnergy(stack) / energyStorage * 100).toInt())))
    }

    fun absorbRatio(stack: ItemStack) = min(1.0, getEnergy(stack) / ratioMaxEnergy) * ratioMax

    override fun getEnergy(stack: ItemStack) = stack.tag?.getDouble("energy") ?: 0.0
    override fun setEnergy(stack: ItemStack, value: Double) = stack.editTag { putDouble("energy", value) }
    override fun getEnergyMax(stack: ItemStack) = energyStorage
    override fun getChargePower(stack: ItemStack) = chargePower
    override fun getDischagePower(stack: ItemStack) = 0.0
    override fun getPriority(stack: ItemStack) = 0
    override fun electricalItemUpdate(stack: ItemStack, time: Double) {}

    companion object {
        /** No armor points of its own (the 1.7.10 E-coal material's points only showed in the HUD); energy does the work. */
        private val MATERIAL = ElnRegistries.ARMOR_MATERIALS.register("ecoal") { id: ResourceLocation ->
            ArmorMaterial(ArmorItem.Type.values().associateWith { 0 }, 0, SoundEvents.ARMOR_EQUIP_IRON, { Ingredient.EMPTY }, listOf(ArmorMaterial.Layer(id)), 0f, 0f)
        }

        private const val ENERGY_PER_DAMAGE = 500.0
        val items = ArrayList<RegistryObject<ElectricalArmor>>()

        fun register() {
            piece("e_coal_helmet", "E-Coal Helmet", Type.HELMET, 3)
            piece("e_coal_chestplate", "E-Coal Chestplate", Type.CHESTPLATE, 8)
            piece("e_coal_leggings", "E-Coal Leggings", Type.LEGGINGS, 6)
            piece("e_coal_boots", "E-Coal Boots", Type.BOOTS, 3)
        }

        private fun piece(id: String, name: String, type: Type, armor: Int) {
            val item = ElnRegistries.ITEMS.register(id) { _: ResourceLocation ->
                ElectricalArmor(type, name, 8000.0, 2000.0, armor / 20.0, armor * ENERGY_PER_DAMAGE, ENERGY_PER_DAMAGE)
            }
            items.add(item)
            CreativeTabPopulator.addStack({ Eln.creativeTabToolsArmor }) { ItemStack(item.get()) }
        }

        @JvmStatic
        fun byName(name: String): ItemStack =
            items.map { it.get() }.firstOrNull { it.legacyName == name }?.let { ItemStack(it) } ?: ItemStack.EMPTY

        /** The 1.7.10 ISpecialArmor rule: the pieces' ratios add up (capped at 1) and each pays for its share. */
        fun absorbDamage(event: LivingIncomingDamageEvent) {
            if (event.source.`is`(DamageTypeTags.BYPASSES_ARMOR)) return
            val pieces = event.entity.armorSlots.filter { it.item is ElectricalArmor }
            if (pieces.isEmpty()) return
            val ratios = pieces.map { (it.item as ElectricalArmor).absorbRatio(it) }
            val scale = 1.0 / max(1.0, ratios.sum())
            val damage = event.amount.toDouble()
            var absorbed = 0.0
            for ((stack, ratio) in pieces.zip(ratios)) {
                val armor = stack.item as ElectricalArmor
                val share = min(damage * ratio * scale, armor.getEnergy(stack) / armor.energyPerDamage)
                if (share <= 0.0) continue
                armor.setEnergy(stack, max(0.0, armor.getEnergy(stack) - max(1.0, share) * armor.energyPerDamage))
                absorbed += share
            }
            event.amount = (damage - absorbed).toFloat()
        }
    }
}
