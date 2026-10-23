package io.rsbox.server.game

import io.github.classgraph.ClassGraph
import io.rsbox.server.script.GameScript
import org.tinylog.kotlin.Logger

class GameScriptManager {

    fun load() {
        Logger.info("Scanning for game scripts to be loaded.")

        var count = 0
        ClassGraph().enableAllInfo().acceptPackages("rsbox.*").scan().use { scan ->
            scan.getSubclasses(GameScript::class.java).directOnly().forEach { info ->
                try {
                    val klass = info.loadClass(GameScript::class.java)
                    klass.getDeclaredConstructor().newInstance()
                    count++
                } catch (e: Exception) {
                    Logger.error(e) { "Failed to load plugin: ${info.name}. " }
                    return@forEach
                }
            }
        }

        Logger.info("Loaded $count game scripts.")
    }
}
