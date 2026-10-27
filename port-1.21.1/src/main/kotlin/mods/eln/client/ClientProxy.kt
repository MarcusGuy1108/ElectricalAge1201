package mods.eln.client

import net.neoforged.neoforge.common.NeoForge

/**
 * Client-only state that 1.7.10 kept on its ClientProxy. Only touched from client code.
 */
object ClientProxy {
    @JvmField
    val uuidManager = UuidManager().also { manager -> NeoForge.EVENT_BUS.addListener(manager::tick) }
}
