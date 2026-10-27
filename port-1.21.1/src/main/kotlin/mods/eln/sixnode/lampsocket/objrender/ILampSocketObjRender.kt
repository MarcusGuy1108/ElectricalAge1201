package mods.eln.sixnode.lampsocket.objrender

import mods.eln.sixnode.lampsocket.LampSocketRender

interface ILampSocketObjRender {

    // PORT: the in-hand item draw(descriptor, ItemRenderType, distance) returns if 3D item models come back.

    fun draw(render: LampSocketRender, distanceToPlayer: Double)

}