package rsbox.core.mechanics.ladder

import io.rsbox.server.engine.interaction.InteractionActions
import io.rsbox.server.engine.interaction.LadderLinks
import io.rsbox.server.engine.interaction.LadderPoint

private val links = LadderLinks.load().associateBy { Triple(it.objectId, it.source, it.option) }

// One action serves every ladder connection saved by the editor.
links.keys.map { it.first to it.third }.distinct().forEach { (id, option) ->
    InteractionActions.onObject(id, option) { player, obj ->
        links[Triple(id, LadderPoint.from(obj.tile), option)]?.let { link ->
            player.teleportTo(link.destination.toTile())
        }
    }
}
