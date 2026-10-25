package mods.eln

import net.minecraftforge.fml.common.Mod

@Mod(Eln.MODID)
object ElnMod {
    init {
        Eln.loadConfig()
        Eln.logger.info("Electrical Age loading")
    }
}
