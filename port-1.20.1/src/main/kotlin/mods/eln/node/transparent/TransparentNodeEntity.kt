package mods.eln.node.transparent

import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.wrapper.SidedInvWrapper
import net.minecraft.world.WorldlyContainer
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.capabilities.Capability
import mods.eln.Eln
import mods.eln.cable.CableRenderDescriptor
import mods.eln.misc.Direction
import mods.eln.misc.LRDU
import mods.eln.node.NodeBlockEntity
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.lang.reflect.InvocationTargetException

open class TransparentNodeEntity(pos: BlockPos, state: BlockState) :
    NodeBlockEntity(TransparentNodeBlock.ENTITY.get(), pos, state) {
    var elementRender: TransparentNodeElementRender? = null

    // The element's tank, as 1.7.10 TransparentNodeEntityWithFluid exposed it, and its inventory with the 1.7.10
    // ISidedInventory side access, for hoppers and pipes. Server side only, like the node. Cached and invalidated with
    // the block entity, so pipes holding on to them let go when the machine is broken.
    private var fluidCapability: LazyOptional<IFluidHandler>? = null
    private val itemCapabilities = arrayOfNulls<LazyOptional<IItemHandler>>(6)

    override fun <T> getCapability(cap: Capability<T>, side: net.minecraft.core.Direction?): LazyOptional<T> {
        val node = node as? TransparentNode
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            val handler = node?.fluidHandler
            if (handler != null) return (fluidCapability ?: LazyOptional.of { handler }.also { fluidCapability = it }).cast()
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER && side != null) {
            val inventory = node?.getInventory(null) as? WorldlyContainer
            if (inventory != null) {
                val handler = itemCapabilities[side.ordinal]
                    ?: LazyOptional.of<IItemHandler> { SidedInvWrapper(inventory, side) }.also { itemCapabilities[side.ordinal] = it }
                return handler.cast()
            }
        }
        return super.getCapability(cap, side)
    }

    override fun invalidateCaps() {
        super.invalidateCaps()
        fluidCapability?.invalidate()
        itemCapabilities.forEach { it?.invalidate() }
    }

    var elementRenderId: Short = 0

    override fun getCableRender(side: Direction, lrdu: LRDU): CableRenderDescriptor? {
        return if (elementRender == null) null else elementRender!!.getCableRenderSide(side, lrdu)
    }

    override fun serverPublishUnserialize(stream: DataInputStream) {
        super.serverPublishUnserialize(stream)
        try {
            val id = stream.readShort()
            if (id.toInt() == 0) {
                elementRenderId = 0.toShort()
                elementRender = null
            } else {
                if (id != elementRenderId) {
                    elementRenderId = id
                    val descriptor = Eln.transparentNodeItem.getDescriptor(id.toInt())
                    elementRender = descriptor!!.RenderClass.getConstructor(TransparentNodeEntity::class.java, TransparentNodeDescriptor::class.java).newInstance(this, descriptor) as TransparentNodeElementRender
                }
                elementRender!!.networkUnserialize(stream)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun newContainer(side: Direction, player: Player): AbstractContainerMenu? {
        val n = node as TransparentNode? ?: return null
        return n.newContainer(side, player)
    }

    override fun newGuiDraw(side: Direction, player: Player): Screen? {
        return elementRender?.newGuiDraw(side, player)
    }

    override fun preparePacketForServer(stream: DataOutputStream) {
        try {
            super.preparePacketForServer(stream)
            stream.writeShort(elementRenderId.toInt())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun sendPacketToServer(bos: ByteArrayOutputStream?) {
        super.sendPacketToServer(bos)
    }

    override fun cameraDrawOptimisation(): Boolean {
        return if (elementRender == null) super.cameraDrawOptimisation() else elementRender!!.cameraDrawOptimisation()
    }

    override fun unoptimizedRenderBoundingBox(): AABB {
        return if (elementRender == null) super.unoptimizedRenderBoundingBox() else elementRender!!.unoptimizedRenderBoundingBox()
    }

    override fun serverPacketUnserialize(stream: DataInputStream) {
        super.serverPacketUnserialize(stream)
        if (elementRender != null) elementRender!!.serverPacketUnserialize(stream)
    }

    override val nodeUuid: String
        get() = TransparentNodeBlock.NODE_UUID

    override fun destructor() {
        if (elementRender != null) elementRender!!.destructor()
        super.destructor()
    }

    override fun clientRefresh(deltaT: Float) {
        if (elementRender != null) {
            elementRender!!.refresh(deltaT)
        }
    }

    override fun tileEntityNeighborSpawn() {
        if (elementRender != null) elementRender!!.notifyNeighborSpawn()
    }

    override fun isProvidingWeakPower(side: Direction?): Int {
        return 0
    }
}
