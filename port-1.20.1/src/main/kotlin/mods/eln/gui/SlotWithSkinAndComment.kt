package mods.eln.gui

import mods.eln.gui.ISlotSkin.SlotSkin
import net.minecraft.world.Container

open class SlotWithSkinAndComment(
    inventory: Container,
    slotIndex: Int,
    xDisplayPosition: Int,
    yDisplayPosition: Int,
    skin: SlotSkin,
    var comment: Array<String>
) : SlotWithSkin(
    inventory,
    slotIndex,
    xDisplayPosition,
    yDisplayPosition,
    skin
), ISlotWithComment {
    override fun getComment(list: MutableList<String>) {
        list.addAll(comment)
    }
}
