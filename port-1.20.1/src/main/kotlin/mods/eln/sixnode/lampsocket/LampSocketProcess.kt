package mods.eln.sixnode.lampsocket

import net.minecraft.server.level.ServerLevel
import mods.eln.compat.*
import mods.eln.item.lampitem.BoilerplateLampData
import mods.eln.item.lampitem.LampDescriptor
import mods.eln.lightblock.LightBlockEntity
import mods.eln.misc.Coordinate
import mods.eln.misc.Utils
import mods.eln.sim.IProcess
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import kotlin.math.abs

class LampSocketProcess(var element: LampSocketElement) : IProcess {

    private var processElapsedTime = 0.0

    var stableLightProbability = 0.0
    var fastLightValue = 0

    private var lampInInventory = false
    private var cableInInventory = false

    override fun process(time: Double) {
        val lampStack = element.inventory.getStackInSlot(LampSocketContainer.LAMP_SLOT_ID)
        val cableStack = element.inventory.getStackInSlot(LampSocketContainer.CABLE_SLOT_ID)

        var activeLampSupplyConnection = false
        var newLightValue = BoilerplateLampData.MIN_LIGHT_VALUE

        if (lampStack != null && cableStack != null) {
            val lampDescriptor = Utils.getItemObject(lampStack) as LampDescriptor

            if (element.poweredByLampSupply) {
                // PORT: lamp supply channels return with mods.eln.sixnode.lampsupply; until then such a socket stays dark.
                element.electricalLoad.state = 0.0
            }

            val lampData = lampDescriptor.lampData
            val lampVoltage = abs(element.lampResistor.voltage)

            if (lampVoltage > (lampData.nominalU * lampData.technology.minimalUFactor)) {
                val num: Double = lampVoltage - (lampData.nominalU * lampData.technology.minimalUFactor)
                val den: Double = lampData.nominalU - (lampData.nominalU * lampData.technology.minimalUFactor)

                newLightValue = ((num / den) * lampData.nominalLightValue).toInt()

                // This code makes the fluorescent lights blink, and the other lights are just "stable"
                if (lampData.technology.lampType == "fluorescent") {
                    if (newLightValue >= LampSocketRender.MIN_LIGHT_ON_VALUE && stableLightProbability <= 1.0) {
                        stableLightProbability += (lampVoltage / lampData.nominalU) * (time / lampData.technology.timeUntilStableInSeconds)
                        if (stableLightProbability < Math.random()) newLightValue = BoilerplateLampData.MIN_LIGHT_VALUE
                        if (stableLightProbability > 1.0) stableLightProbability = 1.0
                    } else {
                        newLightValue = BoilerplateLampData.MIN_LIGHT_VALUE
                        stableLightProbability = 0.0
                    }
                } else {
                    stableLightProbability = 1.0
                }

                if (newLightValue < BoilerplateLampData.MIN_LIGHT_VALUE) newLightValue = BoilerplateLampData.MIN_LIGHT_VALUE
                else if (newLightValue > BoilerplateLampData.MAX_LIGHT_VALUE) newLightValue = BoilerplateLampData.MAX_LIGHT_VALUE
            } else {
                stableLightProbability = 0.0
            }

            if (element.coordinate!!.blockExist) {
                updateNearbyBlocks(lampData.technology.cropGrowthRateFactor, lampData.nominalLightValue, newLightValue, time)
            }

            /* Only decrease the life of a bulb once a second. This reduces the update rate at which the NBT is changed
             * to once per second from once per tick, reducing the probability of an NBT mismatch bug occurring when
             * shift-clicking. When the bug is eventually fixed, the processElapsedTime variable and supporting code can
             * be deleted. Also update the decreaseLampLife function definition according to the note there.
             */
            if (processElapsedTime in -0.001..0.001) {
                val lampLife = lampDescriptor.decreaseLampLife(lampStack, lampVoltage)

                if (lampLife <= 0.0) {
                    newLightValue = BoilerplateLampData.MIN_LIGHT_VALUE
                    element.inventory.setInventorySlotContents(LampSocketContainer.LAMP_SLOT_ID, null)
                    element.inventory.setChanged()
                }
            }
        } else {
            stableLightProbability = 0.0
        }

        // Only run raytracing when the lamp socket is actually on.
        if (newLightValue > BoilerplateLampData.MIN_LIGHT_VALUE) placeSpot(newLightValue)

        updateFastLight(newLightValue)
        updateInventoryAndPublish(lampStack, cableStack, activeLampSupplyConnection, newLightValue)

        processElapsedTime += time
        if (processElapsedTime >= 1.0) processElapsedTime = 0.0
    }

