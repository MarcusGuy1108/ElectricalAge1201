package mods.eln.node

import java.io.DataInputStream

interface INodeEntity {
    val nodeUuid: String
    fun serverPublishUnserialize(stream: DataInputStream)
    fun serverPacketUnserialize(stream: DataInputStream)

    // PORT: newGuiDraw/newContainer return as Screen/AbstractContainerMenu factories with the GUI layer.
}
