package mods.eln.railroad

import net.minecraft.resources.ResourceLocation
import mods.eln.Eln
import mods.eln.misc.Coordinate
import mods.eln.node.NodeManager
import mods.eln.node.transparent.TransparentNode
import mods.eln.registry.ElnRegistries
import mods.eln.sim.mna.misc.MnaConst
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.vehicle.Minecart
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import mods.eln.registry.RegistryObject
import kotlin.math.abs
import kotlin.math.sign

class EntityElectricMinecart(type: EntityType<*>, level: Level) : Minecart(type, level) {

    constructor(level: Level, x: Double, y: Double, z: Double) : this(TYPE.get(), level) {
        setPos(x, y, z)
        xo = x
        yo = y
        zo = z
    }

    private var lastPowerElement: RailroadPowerInterface? = null
    private val locomotiveMaximumResistance = 200.0
    var energyBufferTargetJoules = 10_000.0
    var energyBufferJoules = 0.0

    override fun tick() {
        super.tick()
        if (level().isClientSide) return
        val cartCoordinate = Coordinate(blockPosition().x, blockPosition().y, blockPosition().z, level())
        val overheadWires = getOverheadWires(cartCoordinate)

        when (val oldPowerElement = lastPowerElement) {
            overheadWires, null -> {
                // references the current overhead wires, or nothing
            }
            else -> {
                // references old overhead wires that are not the current ones
                energyBufferJoules += PoweredMinecartSimulationSingleton.cartCollectEnergy(this)
                oldPowerElement.deregisterCart(this)
            }
        }

        // PORT: 1.7.10 also looked for under track power one block down; that block is not registered there either.
        val currentElement: RailroadPowerInterface? = overheadWires

        if (currentElement != null) {
            if (currentElement != lastPowerElement) {
                currentElement.registerCart(this)
            }

            if (energyBufferJoules < energyBufferTargetJoules) {
                val chargeRateInv = energyBufferTargetJoules / (abs(energyBufferTargetJoules - energyBufferJoules) * 2)
                PoweredMinecartSimulationSingleton.powerCart(this, chargeRateInv * locomotiveMaximumResistance, 0.1)
            } else {
                PoweredMinecartSimulationSingleton.powerCart(this, MnaConst.highImpedance, 0.1)
            }
            energyBufferJoules += PoweredMinecartSimulationSingleton.cartCollectEnergy(this)
        }

        lastPowerElement = currentElement
    }

    // 1.7.10 passed speed + 1 as the cart's top speed on rails.
    override fun getMaxSpeedWithRail(): Double = super.getMaxSpeedWithRail() + 1

    override fun moveAlongTrack(pos: BlockPos, state: BlockState) {
        super.moveAlongTrack(pos, state)
        if (energyBufferJoules <= 0) return
        val maxEnergy = 40.0
        val energyAvailable = minOf(energyBufferJoules, maxEnergy)
        val startingThreshold = 0.0005
        val motion = deltaMovement
        if ((abs(motion.x) >= startingThreshold || abs(motion.z) >= startingThreshold) && abs(motion.x) < 0.5 && abs(motion.z) < 0.5) {
            val push = 0.05 * (energyAvailable / maxEnergy)
            deltaMovement = motion.add(motion.x.sign * push, 0.0, motion.z.sign * push)
            energyBufferJoules -= energyAvailable
        }
    }

    private fun getOverheadWires(coordinate: Coordinate): OverheadLinesElement? {
        // Pass coordinate of tracks and check vertically the next 3 blocks (4 up looks visually weird)
        val originalY = coordinate.y
        while (coordinate.y <= (originalY + 3)) {
            val node = (NodeManager.instance!!.getNodeFromCoordonate(coordinate) as? TransparentNode)?.element
            if (node is OverheadLinesElement) {
                return node
            }
            coordinate.y++
        }
        return null
    }

    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        tag.putDouble("EnergyBufferJ", energyBufferJoules)
        tag.putDouble("EnergyBufferTargetJ", energyBufferTargetJoules)
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        if (tag.contains("EnergyBufferJ")) {
            energyBufferJoules = tag.getDouble("EnergyBufferJ")
        }
        if (tag.contains("EnergyBufferTargetJ")) {
            energyBufferTargetJoules = tag.getDouble("EnergyBufferTargetJ")
        }
    }

    // 1.7.10 dropped a plain minecart; the port gives the electric one back.
    override fun getDropItem(): Item = Eln.findItemStack("Electric Minecart", 1).item

    override fun getPickResult(): ItemStack = Eln.findItemStack("Electric Minecart", 1)

    // The item's legacy lang key; the entity type has no key of its own.
    override fun getTypeName(): Component = Component.translatable("Electric_Minecart.name")

    override fun getDefaultDisplayBlockState(): BlockState = Blocks.IRON_BLOCK.defaultBlockState()

    companion object {
        @JvmField
        val TYPE: RegistryObject<EntityType<EntityElectricMinecart>> = ElnRegistries.ENTITY_TYPES.register("electric_minecart") { _: ResourceLocation ->
            EntityType.Builder.of(::EntityElectricMinecart, MobCategory.MISC).sized(0.98f, 0.7f).clientTrackingRange(8)
                .build("electric_minecart")
        }
    }
}
