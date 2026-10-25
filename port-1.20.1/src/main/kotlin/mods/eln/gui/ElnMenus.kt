package mods.eln.gui

import mods.eln.Eln
import mods.eln.misc.BasicContainer
import mods.eln.misc.Coordinate
import mods.eln.misc.Direction
import mods.eln.misc.Utils
import mods.eln.node.NodeBlockEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.minecraftforge.common.extensions.IForgeMenuType
import net.minecraftforge.network.NetworkHooks
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/**
 * Opens node GUIs, replacing the 1.7.10 GuiHandler. A node with a container opens through a menu: the server builds
 * the element's container, and the client builds the render's GUI around its own copy of it. A node without one gets
 * a packetOpenLocalGui so the client opens its screen alone, as 1.7.10 did.
 */
object ElnMenus {
    @JvmField
    val MENUS: DeferredRegister<MenuType<*>> = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Eln.MODID)

    // Every Eln container uses this type; the client-side factory only carries where the GUI is.
    @JvmField
    val NODE: RegistryObject<MenuType<AbstractContainerMenu>> = MENUS.register("node") {
        IForgeMenuType.create<AbstractContainerMenu> { id, _, buf -> NodeMenuTarget(id, buf.readBlockPos(), Direction.fromInt(buf.readByte().toInt())!!) }
    }

    fun openNodeGui(player: ServerPlayer, coordinate: Coordinate, side: Direction) {
        val pos = BlockPos(coordinate.x, coordinate.y, coordinate.z)
        val entity = coordinate.world().getBlockEntity(pos) as? NodeBlockEntity ?: return
        var opened = false
        val provider = SimpleMenuProvider({ id, _, p ->
            BasicContainer.withContainerId(id) { entity.newContainer(side, p) }?.also { opened = true }
        }, Component.empty())
        NetworkHooks.openScreen(player, provider) { buf ->
            buf.writeBlockPos(pos)
            buf.writeByte(side.int)
        }
        if (!opened) {
            val bos = ByteArrayOutputStream()
            val stream = DataOutputStream(bos)
            stream.writeByte(Eln.packetOpenLocalGui.toInt())
            stream.writeInt(side.int)
            stream.writeInt(pos.x)
            stream.writeInt(pos.y)
            stream.writeInt(pos.z)
            Utils.sendPacketToClient(bos, player)
        }
    }

    // Client: the screen for a menu opened by the server, built by the node render with the server's window id.
    @JvmStatic
    fun createScreen(menu: AbstractContainerMenu, inventory: Inventory): AbstractContainerScreen<AbstractContainerMenu> {
        val target = menu as NodeMenuTarget
        val player = Minecraft.getInstance().player!!
        val entity = player.level().getBlockEntity(target.pos) as? NodeBlockEntity
        @Suppress("UNCHECKED_CAST")
        val screen = BasicContainer.withContainerId(target.containerId) { entity?.newGuiDraw(target.side, player) }
            as? AbstractContainerScreen<AbstractContainerMenu>
        return screen ?: MissingNodeScreen(menu, inventory)
    }

    // Client, packetOpenLocalGui.
    @JvmStatic
    fun openLocalGui(side: Direction, pos: BlockPos) {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        val entity = player.level().getBlockEntity(pos) as? NodeBlockEntity ?: return
        entity.newGuiDraw(side, player)?.let { minecraft.setScreen(it) }
    }

    class NodeMenuTarget(id: Int, val pos: BlockPos, val side: Direction) : AbstractContainerMenu(NODE.get(), id) {
        override fun stillValid(player: Player) = true
        override fun quickMoveStack(player: Player, slotId: Int): ItemStack = ItemStack.EMPTY
    }

    // Shown, and closed at once, if the node or its render is gone by the time the menu reaches the client.
    private class MissingNodeScreen(menu: AbstractContainerMenu, inventory: Inventory) :
        AbstractContainerScreen<AbstractContainerMenu>(menu, inventory, Component.empty()) {
        override fun init() {
            super.init()
            onClose()
        }

        override fun renderBg(graphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {}
    }
}
