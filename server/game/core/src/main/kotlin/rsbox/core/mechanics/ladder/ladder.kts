package rsbox.core.mechanics.ladder

import io.rsbox.server.engine.interaction.InteractionActions
import io.rsbox.server.engine.model.coord.Tile

private val ladder = Tile(3229, 3213, 0)

// A single ladder demonstrates a registered object click and destination.
InteractionActions.onObject(16683, 1) { player, obj ->
    if(obj.tile == ladder) player.teleportTo(Tile(3229, 3213, 1))
}
