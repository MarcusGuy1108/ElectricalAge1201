package mods.eln.datagen

import java.util.function.Consumer
import net.minecraft.data.recipes.FinishedRecipe
import net.minecraft.data.recipes.RecipeProvider
import mods.eln.craft.CraftingRecipes
import net.minecraftforge.common.Tags
import net.minecraft.tags.BlockTags
import mods.eln.block.ArcBlock
import mods.eln.item.CopperGear
import mods.eln.item.electricalitem.ElectricalArmor
import mods.eln.ore.ElnOre
import mods.eln.ghost.GhostBlock
import mods.eln.misc.OreDictionaryTags
import java.util.concurrent.CompletableFuture
import net.minecraftforge.common.data.BlockTagsProvider
import net.minecraft.world.level.block.Block
import net.minecraft.tags.ItemTags
import net.minecraft.resources.ResourceLocation
import net.minecraft.data.tags.TagsProvider
import net.minecraft.data.tags.ItemTagsProvider
import net.minecraft.core.HolderLookup
import mods.eln.node.transparent.TransparentNodeBlock
import mods.eln.node.transparent.TransparentNodeDescriptor
import mods.eln.lightblock.LightBlock
import mods.eln.Eln
import mods.eln.generic.CreativeTabPopulator
import mods.eln.misc.Utils
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.six.SixNodeBlockRegistration
import mods.eln.node.six.SixNodeDescriptor
import net.minecraft.data.PackOutput
import net.minecraft.server.packs.PackType
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemDisplayContext
import net.minecraftforge.client.model.generators.BlockStateProvider
import net.minecraftforge.client.model.generators.ItemModelBuilder
import net.minecraftforge.client.model.generators.ItemModelProvider
import net.minecraftforge.client.model.generators.ModelFile
import net.minecraftforge.client.model.generators.loaders.SeparateTransformsModelBuilder
import net.minecraftforge.common.data.ExistingFileHelper
import net.minecraftforge.data.event.GatherDataEvent
import net.minecraftforge.registries.ForgeRegistries

/** `./gradlew runData` writes the item and block models into src/generated/resources. */
object ElnDataGen {
    fun gatherData(event: GatherDataEvent) {
        val output = event.generator.packOutput
        event.generator.addProvider(event.includeClient(), ElnItemModels(output, event.existingFileHelper))
        event.generator.addProvider(event.includeClient(), ElnBlockStates(output, event.existingFileHelper))
        val blockTags = ElnBlockTags(output, event.lookupProvider, event.existingFileHelper)
        event.generator.addProvider(event.includeServer(), blockTags)
        event.generator.addProvider(event.includeServer(), ElnItemTags(output, event.lookupProvider, blockTags.contentsGetter(), event.existingFileHelper))
        event.generator.addProvider(event.includeServer(), ElnWorldGen(output, event.lookupProvider))
        event.generator.addProvider(event.includeServer(), elnLootTables(output))
        event.generator.addProvider(event.includeServer(), object : RecipeProvider(output) {
            override fun buildRecipes(consumer: Consumer<FinishedRecipe>) = CraftingRecipes.generate(consumer)
        })
    }
}

/**
 * 1.7.10 drew descriptor icons with an IItemRenderer: the icon, plus the voltage level background in inventories.
 * Here each descriptor item gets an item/generated model, with the background layer only in the GUI perspective.
 */
class ElnItemModels(output: PackOutput, files: ExistingFileHelper) : ItemModelProvider(output, Eln.MODID, files) {
    override fun registerModels() {
        for (registrar in CreativeTabPopulator.blockItems) {
            for (descriptor in registrar.descriptors) {
                val voltage = when (descriptor) {
                    is SixNodeDescriptor -> descriptor.voltageLevelColor
                    is TransparentNodeDescriptor -> descriptor.voltageLevelColor
                    else -> VoltageLevelColor.None
                }
                descriptorModel(descriptor.parentItem!!, voltage, "blocks/" + descriptor.iconName)
            }
        }
        for (registrar in CreativeTabPopulator.genericItems) {
            for (descriptor in registrar.subItemList.values) {
                descriptorModel(descriptor.parentItem!!, descriptor.voltageLevelColor, "items/" + descriptor.IconName!!.removePrefix("eln:"))
            }
        }
        // The block models come from ElnBlockStates in the same run, so they cannot be checked for yet.
        for (ore in ElnOre.values()) getBuilder(ore.id).parent(ModelFile.UncheckedModelFile(modLoc("block/" + ore.id)))
        for (arc in ArcBlock.values()) getBuilder(arc.id).parent(ModelFile.UncheckedModelFile(modLoc("block/" + arc.id)))
        for (item in ElectricalArmor.items) {
            val id = item.id.path
            withExistingParent(id, mcLoc("item/generated")).texture("layer0", modLoc("items/" + id.replace("e_coal", "ecoal")))
        }
        for (id in CopperGear.ids) {
            withExistingParent(id, mcLoc(if (id in CopperGear.tools) "item/handheld" else "item/generated")).texture("layer0", modLoc("items/$id"))
        }
    }

