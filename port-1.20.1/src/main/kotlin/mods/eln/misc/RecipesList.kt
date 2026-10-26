@file:Suppress("NAME_SHADOWING")
package mods.eln.misc

import mods.eln.compat.*
import mods.eln.Eln
import net.minecraft.world.item.ItemStack
import java.util.*
import kotlin.collections.ArrayList

class RecipesList {
    val recipes = ArrayList<Recipe>()
    val machines = ArrayList<ItemStack>()
    fun addRecipe(recipe: Recipe) {
        recipes.add(recipe)
        recipe.setMachineList(machines)
    }

    fun addMachine(machine: ItemStack) {
        machines.add(machine)
    }

    fun getRecipe(input: ItemStack?): Recipe? {
        for (r in recipes) {
            if (r.canBeCraftedBy(input)) return r
        }
        return null
    }

    fun getRecipeFromOutput(output: ItemStack?): ArrayList<Recipe> {
        if (output == null) return ArrayList()
        val list = ArrayList<Recipe>()
        for (r in recipes) {
            for (stack in r.outputCopy) {
                if (stack != null) {
                    if (Utils.areSame(stack, output)) {
                        list.add(r)
                        break
                    }
                }
            }
        }
        return list
    }

    companion object {
        val listOfList = ArrayList<RecipesList>()
        // PORT: getGlobalRecipeWithOutput/Input (wiki and NEI recipe lookups, furnace recipes included) return with
        // a recipe viewer integration.
    }

    init {
        listOfList.add(this)
    }
}
