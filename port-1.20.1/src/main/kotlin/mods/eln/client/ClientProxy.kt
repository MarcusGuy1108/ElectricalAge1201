package mods.eln.client

import net.minecraftforge.common.MinecraftForge

/**
 * Client-only state that 1.7.10 kept on its ClientProxy. Only touched from client code.
 */
object ClientProxy {
    @JvmField
    val uuidManager = UuidManager().also { manager -> MinecraftForge.EVENT_BUS.addListener(manager::tick) }
}
