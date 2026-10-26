package mods.eln.datagen

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
    }
}
