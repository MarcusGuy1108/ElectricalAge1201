package mods.eln.item.electricalitem

import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.common.Tags
import net.minecraft.tags.BlockTags
import net.minecraft.core.BlockPos
import mods.eln.compat.*
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils
import net.minecraft.world.level.block.Block
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class ElectricalPickaxe(name: String, strengthOn: Float, strengthOff: Float,
                        energyStorage: Double, energyPerBlock: Double, chargePower: Double) : ElectricalTool(name, strengthOn, strengthOff, energyStorage, energyPerBlock, chargePower) {

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Opens holes. Right-click to open smaller holes."))
    }

    // 1.7.10 dug the rock, iron, glass and anvil materials plus a list of soft blocks; 1.20.1 has tags for those.
    private fun digs(state: BlockState) =
        state.`is`(BlockTags.MINEABLE_WITH_PICKAXE) || state.`is`(BlockTags.MINEABLE_WITH_SHOVEL) || state.`is`(Tags.Blocks.GLASS_BLOCKS)

    override fun getStrVsBlock(stack: ItemStack, block: Block?): Float {
        return if (block != null && digs(block.defaultBlockState())) getStrength(stack) else super.getStrVsBlock(stack, block)
    }

    override fun isCorrectToolForDrops(stack: ItemStack, state: BlockState) = digs(state)

    override fun onItemRightClick(s: ItemStack, w: Level, p: Player): ItemStack {
        if (!w.isRemote) {
            setConservative(p, s, !getConservative(s))
        }
        return s
    }

    private fun getConservative(s: ItemStack) =
        getNbt(s).getBoolean("conservative")

    private fun setConservative(p: Player?, s: ItemStack, state: Boolean) {
        editNbt(s) { putBoolean("conservative", state) }
        if (p != null) {
            Utils.addChatMessage(p, "Set land conservation to $state")
        }
    }

    override fun onBlockDestroyed(stack: ItemStack, w: Level, block: Block, x: Int, y: Int, z: Int, entity: LivingEntity): Boolean {
        val ok = super.onBlockDestroyed(stack, w, block, x, y, z, entity)
        if (entity !is Player) return ok
        if (!ok) return ok
        if (!getConservative(stack)) {
            for (a in (-1..1)) {
                for (b in (-1..0)) {
                    for (c in (-1..1)) {
                        if (a == 0 && b == 0 && c == 0) continue
                        val i = x+a
                        val j = y+b
                        val k = z+c
                        TreeCapitation.removeBlockWithDrops(entity, this, stack, w, i, j, k)
                    }
                }
            }
        }
        return ok
    }
}
