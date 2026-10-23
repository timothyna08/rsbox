package io.rsbox.server.engine.interaction

import io.rsbox.server.engine.model.coord.Tile
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class LadderPoint(val x: Int, val y: Int, val level: Int) {
    fun toTile() = Tile(x, y, level)

    companion object {
        fun from(tile: Tile) = LadderPoint(tile.x, tile.y, tile.level)
    }
}

@Serializable
data class LadderLink(
    val objectId: Int,
    val source: LadderPoint,
    val option: Int,
    val destination: LadderPoint
)

object LadderLinks {
    val projectFile = File("server/game/core/src/main/resources/ladder_links.json")
    private val json = Json { prettyPrint = true }

    fun load(): List<LadderLink> {
        val text = if(projectFile.exists()) projectFile.readText()
        else LadderLinks::class.java.getResourceAsStream("/ladder_links.json")?.bufferedReader()?.use { it.readText() }
            ?: error("Missing ladder_links.json")
        return json.decodeFromString(text)
    }

    fun save(links: List<LadderLink>) {
        projectFile.parentFile.mkdirs()
        projectFile.writeText(json.encodeToString(links.sortedWith(
            compareBy<LadderLink>({ it.source.level }, { it.source.x }, { it.source.y }, { it.option })
        )) + "\n")
    }
}
