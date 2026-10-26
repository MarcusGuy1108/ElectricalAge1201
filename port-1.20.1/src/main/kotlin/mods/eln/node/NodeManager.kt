package mods.eln.node

import mods.eln.misc.Coordinate
import mods.eln.misc.Utils.getTags
import mods.eln.misc.Utils.println
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import java.util.*

class NodeManager(@Suppress("UNUSED_PARAMETER") par1Str: String?) {
    val nodeArray: HashMap<Coordinate, NodeBase>
    val nodes: ArrayList<NodeBase>
    val nodeList: Collection<NodeBase>
        get() = nodeArray.values

    fun addNode(node: NodeBase) {
        // nodeArray.add(node);
        val old = nodeArray.put(node.coordinate, node)
        if (old != null) {
            nodes.remove(old)
        }
        nodes.add(node)
        // println("NodeManager has " + nodeArray.size + "node")
        // nodeArray.put(new NodeIdentifier(node), node);
    }

    fun removeNode(node: NodeBase?) {
        if (node == null) return
        nodeArray.remove(node.coordinate)
        nodes.remove(node)
        // println("NodeManager has " + nodeArray.size + "node")
    }

    fun removeCoordonate(c: Coordinate?) {
        // nodeArray.remove(node);
        val n = nodeArray.remove(c)
        if (n != null) nodes.remove(n)
        // println("NodeManager has " + nodeArray.size + "node")
    }

    fun getNodeFromCoordonate(nodeCoordinate: Coordinate?): NodeBase? {
        return nodeArray[nodeCoordinate]
    }

    // PORT: getTransparentNodeFromCoordinate returns with mods.eln.node.transparent.

    var rand = Random()
    val randomNode: NodeBase?
        get() = if (nodes.isEmpty()) null else nodes[rand.nextInt(nodes.size)]

    // Saved nodes that failed to load, per dimension. 1.7.10 dropped them, so the next save erased them for good; they
    // are written back unchanged instead, so a bug or a not yet ported node type never destroys a world's machines.
    private val unloadableTags = HashMap<ResourceKey<Level>, MutableList<CompoundTag>>()

    fun loadFromNbt(nbt: CompoundTag?, dim: ResourceKey<Level>) {
        val addedNode: MutableList<NodeBase> = ArrayList()
        for (o in getTags(nbt!!)) {
            val tag = o
            val nodeClass = UUIDToClass[tag.getString("tag")]
            var node: NodeBase? = null
            try {
                node = nodeClass!!.getConstructor().newInstance() as NodeBase
                node.readFromNBT(tag)
                addNode(node)
                node.initializeFromNBT()
                addedNode.add(node)
            } catch (e: Exception) {
                e.printStackTrace()
                if (node != null && nodeArray[node.coordinate] === node) removeNode(node)
                println("Electrical Age could not load a saved node (type '${tag.getString("tag")}'); it is kept in the save.")
                unloadableTags.getOrPut(dim) { ArrayList() }.add(tag.copy())
            }
        }
        for (n in addedNode) {
            n.globalBoot()
        }
    }

    fun saveToNbt(nbt: CompoundTag, dim: ResourceKey<Level>?) {
        var nodeCounter = 0
        val nodesCopy: MutableList<NodeBase> = ArrayList()
        nodesCopy.addAll(nodes)
        for (node in nodesCopy) {
            try {
                if (node.mustBeSaved() == false) continue
                if (dim != null && node.coordinate.dimension != dim) continue
                val nbtNode = CompoundTag()
                nbtNode.putString("tag", node.nodeUuid)
                node.writeToNBT(nbtNode)
                nbt.put("n" + nodeCounter++, nbtNode)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        for (tag in unloadableTags[dim].orEmpty()) {
            // A node placed since then at the same spot replaces the unloadable one.
            if (nodeArray.containsKey(Coordinate(if (tag.contains("node")) tag.getCompound("node") else tag, "c"))) continue
            nbt.put("n" + nodeCounter++, tag)
        }
    }

    fun clear() {
        nodes.clear()
        nodeArray.clear()
        unloadableTags.clear()
    }

    fun unload(dimensionId: ResourceKey<Level>) {
        unloadableTags.remove(dimensionId)
        val i = nodes.iterator()
        while (i.hasNext()) {
            val n = i.next()
            if (n.coordinate.dimension == dimensionId) {
                n.unload()
                i.remove()
                nodeArray.remove(n.coordinate)
            }
        }
    }

    companion object {
        @JvmField
        var instance: NodeManager? = null
        val UUIDToClass = HashMap<String, Class<*>>()
        @JvmStatic
        fun registerUuid(uuid: String, classType: Class<*>) {
            UUIDToClass[uuid] = classType
        }
    }

    // private ArrayList<Node> nodeArray = new ArrayList<Node>();
    init {
        nodeArray = HashMap()
        nodes = ArrayList()
        instance = this
    }
}
