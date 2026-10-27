package mods.eln.item

import mods.eln.Eln
import mods.eln.falstad.FalstadImporter
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.misc.Utils.addChatMessage
import mods.eln.misc.UtilsClient
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

private const val MAX_FALSTAD_IMPORT_BYTES = 32000

class FalstadImportToolDescriptor(name: String) : GenericItemUsingDamageDescriptor(name, "configcopytool") {
    override fun onItemRightClick(s: ItemStack, w: Level, p: Player): ItemStack {
        if (w.isClientSide) FalstadImportClient.open()
        return s
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>, par4: Boolean) {
        list.add(tr("Right click to import a Falstad netlist from the clipboard."))
        list.add(tr("Places a simplified ELN build on flat ground near the player."))
    }
}

// Client only: never touched on a dedicated server.
private object FalstadImportClient {
    fun open() {
        Minecraft.getInstance().setScreen(FalstadImportGui())
    }
}

private class FalstadImportGui : Screen(Component.literal(tr("Falstad Import Tool"))) {
    override fun init() {
        addRenderableWidget(Button.builder(Component.literal(tr("Paste Clipboard"))) { paste() }
            .bounds(width / 2 - 70, height / 2 - 10, 140, 20).build())
        addRenderableWidget(Button.builder(Component.literal(tr("Cancel"))) { onClose() }
            .bounds(width / 2 - 70, height / 2 + 16, 140, 20).build())
    }

    private fun paste() {
        val minecraft = Minecraft.getInstance()
        val clipboard = minecraft.keyboardHandler.clipboard.trim()
        val player = minecraft.player
        if (clipboard.isEmpty()) {
            if (player != null) addChatMessage(player, tr("Falstad import: clipboard is empty."))
            return
        }

        if (!looksLikeFalstadData(clipboard)) {
            if (player != null) addChatMessage(player, tr("Falstad import: clipboard is not valid Falstad data."))
            return
        }

        val bytes = clipboard.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size > MAX_FALSTAD_IMPORT_BYTES) {
            if (player != null) {
                addChatMessage(
                    player,
                    tr(
                        "Falstad import: netlist is too large to send (%1$ bytes, limit %2$).",
                        bytes.size,
                        MAX_FALSTAD_IMPORT_BYTES
                    )
                )
            }
            return
        }
        val bos = ByteArrayOutputStream(bytes.size + 8)
        val stream = DataOutputStream(bos)
        stream.writeByte(Eln.packetFalstadImport.toInt())
        stream.writeInt(bytes.size)
        stream.write(bytes)
        UtilsClient.sendPacketToServer(bos)
        onClose()
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        renderBackground(graphics)
        graphics.drawCenteredString(font, tr("Falstad Import Tool"), width / 2, height / 2 - 42, 0xFFFFFF)
        graphics.drawCenteredString(font, tr("Reads Falstad text from the system clipboard."), width / 2, height / 2 - 28, 0xA0A0A0)
        super.render(graphics, mouseX, mouseY, partialTicks)
    }

    override fun isPauseScreen(): Boolean = false
}

object FalstadImportPacketHandler {
    fun handle(player: ServerPlayer, bytes: ByteArray) {
        FalstadImporter.importFromClipboardAsync(player, String(bytes, StandardCharsets.UTF_8))
    }
}

private fun looksLikeFalstadData(text: String): Boolean {
    val trimmed = text.trimStart()
    if (trimmed.startsWith("<cir")) return true
    val firstLine = trimmed.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("$") }
        ?: return false
    val token = firstLine.substringBefore(' ')
    return token.matches(Regex("[A-Za-z]+|\\d+"))
}
