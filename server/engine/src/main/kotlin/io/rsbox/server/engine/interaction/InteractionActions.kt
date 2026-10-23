package io.rsbox.server.engine.interaction

import io.rsbox.server.engine.model.GameObject
import io.rsbox.server.engine.model.entity.Npc
import io.rsbox.server.engine.model.entity.Player

object InteractionActions {

    private val objectActions = mutableMapOf<Pair<Int, Int>, (Player, GameObject) -> Unit>()
    private val npcActions = mutableMapOf<Pair<Int, Int>, (Player, Npc) -> Unit>()

    fun onObject(id: Int, option: Int, action: (Player, GameObject) -> Unit) {
        objectActions[id to option] = action
    }

    fun onNpc(id: Int, option: Int, action: (Player, Npc) -> Unit) {
        npcActions[id to option] = action
    }

    internal fun objectAction(id: Int, option: Int) = objectActions[id to option]
    internal fun npcAction(id: Int, option: Int) = npcActions[id to option]
}
