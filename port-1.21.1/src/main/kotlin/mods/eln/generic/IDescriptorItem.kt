package mods.eln.generic

/**
 * 1.20.1 items are one per descriptor; this replaces looking the descriptor up from the 1.7.10 item damage value.
 */
interface IDescriptorItem {
    val descriptor: GenericItemBlockUsingDamageDescriptor
}
