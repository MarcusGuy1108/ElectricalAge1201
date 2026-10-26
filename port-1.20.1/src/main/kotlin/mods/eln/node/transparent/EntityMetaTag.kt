package mods.eln.node.transparent

/**
 * Used to differentiate between subclasses of TransparentNodeEntity, so that
 * our TEs can implement different interfaces depending on what functionality
 * they have.
 */
// 1.20.1 has one transparent node block entity type; elements with a tank expose it through a capability instead.
enum class EntityMetaTag(val meta: Int) {
    Fluid(1),
    Basic(3); // 3, because this is the default value used in pre-metatag worlds
}
