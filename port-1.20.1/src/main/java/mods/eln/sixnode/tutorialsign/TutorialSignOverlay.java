package mods.eln.sixnode.tutorialsign;

import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.client.event.RenderGuiEvent;

// Shows the text of the tutorial sign the player stands next to in the top left corner.
public class TutorialSignOverlay {

    TutorialSignRender oldRender = null;

    public void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (oldRender != null) {
            oldRender.lightInterpol.setTarget(0);
            oldRender = null;
        }

        int px = Mth.floor(player.getX()), py = Mth.floor(player.getY()), pz = Mth.floor(player.getZ());
        int r = 1;
        Level w = player.level();

        TutorialSignRender best = null;
        double bestDistance = 10000;

        for (int x = px - r; x <= px + r; x++) {
            for (int y = py - r; y <= py + r; y++) {
                for (int z = pz - r; z <= pz + r; z++) {
                    BlockEntity e = w.getBlockEntity(new BlockPos(x, y, z));
                    if (e instanceof SixNodeEntity) {
                        SixNodeEntity sne = (SixNodeEntity) e;
                        for (SixNodeElementRender render : sne.elementRenderList) {
                            if (render instanceof TutorialSignRender) {
                                double d = Utils.getLength(player.getX(), player.getY(), player.getZ(), x + 0.5, y + 0.5, z + 0.5);
                                if (d < bestDistance) {
                                    bestDistance = d;
                                    best = (TutorialSignRender) render;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (best != null) {
            oldRender = best;
            oldRender.lightInterpol.setTarget(1f);
            GuiGraphics graphics = event.getGuiGraphics();
            graphics.pose().pushPose();
            graphics.pose().scale(0.5f, 0.5f, 0.5f);
            int y = 0;
            for (String str : best.texts) {
                graphics.drawString(mc.font, str, 10, 10 + y, 0xFFFFFF);
                y += 10;
            }
            graphics.pose().popPose();
        }
    }
}
