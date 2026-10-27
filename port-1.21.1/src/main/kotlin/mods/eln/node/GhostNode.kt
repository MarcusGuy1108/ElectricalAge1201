package mods.eln.node

/** A node living in a ghost block of a multiblock machine; the machine recreates it, so it is never saved. */
abstract class GhostNode : NodeBase() {
    override fun mustBeSaved(): Boolean {
        return false
    }

    // Same id as GhostBlock.nodeUuid.
    override val nodeUuid = "g"
}
