package mods.eln.item

import mods.eln.generic.GenericItemUsingDamageDescriptor

// PORT: setParent registers the item with the in-game wiki (Data.addUpgrade) once mods.eln.wiki is ported.
open class GenericItemUsingDamageDescriptorUpgrade : GenericItemUsingDamageDescriptor {
    constructor(name: String?) : super(name!!) {}
    constructor(name: String?, iconName: String?) : super(name!!, iconName!!) {}
}
