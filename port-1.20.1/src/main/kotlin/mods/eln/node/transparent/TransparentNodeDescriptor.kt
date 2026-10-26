package mods.eln.node.transparent

import mods.eln.ghost.GhostGroup
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.Obj3D
import mods.eln.misc.Utils.entityLivingHorizontalViewDirection
import mods.eln.misc.Utils.entityLivingViewDirection
import mods.eln.misc.VoltageLevelColor
import mods.eln.node.transparent.TransparentNode.FrontType
import net.minecraft.world.level.block.HopperBlock
import net.minecraft.world.entity.LivingEntity
import mods.eln.client.GL11

open class TransparentNodeDescriptor @JvmOverloads constructor(
    name: String?,
    var ElementClass: Class<*>,
    var RenderClass: Class<*>,
    val tileEntityMetaTag: EntityMetaTag = EntityMetaTag.Basic) : GenericItemBlockUsingDamageDescriptor(name) {
    // Public (was protected) so the item model generator can draw the voltage background.
    @JvmField
    var voltageLevelColor = VoltageLevelColor.None
    @JvmField
    var ghostGroup: GhostGroup? = null

    // PORT: the IItemRenderer drawing is replaced by generated item models (voltage background + icon).

    fun objItemScale(obj: Obj3D?) {
        if (obj == null) return
        var factor = obj.yDim * 0.6f
        factor = factor.coerceAtLeast((obj.zMax.coerceAtLeast(-obj.xMin) + Math.max(obj.xMax, -obj.zMin)) * 0.7f)
        factor = 1f / factor
        GL11.glScalef(factor, factor, factor)
        GL11.glTranslatef((obj.zMin.coerceAtMost(obj.xMin) + obj.xMax.coerceAtLeast(obj.zMax)) / 2 - (obj.xMax + obj.xMin) / 2, 1.0f - (obj.xMax + obj.xMin) / 2 - (obj.zMax + obj.zMin) / 2 - (obj.yMax + obj.yMin) / 2, 0.0f)
    }

    open val frontType: FrontType?
        get() = FrontType.PlayerViewHorizontal

    open fun mustHaveFloor(): Boolean {
        return true
    }

    open fun mustHaveCeiling(): Boolean {
        return false
    }

    open fun mustHaveWall(): Boolean {
        return false
    }

    open fun mustHaveWallFrontInverse(): Boolean {
        return false
    }

    open fun checkCanPlace(coord: Coordinate?, front: Direction): String? {
        // 1.7.10 asked for opaque cubes; solid-render blocks are the 1.20.1 equivalent.
        fun solid(direction: Direction, allowHopper: Boolean = false): Boolean {
            val temp = Coordinate(coord!!)
            temp.move(direction)
            val state = temp.world().getBlockState(temp.blockPos)
            return state.isSolidRender(temp.world(), temp.blockPos) || (allowHopper && state.block is HopperBlock)
        }
        if (mustHaveFloor() && !solid(Direction.YN, true)) return tr("You can't place this block at this side")
        if (mustHaveCeiling() && !solid(Direction.YP)) return tr("You can't place this block at this side")
        if (mustHaveWallFrontInverse() && !solid(front.inverse)) return tr("You can't place this block at this side")
        if (mustHaveWall()) {
            val wall = solid(Direction.XN) || solid(Direction.XP) || solid(Direction.ZN) || solid(Direction.ZP)
            if (!wall) return tr("You can't place this block at this side")
        }
        val ghostGroup = getGhostGroupFront(front)
        return if (ghostGroup != null && !ghostGroup.canBePloted(coord!!)) tr("Not enough space for this block") else null
    }

    open fun getFrontFromPlace(side: Direction, entityLiving: LivingEntity?): Direction? {
        var front = Direction.XN
        when (frontType) {
            FrontType.BlockSide -> front = side
            FrontType.BlockSideInv -> front = side.inverse
            FrontType.PlayerView -> front = entityLivingViewDirection(entityLiving!!).inverse
            FrontType.PlayerViewHorizontal -> front = entityLivingHorizontalViewDirection(entityLiving!!).inverse
            null -> TODO()
        }
        return front
    }

    fun getGhostGroupFront(front: Direction?): GhostGroup? {
        return if (ghostGroup == null) null else ghostGroup!!.newRotate(front)
    }

    val ghostGroupUuid: Int
        get() = -1
    open val spawnDeltaX: Int
        get() = 0
    open val spawnDeltaY: Int
        get() = 0
    open val spawnDeltaZ: Int
        get() = 0

    // PORT: addCollisionBoxesToList (custom collision boxes) returns as a VoxelShape when a machine needs one; every
    // transparent node is a full block until then.
}
