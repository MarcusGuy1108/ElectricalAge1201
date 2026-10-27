package mods.eln.item.electricalitem

import mods.eln.Eln
import mods.eln.client.GL11
import mods.eln.generic.GenericItemUsingDamageDescriptor
import mods.eln.i18n.I18N.tr
import mods.eln.item.electricalinterface.IItemEnergyBattery
import mods.eln.lightblock.LightBlock
import mods.eln.misc.Obj3D
import mods.eln.misc.Obj3D.Obj3DPart
import mods.eln.misc.Utils
import mods.eln.misc.UtilsClient
import mods.eln.ore.OreScanner
import mods.eln.generic.ICustomItemRender
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

/**
 * The X-ray scanner: a portable battery item whose screen shows, while running, what lies behind the blocks in front
 * of its holder, ores in green. 1.7.10 drew it with an IItemRenderer; here [ElnItemRenderer] calls [renderInHand].
 */
class PortableOreScannerItem(
    name: String?, private val obj: Obj3D,
    var energyStorage: Double, var chargePower: Double, var dischargePower: Double,
    var viewRange: Float, var viewYAlpha: Float, var resWidth: Int, var resHeight: Int
) : GenericItemUsingDamageDescriptor(name!!), IItemEnergyBattery, ICustomItemRender {

    var base: Obj3DPart = obj.getPart("Base")!!
    var led: Obj3DPart = obj.getPart("Led")!!
    var ledHalo: Obj3DPart = obj.getPart("LedHalo")!!
    var textBat: Array<Obj3DPart> = (0..3).map { obj.getPart("TextBat$it")!! }.toTypedArray()
    var textRun: Obj3DPart = obj.getPart("TextRun")!!
    var textInit: Obj3DPart = obj.getPart("TextInit")!!
    var buttons: Obj3DPart = obj.getPart("Buttons")!!
    var screenDamage: Array<Obj3DPart> = (0..2).map { obj.getPart("ScreenDamageL" + (it + 1))!! }.toTypedArray()
    var screenLuma: Obj3DPart = obj.getPart("ScreenLuma")!!
    private val damagePerBreakLevel: Byte = 3

    override fun onUpdate(stack: ItemStack, world: Level, entity: Entity, par4: Int, par5: Boolean) {
        if (world.isClientSide) return
        if (entity !is ServerPlayer) return
        val state = getState(stack)
        var counter = getCounter(stack)

        if (getDamage(stack) / damagePerBreakLevel >= 4) {
            if (state != State.Idle)
                setState(stack, State.Idle)
            return
        }

        when (state) {
            State.Boot -> if ((--counter).toInt() != 0) {
                setCounter(stack, counter)
            } else {
                setState(stack, State.Run)
            }
            State.Stop -> if ((--counter).toInt() != 0) {
                setCounter(stack, counter)
            } else {
                setState(stack, State.Idle)
            }
            else -> {}
        }
    }

    override fun onItemRightClick(s: ItemStack, w: Level, p: Player): ItemStack {
        if (w.isClientSide) return s
        val energy = getEnergy(s)
        when (getState(s)) {
            State.Idle -> if (energy > dischargePower) {
                setState(s, State.Boot)
                setCounter(s, bootTime)
            }
            State.Run -> {
                setState(s, State.Stop)
                setCounter(s, stopTime)
            }
            else -> {}
        }
        return s
    }

    // PORT: setParent registers with the in-game wiki (Data.addPortable) once mods.eln.wiki is ported.

    override fun getDefaultNBT(): CompoundTag? {
        val nbt = CompoundTag()
        nbt.putDouble("e", energyStorage * 0.2)
        nbt.putByte("s", State.Boot.serialized)
        nbt.putShort("c", bootTime)
        nbt.putByte("d", 0.toByte())
        return nbt
    }

    override fun addInformation(itemStack: ItemStack?, entityPlayer: Player?, list: MutableList<String>, par4: Boolean) {
        super.addInformation(itemStack, entityPlayer, list, par4)
        list.add(tr("Discharge power: %1\$W", Utils.plotValue(dischargePower)))
        if (itemStack != null) {
            list.add(tr("Stored energy: %1\$J (%2$%)", Utils.plotValue(getEnergy(itemStack)),
                (getEnergy(itemStack) / energyStorage * 100).toInt()))
        }
    }

    override fun getEnergy(stack: ItemStack): Double = getNbt(stack).getDouble("e")

    override fun setEnergy(stack: ItemStack, value: Double) {
        getNbt(stack).putDouble("e", value)
    }

    private fun getState(stack: ItemStack): State = State.from(getNbt(stack).getByte("s")) ?: State.Idle

    private fun setState(stack: ItemStack, value: State) {
        getNbt(stack).putByte("s", value.serialized)
    }

    fun getCounter(stack: ItemStack): Short = getNbt(stack).getShort("c")

    fun setCounter(stack: ItemStack, value: Short) {
        getNbt(stack).putShort("c", value)
    }

    fun getDamage(stack: ItemStack): Byte = getNbt(stack).getByte("d")

    fun setDamage(stack: ItemStack, value: Byte) {
        getNbt(stack).putByte("d", value)
    }

    override fun onDroppedByPlayer(item: ItemStack, player: Player?): Boolean {
        setState(item, State.Idle)
        return super.onDroppedByPlayer(item, player)
    }

    override fun getEnergyMax(stack: ItemStack): Double = energyStorage

    override fun getChargePower(stack: ItemStack): Double = chargePower

    override fun getDischagePower(stack: ItemStack): Double = dischargePower

    override fun getPriority(stack: ItemStack): Int = 0

    // Mining with the scanner in hand cracks its screen.
    override fun onBlockStartBreak(itemstack: ItemStack?, x: Int, y: Int, z: Int, player: Player?): Boolean {
        if (itemstack != null && player != null && !player.level().isClientSide) {
            setDamage(itemstack, (getDamage(itemstack) + 1).toByte())
        }
        return super.onBlockStartBreak(itemstack, x, y, z, player)
    }

    // 1.7.10 renderItem minus the inventory icon, which the item model draws.
    override fun renderInHand(stack: ItemStack, context: ItemDisplayContext) {
        val energy = getEnergy(stack)
        val state = getState(stack)
        GL11.glPushMatrix()
        // Item space runs 0..1 around the hand or ground spot; the model lies along +X with its screen facing +Y.
        GL11.glTranslatef(0.5f, 0.5f, 0.5f)
        val e: Entity? = when (context) {
            ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND -> {
                // Held out in front, far end raised so the screen faces the player.
                val side = if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) 1f else -1f
                GL11.glTranslatef(0.2f * side, 0.15f, 0f)
                GL11.glRotatef(40f, 1f, 0f, 0f)
                GL11.glRotatef(90f, 0f, 1f, 0f)
                GL11.glScalef(0.6f, 0.6f, 0.6f)
                GL11.glTranslatef(-0.5f, 0f, 0f)
                Minecraft.getInstance().player
            }
            ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND -> {
                GL11.glTranslatef(0f, -0.1f, 0f)
                GL11.glRotatef(90f, 0f, 1f, 0f)
                GL11.glScalef(0.5f, 0.5f, 0.5f)
                GL11.glTranslatef(-0.5f, 0f, 0f)
                // shortcut: only the local player's own scan is computed, so other holders show a dark screen.
                null
            }
            else -> {
                GL11.glScalef(0.6f, 0.6f, 0.6f)
                GL11.glTranslatef(-0.5f, 0f, 0f)
                null
            }
        }
        val drawScreen = e != null && UtilsClient.clientDistanceTo(e) < 10
        val drawRay = drawScreen && state == State.Run
        base.draw()
        if (drawRay) {
            GL11.glPushMatrix()
            val render = renderStorage.takeIf { it?.matches(this) == true }
                ?: RenderStorage(viewRange, viewYAlpha, resWidth, resHeight).also { renderStorage = it }
            render.generate(e!!.level(), e.x, e.eyeY, e.z, e.yRot * Math.PI.toFloat() / 180.0f, e.xRot * Math.PI.toFloat() / 180.0f)
            val scale = 1f / resWidth * 0.50f
            GL11.glTranslatef(0.90668f, 0.163f, -0.25078f)
            GL11.glRotatef(270f, 1f, 0f, 0f)
            GL11.glRotatef(270f, 0f, 0f, 1f)
            GL11.glScalef(scale, -scale, 1f)
            render.draw()
            GL11.glPopMatrix()
            var r = 0f
            var g = 0f
            var b = 0f
            var count = 0
            var y = 0
            while (y < resHeight) {
                var x = 0
                while (x < resHeight) {
                    r += render.screenRed[y][x]
                    g += render.screenGreen[y][x]
                    b += render.screenBlue[y][x]
                    count++
                    x += 6
                }
                y += 6
            }
            UtilsClient.drawHalo(screenLuma, r / count, g / count, b / count, e, false)
        }
        if (drawScreen) {
            if (state == State.Idle) {
                GL11.glColor4f(0.5f, 0.5f, 0.5f, 1f)
                led.draw()
                GL11.glColor4f(1f, 1f, 1f, 1f)
                buttons.draw()
            }
            UtilsClient.disableLight()
            if (state != State.Idle) {
                GL11.glColor4f(1f, 1f, 1f, 1f)
                buttons.draw()
                val (r, g, b) = when (state) {
                    State.Boot -> Triple(0.9f, 0.4f, 0f)
                    State.Run -> Triple(0f, 1f, 0f)
                    State.Stop -> Triple(1f, 0f, 0f)
                    else -> Triple(0f, 0f, 0f)
                }
                GL11.glColor4f(r * 0.6f, g * 0.6f, b * 0.6f, 1f)
                led.draw()
                UtilsClient.enableBlend()
                UtilsClient.drawHaloNoLightSetup(ledHalo, r, g, b, e!!, false)
            }
            GL11.glColor4f(1f, 1f, 1f, 0.4f)
            when (state) {
                State.Boot -> textInit.draw()
                State.Run -> {
                    textRun.draw()
                    val batLevel = Math.min(textBat.size - 1, (energy / energyStorage * textBat.size + 0.5f).toInt())
                    textBat[batLevel].draw()
                }
                else -> {}
            }
            UtilsClient.enableBlend()
            GL11.glColor4f(1f, 1f, 1f, 1f)
            var breakLevel = getDamage(stack) / damagePerBreakLevel
            if (state == State.Idle) breakLevel = Math.min(breakLevel, screenDamage.size - 1)
            for (idx in 0 until breakLevel) {
                if (idx == screenDamage.size) break
                screenDamage[Math.min(screenDamage.size - 1, breakLevel - 1) - idx].draw()
            }
            UtilsClient.disableBlend()
            UtilsClient.enableLight()
        }
        GL11.glPopMatrix()
    }

    class RenderStorage(private val viewRange: Float, viewYAlpha: Float, private val resWidth: Int, private val resHeight: Int) {
        private val camDist = (resWidth / 2 / Math.tan((viewYAlpha / 2).toDouble())).toFloat()
        val screenRed = Array(resHeight) { FloatArray(resWidth) }
        val screenBlue = Array(resHeight) { FloatArray(resWidth) }
        val screenGreen = Array(resHeight) { FloatArray(resWidth) }
        private val worldBlocksDim = (viewRange * 2 + 3).toInt()
        private val worldBlocksDim2 = worldBlocksDim / 2
        // The block states the rays have already read this frame, around the holder's head.
        private val worldBlocks = arrayOfNulls<BlockState>(worldBlocksDim * worldBlocksDim * worldBlocksDim)

        fun matches(item: PortableOreScannerItem) =
            viewRange == item.viewRange && resWidth == item.resWidth && resHeight == item.resHeight

        fun generate(w: Level, posX: Double, posY: Double, posZ: Double, alphaY: Float, alphaX: Float) {
            val posXint = Math.round(posX).toInt()
            val posYint = Math.round(posY).toInt()
            val posZint = Math.round(posZ).toInt()
            worldBlocks.fill(null)
            val pos = BlockPos.MutableBlockPos()
            for (screenY in 0 until resHeight) {
                for (screenX in 0 until resWidth) {
                    var x = (posX - posXint).toFloat()
                    var y = (posY - posYint).toFloat()
                    var z = (posZ - posZint).toFloat()
                    var vx = -(screenX - resWidth / 2).toFloat()
                    var vy = -(screenY - resHeight / 2).toFloat()
                    var vz = camDist
                    run {
                        val sin = Mth.sin(alphaX)
                        val cos = Mth.cos(alphaX)
                        val temp = vy
                        vy = vy * cos - vz * sin
                        vz = vz * cos + temp * sin
                    }
                    run {
                        val sin = Mth.sin(alphaY)
                        val cos = Mth.cos(alphaY)
                        val temp = vx
                        vx = vx * cos - vz * sin
                        vz = vz * cos + temp * sin
                    }
                    val normInv = 1f / Math.sqrt((vx * vx + vy * vy + vz * vz).toDouble()).toFloat()
                    vx *= normInv
                    vy *= normInv
                    vz *= normInv
                    if (vx == 0f) vx += 0.0001f
                    if (vy == 0f) vy += 0.0001f
                    if (vz == 0f) vz += 0.0001f
                    val vxInv = 1f / vx
                    val vyInv = 1f / vy
                    val vzInv = 1f / vz
                    var stackRed = 0f
                    var stackBlue = 0f
                    var stackGreen = 0f
                    var d = 0f
                    while (d < viewRange) {
                        val xFloor = Mth.floor(x)
                        val yFloor = Mth.floor(y)
                        val zFloor = Mth.floor(z)
                        var dx = x - xFloor
                        var dy = y - yFloor
                        var dz = z - zFloor
                        dx = if (vx > 0) (1 - dx) * vxInv else -dx * vxInv
                        dy = if (vy > 0) (1 - dy) * vyInv else -dy * vyInv
                        dz = if (vz > 0) (1 - dz) * vzInv else -dz * vzInv
                        val dBest = Math.min(Math.min(dx, dy), dz) + 0.01f
                        val index = ((xFloor + worldBlocksDim2) * worldBlocksDim + yFloor + worldBlocksDim2) * worldBlocksDim + zFloor + worldBlocksDim2
                        val state = worldBlocks[index]
                            ?: w.getBlockState(pos.set(posXint + xFloor, posYint + yFloor, posZint + zFloor)).also { worldBlocks[index] = it }
                        val dToStack = if (d + dBest < viewRange) dBest else viewRange - d
                        stackGreen += OreScanner.factor(state) * dToStack
                        if (!state.isAir && !state.`is`(LightBlock.BLOCK.get())) {
                            stackRed += if (state.canOcclude()) 0.2f * dToStack else 0.1f * dToStack
                        } else stackBlue += 0.06f * dToStack
                        x += vx * dBest
                        y += vy * dBest
                        z += vz * dBest
                        d += dBest
                    }
                    screenRed[screenY][screenX] = stackRed
                    screenGreen[screenY][screenX] = stackGreen
                    screenBlue[screenY][screenX] = stackBlue
                }
            }
        }

        private fun noiseRand(): Float = (Math.random().toFloat() - 0.5f) * 0.03f

        fun draw() {
            UtilsClient.disableLight()
            UtilsClient.disableTexture()
            for (screenY in 0 until resHeight) {
                GL11.glBegin(GL11.GL_QUAD_STRIP)
                for (screenX in 0 until resWidth + 1) {
                    if (screenX != resWidth) GL11.glColor3f(screenRed[screenY][screenX] + noiseRand(), screenGreen[screenY][screenX] + noiseRand(), screenBlue[screenY][screenX] + noiseRand())
                    GL11.glVertex3f(screenX.toFloat(), screenY.toFloat(), 0f)
                    GL11.glVertex3f(screenX.toFloat(), (screenY + 1).toFloat(), 0f)
                }
                GL11.glEnd()
            }
            UtilsClient.enableTexture()
            UtilsClient.enableLight()
            GL11.glColor3f(1f, 1f, 1f)
        }
    }

    override fun electricalItemUpdate(stack: ItemStack, time: Double) {
        if (getState(stack) == State.Idle) return
        val energy = getEnergy(stack) - dischargePower * time
        if (energy <= 0) {
            setState(stack, State.Idle)
            setEnergy(stack, 0.0)
            return
        }
        setEnergy(stack, energy)
    }

    companion object {
        // The local player's scan; 1.7.10 kept one per stack in its client live data manager.
        private var renderStorage: RenderStorage? = null
    }
}

private enum class State(val serialized: Byte) {
    Idle(0),
    Boot(1),
    Run(2),
    Stop(3),
    Error(4);

    companion object {
        private val map = values().associateBy(State::serialized)
        fun from(type: Byte) = map[type]
    }
}

private const val bootTime: Short = (4 * 20).toShort()
private const val stopTime: Short = (1 * 20).toShort()