    private fun descriptorModel(item: Item, voltage: VoltageLevelColor, icon: String) {
        val name = ForgeRegistries.ITEMS.getKey(item)!!.path
        val iconLayer = texture(icon)
        if (iconLayer == null) Eln.logger.warn("No icon texture $icon for item $name")
        val background = voltage.backgroundTexture?.let { texture(it) }
        val base = layers(nested(), listOfNotNull(iconLayer))
        if (background == null) {
            layers(getBuilder(name), listOfNotNull(iconLayer))
            return
        }
        getBuilder(name).customLoader(SeparateTransformsModelBuilder<ItemModelBuilder>::begin)
            .base(base)
            .perspective(ItemDisplayContext.GUI, layers(nested(), listOfNotNull(background, iconLayer)))
            .end()
    }

    private fun layers(builder: ItemModelBuilder, textures: List<String>): ItemModelBuilder {
        builder.parent(ModelFile.UncheckedModelFile("item/generated"))
        textures.forEachIndexed { index, texture -> builder.texture("layer$index", texture) }
        return builder
    }

    // Texture reference for a legacy texture path, or null if the shared assets do not have it.
    private fun texture(path: String): String? {
        val location = modLoc(Utils.resourcePath(path))
        return if (existingFileHelper.exists(location, PackType.CLIENT_RESOURCES, ".png", "textures")) location.toString() else null
    }
}

/** Node blocks are drawn by block entity renderers; their block models only supply the breaking particle. */
class ElnBlockStates(output: PackOutput, files: ExistingFileHelper) : BlockStateProvider(output, Eln.MODID, files) {
    override fun registerStatesAndModels() {
        simpleBlock(
            SixNodeBlockRegistration.BLOCK.get(),
            models().getBuilder("six_node").texture("particle", mcLoc("block/stone"))
        )
        // 1.7.10 used the iron block texture for transparent node particles.
        simpleBlock(
            TransparentNodeBlock.BLOCK.get(),
            models().getBuilder("transparent_node").texture("particle", mcLoc("block/iron_block"))
        )
        // Invisible; the model only exists so the block state resolves.
        simpleBlock(LightBlock.BLOCK.get(), models().getBuilder("light").texture("particle", mcLoc("block/glass")))
        simpleBlock(GhostBlock.BLOCK.get(), models().getBuilder("ghost").texture("particle", mcLoc("block/iron_block")))
        for (ore in ElnOre.values()) simpleBlock(ore.block.get(), models().cubeAll(ore.id, modLoc("blocks/" + ore.texture)))
        for (arc in ArcBlock.values()) simpleBlock(arc.block.get(), models().cubeAll(arc.id, modLoc("blocks/" + arc.id)))
    }
}

class ElnBlockTags(output: PackOutput, lookup: CompletableFuture<HolderLookup.Provider>, files: ExistingFileHelper) :
    BlockTagsProvider(output, lookup, Eln.MODID, files) {
    override fun addTags(provider: HolderLookup.Provider) {
        for (ore in ElnOre.values()) {
            val block = ore.block.get()
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block)
            tag(BlockTags.NEEDS_STONE_TOOL).add(block)
            tag(Tags.Blocks.ORES).add(block)
            tag(BlockTags.create(ResourceLocation("forge", "ores/" + ore.material))).add(block)
        }
        for (arc in ArcBlock.values()) {
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(arc.block.get())
            tag(Tags.Blocks.STORAGE_BLOCKS).add(arc.block.get())
            tag(BlockTags.create(ResourceLocation("forge", "storage_blocks/" + arc.material))).add(arc.block.get())
        }
    }
}

/** 1.7.10 registered materials in the Forge ore dictionary; 1.20.1 uses item tags, e.g. ingotCopper -> forge:ingots/copper. */
class ElnItemTags(
    output: PackOutput,
    lookup: CompletableFuture<HolderLookup.Provider>,
    blockTags: CompletableFuture<TagsProvider.TagLookup<Block>>,
    files: ExistingFileHelper
) : ItemTagsProvider(output, lookup, blockTags, Eln.MODID, files) {
    override fun addTags(provider: HolderLookup.Provider) {
        for ((name, descriptors) in Eln.oreDictionary) {
            val appender = tag(ItemTags.create(OreDictionaryTags.tagFor(name)))
            for (descriptor in descriptors) appender.add(descriptor.parentItem!!)
        }
        copy(Tags.Blocks.ORES, Tags.Items.ORES)
        for (ore in ElnOre.values()) {
            copy(BlockTags.create(ResourceLocation("forge", "ores/" + ore.material)), ItemTags.create(ResourceLocation("forge", "ores/" + ore.material)))
        }
        copy(Tags.Blocks.STORAGE_BLOCKS, Tags.Items.STORAGE_BLOCKS)
        for (arc in ArcBlock.values()) {
            copy(BlockTags.create(ResourceLocation("forge", "storage_blocks/" + arc.material)), ItemTags.create(ResourceLocation("forge", "storage_blocks/" + arc.material)))
        }
    }
}
