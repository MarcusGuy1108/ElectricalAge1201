package mods.eln.node.transparent

/**
 * Used to differentiate between subclasses of TransparentNodeEntity, so that
 * our TEs can implement different interfaces depending on what functionality
 * they have.
 */
// PORT: 1.20.1 has one transparent node block entity type; Fluid gets its own once fluid capabilities are ported.
enum class EntityMetaTag(val meta: Int) {
    Fluid(1),
    Basic(3); // 3, because this is the default value used in pre-metatag worlds
}
