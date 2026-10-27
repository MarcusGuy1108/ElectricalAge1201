package mods.eln.sim;

import mods.eln.Eln;
import mods.eln.misc.Coordinate;
import mods.eln.misc.Utils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;

import java.util.List;

public class MonsterPopFreeProcess implements IProcess {

    private final Coordinate coordinate;
    private final int range;

    double timerCounter = 0;
    final double timerPeriod = 0.212;

    List<Monster> oldList = null;

    public MonsterPopFreeProcess(Coordinate coordinate, int range) {
        this.coordinate = coordinate;
        this.range = range;
    }

    @Override
    public void process(double time) {
        //Monster killing must be active before continuing :
        if (!Eln.config.getBooleanOrElse("entities.mobSpawning.preventNearLamps", true))
            return;

        timerCounter += time;
        if (timerCounter > timerPeriod) {
            timerCounter -= Utils.rand(1, 1.5) * timerPeriod;
            List<Monster> list = coordinate.world().getEntitiesOfClass(Monster.class, coordinate.getAxisAlignedBB(range + 8));

            for (Monster mob : list) {
                if (oldList == null || !oldList.contains(mob)) {
                    if (coordinate.distanceTo(mob) < range) {
                        // PORT: the Eln replicator is spared too once mods.eln.entity is ported.
                        if (!(mob instanceof WitherBoss) && !(mob instanceof EnderMan)) {
                            mob.remove(Entity.RemovalReason.DISCARDED);
                            Utils.println("MonsterPopFreeProcess : Dead");
                        }
                    }
                }
            }
            oldList = list;
        }
    }

}
