package mods.eln.railroad

import mods.eln.generic.GenericItemUsingDamageDescriptor
import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class ElectricMinecartItem(name: String) : GenericItemUsingDamageDescriptor(name) {
    override fun onItemUse(
        stack: ItemStack?,
        player: Player?,
        world: Level?,
        x: Int,
        y: Int,
        z: Int,
        side: Int,
        vx: Float,
        vy: Float,
        vz: Float
    ): Boolean {
        if (world == null || stack == null) return false
        if (!world.getBlockState(BlockPos(x, y, z)).`is`(BlockTags.RAILS)) return false
        if (!world.isClientSide) {
            val minecart = EntityElectricMinecart(world, x + 0.5, y + 0.0625, z + 0.5)
            if (stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
                minecart.customName = stack.hoverName
            }
            world.addFreshEntity(minecart)
        }
        if (player?.abilities?.instabuild != true) stack.shrink(1)
        return true
    }
}
