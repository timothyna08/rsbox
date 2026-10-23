package rsbox.core.mechanics.door

import io.rsbox.server.engine.interaction.InteractionActions
import io.rsbox.server.engine.model.GameObject
import io.rsbox.server.engine.model.coord.Tile

private val doorTile = Tile(3226, 3214, 0)

// Toggle this Lumbridge door between its cached Open and Close objects.
InteractionActions.onObject(1535, 1) { player, obj ->
    if(obj.tile == doorTile && obj.shape == 0 && obj.rotation == 2) {
        player.world.zones[doorTile].replaceObject(obj, GameObject(1536, doorTile, 0, 3))
    }
}

InteractionActions.onObject(1536, 1) { player, obj ->
    if(obj.tile == doorTile && obj.shape == 0 && obj.rotation == 3) {
        player.world.zones[doorTile].replaceObject(obj, GameObject(1535, doorTile, 0, 2))
    }
}
