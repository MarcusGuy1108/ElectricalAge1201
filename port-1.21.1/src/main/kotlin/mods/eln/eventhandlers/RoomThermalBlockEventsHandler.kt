package mods.eln.eventhandlers

import mods.eln.environment.RoomThermalManager
import net.minecraft.world.level.Level
import net.neoforged.neoforge.event.level.BlockEvent
import net.neoforged.neoforge.event.level.ExplosionEvent

/** Block changes that may open or close a room make the room thermal system rescan it. */
object RoomThermalBlockEventsHandler {
    fun onBlockBreak(event: BlockEvent.BreakEvent) {
        val level = event.level as? Level ?: return
        RoomThermalManager.onBlockChanged(level, event.pos.x, event.pos.y, event.pos.z)
    }

    // Also covers multi-block placements (doors, beds), whose event extends this one.
    fun onBlockPlace(event: BlockEvent.EntityPlaceEvent) {
        val level = event.level as? Level ?: return
        RoomThermalManager.onBlockChanged(level, event.pos.x, event.pos.y, event.pos.z)
    }

    fun onExplosionDetonate(event: ExplosionEvent.Detonate) {
        if (event.level.isClientSide) return
        for (pos in event.affectedBlocks) RoomThermalManager.onBlockChanged(event.level, pos.x, pos.y, pos.z)
    }
}
