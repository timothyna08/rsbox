package io.rsbox.server.engine.model.map

import io.rsbox.server.common.inject
import io.rsbox.server.engine.model.GameObject
import io.rsbox.server.engine.model.World
import io.rsbox.server.engine.model.coord.Chunk
import io.rsbox.server.engine.model.coord.Tile
import io.rsbox.server.engine.model.entity.Entity
import io.rsbox.server.engine.model.entity.Npc
import io.rsbox.server.engine.model.entity.Player
import io.rsbox.server.engine.net.packet.server.LocChange
import io.rsbox.server.engine.net.packet.server.UpdateZonePartialFollows

data class Zone(val chunk: Chunk) {

    private val world: World by inject()

    private val players = hashSetOf<Player>()
    private val npcs = hashSetOf<Npc>()

    private val dynamicObjectList = mutableListOf<GameObject>()
    private val staticObjectsList = mutableListOf<GameObject>()
    private val replacements = mutableMapOf<GameObject, GameObject>()

    fun getPlayers(tile: Tile) = players.filter { it.tile == tile }
    fun getNpcs(tile: Tile) = npcs.filter { it.tile == tile }
    fun getObjects(tile: Tile) = dynamicObjectList.plus(staticObjectsList).filter { it.tile == tile }

    fun addEntity(entity: Entity) {
        when(entity) {
            is Player -> players.add(entity)
            is Npc -> npcs.add(entity)
        }
    }

    fun removeEntity(entity: Entity) {
        when(entity) {
            is Player -> players.remove(entity)
            is Npc -> npcs.remove(entity)
        }
    }

    fun addObject(obj: GameObject, isDynamic: Boolean = true) {
        if(isDynamic) dynamicObjectList.add(obj) else staticObjectsList.add(obj)
        world.collision.addObjectCollision(obj)
    }

    fun removeObject(obj: GameObject) {
        dynamicObjectList.remove(obj)
        staticObjectsList.remove(obj)
        world.collision.removeObjectCollision(obj)
    }

    // Replace one cached object and keep its current appearance for later scene rebuilds.
    fun replaceObject(old: GameObject, new: GameObject): Boolean {
        require(old.tile == new.tile && old.shape == new.shape)
        val wasDynamic = dynamicObjectList.remove(old)
        val wasStatic = if(wasDynamic) false else staticObjectsList.remove(old)
        if(!wasDynamic && !wasStatic) return false

        world.collision.removeObjectCollision(old)
        val original = replacements.entries.firstOrNull { it.value == old }?.key
        if(original != null) replacements.remove(original)
        if(original == new) {
            staticObjectsList.add(new)
        } else {
            dynamicObjectList.add(new)
            if(wasStatic) replacements[old] = new
            else if(original != null) replacements[original] = new
        }
        world.collision.addObjectCollision(new)
        world.players.filterNotNull().filter { it.scene.contains(new.tile) }.forEach { sendObjectUpdate(it, new) }
        return true
    }

    fun sendUpdates(player: Player) {
        replacements.values.forEach { sendObjectUpdate(player, it) }
    }

    private fun sendObjectUpdate(player: Player, obj: GameObject) {
        val base = player.scene.baseTile.toChunk()
        val sceneX = obj.tile.x - (base.x - 6) * 8
        val sceneY = obj.tile.y - (base.y - 6) * 8
        player.session.write(UpdateZonePartialFollows(sceneX and -8, sceneY and -8))
        player.session.write(LocChange(obj, sceneX and 7, sceneY and 7))
    }
}