    private fun updateNearbyBlocks(growRate: Double, nominalLight: Int, actualLight: Int, deltaT: Double) {
        val randTarget = growRate * deltaT * (actualLight.toDouble() / nominalLight.toDouble())

        if (randTarget > Math.random()) {
            var rotationVector = Vec3(1.0, 0.0, 0.0)
                .zRot((element.projectionRotationAngle * (Math.PI / 180.0)).toFloat())
                .yRot(((Math.random() - 0.5) * Math.PI / 2.0).toFloat())
                .zRot(((Math.random() - 0.5) * Math.PI / 2.0).toFloat())
            rotationVector = element.front.rotatedOnXnLeft(rotationVector)
            rotationVector = element.side.rotatedFromXN(rotationVector)

            val lbCoordinate = raytrace(rotationVector, actualLight)
            // 1.7.10 forced a block update tick; a random tick is what makes crops grow in 1.20.1.
            val level = lbCoordinate.world() as? ServerLevel ?: return
            val state = level.getBlockState(lbCoordinate.blockPos)
            if (state.isRandomlyTicking) state.randomTick(level, lbCoordinate.blockPos, level.random)
        }
    }

    private fun placeSpot(lightValue: Int) {
        var rotationVector = Vec3(1.0, 0.0, 0.0).zRot((element.projectionRotationAngle * (Math.PI / 180.0)).toFloat())
        rotationVector = element.front.rotatedOnXnLeft(rotationVector)
        rotationVector = element.side.rotatedFromXN(rotationVector)

        val lbCoordinate = raytrace(rotationVector, 0)

        // This makes the projected light "flicker" when a fluorescent bulb is turning on. It's not quite in sync with
        // the bulb, but it's the best that can be done without rewriting the light block handler to allow updating the
        // light value of an existing light block.
        val lightTimeout = if (stableLightProbability <= 1.0) 1 else 5

        LightBlockEntity.addLight(lbCoordinate, lightValue, lightTimeout)
    }

    private fun raytrace(rotationVector: Vec3, vectorLengthModifier: Int): Coordinate {
        var lightVector = element.sixNode!!.coordinate.toVec3()
        val lbCoordinate = Coordinate(lightVector, element.sixNode!!.coordinate.dimension)

        for (idx in 0 until element.descriptor.range + vectorLengthModifier) {
            lightVector = lightVector.add(rotationVector)
            lbCoordinate.setPosition(lightVector)

            if (!lbCoordinate.blockExist || lbCoordinate.world().getBlockState(lbCoordinate.blockPos).isSolidRender(lbCoordinate.world(), lbCoordinate.blockPos)) {
                lightVector = lightVector.subtract(rotationVector)
                lbCoordinate.setPosition(lightVector)
                break
            }
        }

        return lbCoordinate
    }

    /**
     * Sync "fast" light changes (fluorescent flicker)
     */
    private fun updateFastLight(newLightValue: Int) {
        if (fastLightValue != newLightValue) {
            fastLightValue = newLightValue

            val bos = ByteArrayOutputStream(64)
            val packet = DataOutputStream(bos)

            element.preparePacketForClient(packet)

            try {
                packet.writeInt(newLightValue)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            element.sendPacketToAllClient(bos)
        }
    }

    /**
     * Manually update the server-side inventory every time an item is inserted/removed from the GUI.
     * This should be happening automatically, but it is not.
     */
    private fun updateInventoryAndPublish(lampStack: ItemStack?, cableStack: ItemStack?, activeLampSupplyConnection: Boolean, newLightValue: Int) {
        var inventoryChanged = false
        var publishChanges = false

        if (lampInInventory != (lampStack != null)) {
            lampInInventory = (lampStack != null)
            inventoryChanged = true
        }

        if (cableInInventory != (cableStack != null)) {
            cableInInventory = (cableStack != null)
            inventoryChanged = true
        }

        if (element.activeLampSupplyConnection != activeLampSupplyConnection) {
            element.activeLampSupplyConnection = activeLampSupplyConnection
            publishChanges = true
        }

        if (element.sixNode!!.lightValue != newLightValue) {
            element.sixNode!!.lightValue = newLightValue
            publishChanges = true
        }

        // Prevent duplicate calls of these functions
        if (inventoryChanged) element.inventoryChange(element.inventory)
        else if (publishChanges) element.needPublish()
    }

}