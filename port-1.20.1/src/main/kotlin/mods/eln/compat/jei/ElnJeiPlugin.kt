package mods.eln.compat.jei

import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.constants.VanillaTypes
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.recipe.category.IRecipeCategory
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import mods.eln.Eln
import mods.eln.craft.CraftingRecipes
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Recipe
import mods.eln.misc.RecipesList
import mods.eln.misc.Utils
import mods.eln.transparentnode.electricalmachine.ElectricalMachineDescriptor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/**
 * JEI pages for the machine recipes, which are Eln's own lists rather than data recipes (1.7.10 showed them in NEI
 * and the in-game wiki). Crafting and smelting recipes are data, so JEI lists them without help. Only loaded when JEI
 * is installed.
 */
@JeiPlugin
class ElnJeiPlugin : IModPlugin {
    private class Machine(val id: String, val title: String, val recipes: RecipesList) {
        val type: RecipeType<Recipe> = RecipeType(ResourceLocation(Eln.MODID, id), Recipe::class.java)
    }

    private val machines = listOf(
        Machine("macerator", tr("Macerator"), Eln.maceratorRecipes),
        Machine("compressor", tr("Compressor"), Eln.compressorRecipes),
        Machine("plate_machine", tr("Plate Machine"), Eln.plateMachineRecipes),
        Machine("magnetizer", tr("Magnetizer"), Eln.magnetiserRecipes),
        Machine("arc_furnace", tr("Arc Furnace"), Eln.arcFurnaceRecipes)
    )

    override fun getPluginUid(): ResourceLocation = ResourceLocation(Eln.MODID, "jei")

    override fun registerCategories(registration: IRecipeCategoryRegistration) {
        val guiHelper = registration.jeiHelpers.guiHelper
        for (machine in machines) {
            registration.addRecipeCategories(MachineCategory(machine.type, machine.title, catalysts(machine).firstOrNull(), guiHelper))
        }
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        // The lists fill at server start; a client on a dedicated server has never started one.
        if (machines.all { it.recipes.recipes.isEmpty() }) CraftingRecipes.registerMachineRecipes()
        for (machine in machines) registration.addRecipes(machine.type, machine.recipes.recipes.toList())
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        for (machine in machines) catalysts(machine).forEach { registration.addRecipeCatalyst(it, machine.type) }
    }

    private fun catalysts(machine: Machine) = Eln.transparentNodeItem.descriptors
        .filter { (it as? ElectricalMachineDescriptor)?.recipe === machine.recipes }
        .map { it.newItemStack() }
}

private class MachineCategory(
    private val type: RecipeType<Recipe>,
    private val title: String,
    icon: net.minecraft.world.item.ItemStack?,
    private val guiHelper: IGuiHelper
) : IRecipeCategory<Recipe> {
    private val icon: IDrawable = if (icon != null) guiHelper.createDrawableItemStack(icon) else guiHelper.createBlankDrawable(16, 16)
    private val arrow = guiHelper.recipeArrow

    override fun getRecipeType(): RecipeType<Recipe> = type
    override fun getTitle(): Component = Component.literal(title)
    override fun getIcon(): IDrawable = icon
    override fun getWidth(): Int = 120
    override fun getHeight(): Int = 34

    override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: Recipe, focuses: IFocusGroup) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1).setStandardSlotBackground().addItemStack(recipe.input)
        for ((i, output) in recipe.output.withIndex()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 50 + i * 18, 1).setOutputSlotBackground().addIngredient(VanillaTypes.ITEM_STACK, output)
        }
    }

    override fun draw(recipe: Recipe, recipeSlotsView: IRecipeSlotsView, graphics: GuiGraphics, mouseX: Double, mouseY: Double) {
        arrow.draw(graphics, 22, 1)
        graphics.drawString(Minecraft.getInstance().font, Utils.plotEnergy(tr("Energy:") + " ", recipe.energy), 1, 24, 0x404040, false)
    }
}
