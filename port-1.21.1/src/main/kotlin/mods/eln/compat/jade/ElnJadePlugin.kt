package mods.eln.compat.jade

import mods.eln.Eln
import mods.eln.node.six.SixNode
import mods.eln.node.six.SixNodeBlock
import mods.eln.node.six.SixNodeEntity
import mods.eln.node.transparent.TransparentNode
import mods.eln.node.transparent.TransparentNodeBlock
import mods.eln.node.transparent.TransparentNodeEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.IServerDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.IWailaClientRegistration
import snownee.jade.api.IWailaCommonRegistration
import snownee.jade.api.IWailaPlugin
import snownee.jade.api.WailaPlugin
import snownee.jade.api.config.IPluginConfig

/**
 * Jade (the modern WAILA) shows each node's 1.7.10 WAILA lines: the server reads [TransparentNodeElement.getWaila] or,
 * for a six-node, the looked-at element's, and the client lists them. Only loaded when Jade is installed.
 *
 * shortcut: ghost blocks (the outer parts of large machines) show nothing, as Jade only asks the server about blocks
 * with a block entity; look at the machine's own block. A block entity on the ghost block would lift that.
 */
@WailaPlugin
class ElnJadePlugin : IWailaPlugin {
    override fun register(registration: IWailaCommonRegistration) {
        registration.registerBlockDataProvider(NodeInfoProvider, SixNodeEntity::class.java)
        registration.registerBlockDataProvider(NodeInfoProvider, TransparentNodeEntity::class.java)
    }

    override fun registerClient(registration: IWailaClientRegistration) {
        registration.registerBlockComponent(NodeInfoProvider, SixNodeBlock::class.java)
        registration.registerBlockComponent(NodeInfoProvider, TransparentNodeBlock::class.java)
        // Name and icon of the element or machine looked at, not of the shared node block.
        registration.usePickedResult(Eln.sixNodeBlock)
        registration.usePickedResult(TransparentNodeBlock.BLOCK.get())
    }
}

object NodeInfoProvider : IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    private val UID = ResourceLocation.fromNamespaceAndPath(Eln.MODID, "node_info")
    private const val KEY = "ElnInfo"

    override fun getUid(): ResourceLocation = UID

    override fun appendServerData(data: CompoundTag, accessor: BlockAccessor) {
        val info = when (val entity = accessor.blockEntity) {
            is SixNodeEntity -> {
                val side = SixNodeBlock.elementSideAt(entity, accessor.position, accessor.hitResult.location) ?: return
                (entity.node as? SixNode)?.sideElementList?.get(side.int)?.getWaila()
            }
            is TransparentNodeEntity -> (entity.node as? TransparentNode)?.element?.getWaila()
            else -> null
        } ?: return
        data.put(KEY, ListTag().apply { info.forEach { (label, value) -> add(StringTag.valueOf("$label: §f$value")) } })
    }

    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        accessor.serverData.getList(KEY, Tag.TAG_STRING.toInt()).forEach { tooltip.add(Component.literal(it.asString)) }
    }
}
