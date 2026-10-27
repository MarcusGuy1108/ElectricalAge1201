package mods.eln.gametest

import mods.eln.Eln
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.misc.Direction
import mods.eln.node.NodeBlockEntity
import mods.eln.node.six.SixNodeItem
import mods.eln.node.transparent.TransparentNodeItem
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate

@GameTestHolder(Eln.MODID)
@PrefixGameTestTemplate(false)
object SmokeGameTests {
    private const val TICKS_PER_NODE = 4

    // Every registered machine and six-node is placed on the same floor in turn, simulated for a few ticks and
    // broken again. Refused placements (a wall-mounted part on a floor) are fine; exceptions fail the test, and one
    // thrown during a tick stops the server with its stack trace. Own batch: the larger machines' ghost blocks reach
    // past the 3x3x3 template.
    @GameTest(template = "empty3x3x3", batch = "smoke", timeoutTicks = 4000)
    @JvmStatic
    fun everyNodePlacesRunsAndBreaks(helper: GameTestHelper) {
        val pos = BlockPos(1, 1, 1)
        for (x in -4..6) for (z in -4..6) helper.setBlock(BlockPos(x, 0, z), Blocks.STONE)
        val queue = ArrayDeque<GenericItemBlockUsingDamageDescriptor>()
        queue.addAll(Eln.transparentNodeItem.descriptors)
        queue.addAll(Eln.sixNodeItem.descriptors)
        val total = queue.size
        val failures = ArrayList<String>()
        var placed = 0
        val refused = ArrayList<String>()
        var ticks = 0
        helper.onEachTick {
            if (ticks++ % TICKS_PER_NODE != 0) return@onEachTick
            // Some machines sit a few blocks from the clicked spot (their spawn delta); break whichever node is there.
            for (at in BlockPos.betweenClosed(helper.absolutePos(pos).offset(-6, -1, -6), helper.absolutePos(pos).offset(6, 3, 6))) {
                if (helper.level.getBlockEntity(at) is NodeBlockEntity) helper.level.destroyBlock(at, false)
            }
            val descriptor = queue.removeFirstOrNull()
            if (descriptor == null) {
                helper.assertTrue(failures.isEmpty(), "${failures.size} failed: ${failures.joinToString("; ")}")
                helper.assertTrue(placed > 0, "nothing placed")
                Eln.logger.info("Smoke test placed $placed of $total nodes; refused: ${refused.joinToString()}")
                helper.succeed()
                return@onEachTick
            }
            try {
                val stack = descriptor.newItemStack()
                val ok = when (val item = stack.item) {
                    is TransparentNodeItem -> item.placeBlockAt(stack, helper.makeMockPlayer(), helper.level, helper.absolutePos(pos), Direction.YP)
                    is SixNodeItem -> item.placeBlockAt(stack, helper.makeMockPlayer(), helper.level, helper.absolutePos(pos), Direction.YN)
                    else -> false
                }
                if (ok) placed++ else refused.add(descriptor.name)
            } catch (e: Exception) {
                failures.add("${descriptor.name}: $e")
            }
        }
    }
}
