package io.rsbox.server.engine.interaction

import io.rsbox.server.engine.model.GameObject
import io.rsbox.server.engine.model.coord.Tile
import io.rsbox.server.engine.model.entity.Npc
import io.rsbox.server.engine.model.entity.Player
import kotlin.math.sign

// Resolves clicks, approaches valid targets, then runs their registered actions.
class InteractionManager(private val player: Player) {

    private var pending: Pending? = null

    fun onObject(id: Int, tile: Tile, option: Int) {
        cancel()
        val action = InteractionActions.objectAction(id, option) ?: return
        val obj = player.world.zones[tile].getObjects(tile).firstOrNull { it.id == id } ?: return
        start(Pending({ obj.tile }, { player.world.zones[tile].getObjects(tile).contains(obj) }) {
            action(player, obj)
        })
    }

    fun onNpc(index: Int, option: Int) {
        cancel()
        if(index !in player.world.npcs.indices) return
        val npc = player.world.npcs[index] ?: return
        val action = InteractionActions.npcAction(npc.id, option) ?: return
        start(Pending({ npc.tile }, { player.world.npcs[index] === npc }) {
            action(player, npc)
        })
    }

    fun cancel() {
        pending = null
    }

    fun cycle() {
        val request = pending ?: return
        if(!request.exists()) {
            cancel()
            return
        }
        val tile = request.tile()
        if(tile.level != player.tile.level) {
            cancel()
        } else if(player.tile.isWithinRadius(tile, 1)) {
            cancel()
            request.action()
        } else if(++request.ticks > 50) {
            cancel()
        } else if(tile != request.lastTile) {
            request.lastTile = tile
            approach(tile)
        }
    }

    private fun start(request: Pending) {
        pending = request
        cycle()
    }

    private fun approach(tile: Tile) {
        val x = (player.tile.x - tile.x).sign
        val y = (player.tile.y - tile.y).sign
        player.walkTo(tile.translate(x, y))
    }

    private class Pending(val tile: () -> Tile, val exists: () -> Boolean, val action: () -> Unit) {
        var ticks = 0
        var lastTile: Tile? = null
    }
}
