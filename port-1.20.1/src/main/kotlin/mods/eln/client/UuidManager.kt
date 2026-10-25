package mods.eln.client

import net.minecraftforge.event.TickEvent

class UuidManager {
    internal val entities = HashMap<Int, IUuidEntity>()
    internal val uuids = HashMap<IUuidEntity, ArrayList<Int>>()

    fun add(uuid: ArrayList<Int>, e: IUuidEntity) {
        uuid.forEach {
            entities[it] = e
            uuids.getOrPut(e) { ArrayList() }.add(it)
        }
    }

    // Registered on the Forge event bus by ClientProxy.
    fun tick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.END) return
        val i = entities.iterator()
        while (i.hasNext()) {
            val p = i.next()
            if (!p.value.isAlive()) {
                uuids.remove(p.value)
                i.remove()
            }
        }
    }

    fun kill(uuid: Int) {
        entities.remove(uuid)?.apply {
            kill()
            uuids.remove(this)?.forEach { entities.remove(it) }
        }
    }
}
