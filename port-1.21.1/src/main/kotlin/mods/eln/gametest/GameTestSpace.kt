package mods.eln.gametest

import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks

// 1.21 encases each test in barrier blocks; machines larger than the template need that room back.
internal fun clearRoom(helper: GameTestHelper, from: BlockPos, to: BlockPos) {
    for (pos in BlockPos.betweenClosed(helper.absolutePos(from), helper.absolutePos(to))) {
        if (helper.level.getBlockState(pos).`is`(Blocks.BARRIER)) helper.level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())
    }
}
