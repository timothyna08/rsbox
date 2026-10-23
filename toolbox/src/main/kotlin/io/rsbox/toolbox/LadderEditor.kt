package io.rsbox.toolbox

import io.rsbox.server.cache.CacheModule
import io.rsbox.server.cache.GameCache
import io.rsbox.server.cache.map.MapRegionEntry
import io.rsbox.server.common.get
import io.rsbox.server.config.ConfigModule
import io.rsbox.server.config.XteaConfig
import io.rsbox.server.engine.EngineModule
import io.rsbox.server.engine.interaction.LadderLink
import io.rsbox.server.engine.interaction.LadderLinks
import io.rsbox.server.engine.interaction.LadderPoint
import org.koin.core.context.startKoin
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridLayout
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.SwingWorker
import kotlin.math.abs
import kotlin.math.min

object LadderEditor {
    @JvmStatic
    fun main(args: Array<String>) {
        SwingUtilities.invokeLater { EditorWindow().isVisible = true }
    }
}

private data class Option(val number: Int, val name: String) {
    override fun toString() = "$number: $name"
}

private data class Ladder(
    val id: Int,
    val point: LadderPoint,
    val name: String,
    val options: List<Option>
) {
    override fun toString() = "$name · $id · ${point.x}, ${point.y}, level ${point.level}"
}

private data class WorldData(
    val regions: Map<Int, MapRegionEntry>,
    val ladders: List<Ladder>,
    val links: List<LadderLink>
)

private class EditorWindow : JFrame("RSBox Ladder Editor") {
    private val search = JTextField(19)
    private val xField = JTextField("3229", 5)
    private val yField = JTextField("3213", 5)
    private val floor = JComboBox(arrayOf(0, 1, 2, 3))
    private val ladderModel = DefaultListModel<Ladder>()
    private val ladderList = JList(ladderModel)
    private val linkModel = DefaultListModel<LadderLink>()
    private val linkList = JList(linkModel)
    private val sourceLabel = JLabel("Select a ladder on the map or from the list")
    private val connectionLabel = JLabel("Select a ladder to see its connection")
    private val destinationLabel = JLabel("No arrival tile selected")
    private val suggestionLabel = JLabel("No suggestion selected")
    private val optionBox = JComboBox<Option>()
    private val status = JLabel("Loading local game cache…")
    private val map = MapView()

    private var data: WorldData? = null
    private var source: Ladder? = null
    private var destination: LadderPoint? = null
    private var suggestion: LadderPoint? = null
    private var pickingDestination = false
    private var dirty = false
    private var viewX = 3229 / 64 * 64
    private var viewY = 3213 / 64 * 64

    init {
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(event: java.awt.event.WindowEvent) {
                if(!dirty || JOptionPane.showConfirmDialog(this@EditorWindow,
                        "Discard unsaved ladder changes?", "Close editor", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    dispose()
                }
            }
        })
        minimumSize = Dimension(1180, 720)
        size = Dimension(1450, 850)
        setLocationRelativeTo(null)
        layout = BorderLayout(8, 8)
        add(buildToolbar(), BorderLayout.NORTH)
        add(buildMain(), BorderLayout.CENTER)
        status.border = BorderFactory.createEmptyBorder(5, 10, 7, 10)
        add(status, BorderLayout.SOUTH)
        loadWorld()
    }

    private fun buildToolbar(): JPanel {
        val bar = JPanel()
        bar.add(JLabel("Find ladder"))
        bar.add(search)
        bar.add(JButton("Search").apply { addActionListener { searchLadders() } })
        search.addActionListener { searchLadders() }
        bar.add(JLabel("Go to X"))
        bar.add(xField)
        bar.add(JLabel("Y"))
        bar.add(yField)
        bar.add(JButton("Go").apply { addActionListener { goToFields() } })
        bar.add(JLabel("Floor"))
        bar.add(floor)
        floor.addActionListener { map.repaint() }
        return bar
    }

    private fun buildMain(): JPanel {
        val content = JPanel(BorderLayout(8, 8))
        ladderList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        ladderList.cellRenderer = object : javax.swing.DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>, value: Any?, index: Int,
                isSelected: Boolean, cellHasFocus: Boolean): java.awt.Component {
                val ladder = value as? Ladder
                val links = ladder?.let(::linksFor).orEmpty()
                val label = if(ladder == null) "" else {
                    val name = ladder.name.replace("&", "&amp;").replace("<", "&lt;")
                    val place = "${ladder.point.x},${ladder.point.y},L${ladder.point.level}"
                    if(links.isEmpty()) "<html>○ $name #${ladder.id}<br>$place · Not connected</html>"
                    else "<html>✓ $name #${ladder.id} · $place<br>${links.joinToString("<br>") {
                        "Option ${it.option} → ${it.destination.x},${it.destination.y},L${it.destination.level}"
                    }}</html>"
                }
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus)
            }
        }
        ladderList.addListSelectionListener {
            if(!it.valueIsAdjusting) ladderList.selectedValue?.let(::selectSource)
        }
        content.add(JScrollPane(ladderList).apply {
            preferredSize = Dimension(280, 650)
            border = BorderFactory.createTitledBorder("Cached ladders and stairs")
        }, BorderLayout.WEST)

        val center = JPanel(BorderLayout())
        center.add(map, BorderLayout.CENTER)
        val navigation = JPanel()
        navigation.add(JButton("← West").apply { addActionListener { moveRegion(-64, 0) } })
        navigation.add(JButton("↑ North").apply { addActionListener { moveRegion(0, 64) } })
        navigation.add(JButton("↓ South").apply { addActionListener { moveRegion(0, -64) } })
        navigation.add(JButton("East →").apply { addActionListener { moveRegion(64, 0) } })
        center.add(navigation, BorderLayout.SOUTH)
        content.add(center, BorderLayout.CENTER)

        val details = JPanel()
        details.layout = BoxLayout(details, BoxLayout.Y_AXIS)
        details.border = BorderFactory.createTitledBorder("Ladder connection")
        details.preferredSize = Dimension(340, 650)
        details.add(JLabel("Source"))
        details.add(sourceLabel)
        details.add(JLabel("Click option"))
        details.add(optionBox)
        optionBox.addActionListener { updateConnectionIndicator() }
        details.add(connectionLabel)
        details.add(JButton("Pick arrival tile on map").apply {
            addActionListener {
                pickingDestination = true
                status.text = "Choose a floor, then click the arrival tile on the map."
            }
        })
        details.add(destinationLabel)
        details.add(JButton("Suggest nearby ladder").apply { addActionListener { suggestDestination() } })
        details.add(suggestionLabel)
        details.add(JButton("Use suggested tile").apply {
            addActionListener { suggestion?.let { setDestination(it) } }
        })
        details.add(JLabel("Saved connections"))
        linkList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        linkList.cellRenderer = object : javax.swing.DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>, value: Any?, index: Int,
                isSelected: Boolean, cellHasFocus: Boolean): java.awt.Component {
                val link = value as? LadderLink
                val label = link?.let {
                    "${it.source.x},${it.source.y},${it.source.level} → ${it.destination.x},${it.destination.y},${it.destination.level} (option ${it.option})"
                } ?: ""
                return super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus)
            }
        }
        linkList.addListSelectionListener {
            if(!it.valueIsAdjusting) linkList.selectedValue?.let(::editLink)
        }
        details.add(JScrollPane(linkList).apply { preferredSize = Dimension(325, 270) })
        val buttons = JPanel(GridLayout(2, 2, 5, 5))
        buttons.add(JButton("Add / update").apply { addActionListener { addOrUpdate() } })
        buttons.add(JButton("Remove").apply { addActionListener { removeLink() } })
        buttons.add(JButton("Save to project").apply { addActionListener { saveLinks() } })
        details.add(buttons)
        content.add(details, BorderLayout.EAST)
        return content
    }

    private fun loadWorld() {
        object : SwingWorker<WorldData, Unit>() {
            override fun doInBackground(): WorldData {
                startKoin { modules(ConfigModule, CacheModule, EngineModule) }
                val cache = get<GameCache>()
                cache.load()
                val regions = XteaConfig.xteas.keys.associateWith { cache.mapArchive[it] }
                val ladders = mutableListOf<Ladder>()
                for(entry in regions.values) {
                    for(level in 0..3) for(x in 0..63) for(y in 0..63) {
                        entry.locations[entry.pack(x, y, level)]?.forEach { loc ->
                            if(loc == null) return@forEach
                            val definition = cache.configArchive.objects[loc.id] ?: return@forEach
                            if(!definition.name.contains("ladder", true) && !definition.name.contains("stair", true)) return@forEach
                            val options = definition.options.mapIndexedNotNull { index, text ->
                                text?.takeIf { it.contains("Climb", true) }?.let { Option(index + 1, it) }
                            }
                            if(options.isNotEmpty()) ladders.add(Ladder(loc.id,
                                LadderPoint((entry.regionX shl 6) + loc.x, (entry.regionY shl 6) + loc.y, loc.level),
                                definition.name, options))
                        }
                    }
                }
                return WorldData(regions, ladders.sortedWith(compareBy({ it.point.level }, { it.point.x }, { it.point.y })), LadderLinks.load())
            }

            override fun done() {
                try {
                    data = get()
                    data!!.ladders.forEach(ladderModel::addElement)
                    data!!.links.forEach(linkModel::addElement)
                    ladderList.repaint()
                    val invalid = data!!.links.count { validate(it, data!!) != null }
                    status.text = if(invalid == 0) "Loaded ${data!!.ladders.size} climbable objects. Select a ladder to begin."
                        else "$invalid saved connections need correction before saving."
                    println("Ladder editor loaded ${data!!.ladders.size} climbable objects and ${data!!.links.size} saved connections ($invalid invalid).")
                    map.repaint()
                } catch(error: Exception) {
                    status.text = "Could not load the editor data."
                    error.printStackTrace()
                    JOptionPane.showMessageDialog(this@EditorWindow, error.cause?.message ?: error.message,
                        "Load failed", JOptionPane.ERROR_MESSAGE)
                }
            }
        }.execute()
    }

    private fun searchLadders() {
        val world = data ?: return
        val query = search.text.trim()
        val coords = Regex("^(\\d+)[,\\s]+(\\d+)(?:[,\\s]+([0-3]))?$").matchEntire(query)
        val matches = when {
            query.isEmpty() -> world.ladders
            coords != null -> {
                val x = coords.groupValues[1].toInt()
                val y = coords.groupValues[2].toInt()
                val level = coords.groupValues[3].takeIf(String::isNotEmpty)?.toInt()
                world.ladders.filter { it.point.x == x && it.point.y == y && (level == null || it.point.level == level) }
            }
            query.toIntOrNull() != null -> world.ladders.filter { it.id == query.toInt() }
            else -> world.ladders.filter { it.name.contains(query, true) }
        }
        ladderModel.clear()
        matches.forEach(ladderModel::addElement)
        status.text = "${matches.size} matching ladders."
        if(matches.size == 1) ladderList.selectedIndex = 0
    }

    private fun goToFields() {
        val x = xField.text.trim().toIntOrNull()
        val y = yField.text.trim().toIntOrNull()
        if(x == null || y == null || x !in 0..16383 || y !in 0..16383) {
            showError("Enter valid world coordinates for X and Y.")
            return
        }
        goTo(LadderPoint(x, y, currentFloor()))
    }

    private fun goTo(point: LadderPoint) {
        viewX = point.x / 64 * 64
        viewY = point.y / 64 * 64
        xField.text = point.x.toString()
        yField.text = point.y.toString()
        floor.selectedItem = point.level
        map.repaint()
    }

    private fun moveRegion(dx: Int, dy: Int) {
        viewX = (viewX + dx).coerceIn(0, 16320)
        viewY = (viewY + dy).coerceIn(0, 16320)
        xField.text = viewX.toString()
        yField.text = viewY.toString()
        map.repaint()
    }

    private fun selectSource(ladder: Ladder) {
        source = ladder
        destination = null
        suggestion = null
        sourceLabel.text = "${ladder.id} at ${format(ladder.point)}"
        destinationLabel.text = "No arrival tile selected"
        suggestionLabel.text = "No suggestion selected"
        optionBox.model = DefaultComboBoxModel(ladder.options.toTypedArray())
        updateConnectionIndicator()
        pickingDestination = false
        goTo(ladder.point)
        status.text = "Selected ${ladder.name}. Choose its option and an arrival tile."
    }

    private fun setDestination(point: LadderPoint) {
        destination = point
        destinationLabel.text = "Arrival: ${format(point)}"
        pickingDestination = false
        status.text = "Arrival selected. Add or update the connection, then save to the project."
        map.repaint()
    }

    private fun suggestDestination() {
        val world = data ?: return
        val from = source ?: return showError("Select a source ladder first.")
        val option = optionBox.selectedItem as? Option ?: return
        val levels = when {
            option.name.contains("up", true) -> listOf(from.point.level + 1)
            option.name.contains("down", true) -> listOf(from.point.level - 1)
            else -> listOf(from.point.level - 1, from.point.level + 1)
        }
        val nearby = world.ladders.asSequence().filter { it.point.level in levels }
            .filter { abs(it.point.x - from.point.x) <= 12 && abs(it.point.y - from.point.y) <= 12 }
            .minByOrNull { abs(it.point.x - from.point.x) + abs(it.point.y - from.point.y) }
        suggestion = nearby?.point
        suggestionLabel.text = nearby?.let { "Suggested: ${format(it.point)}" } ?: "No nearby ladder found"
        nearby?.let { goTo(it.point) }
    }

    private fun editLink(link: LadderLink) {
        data?.ladders?.firstOrNull { it.id == link.objectId && it.point == link.source }?.let { ladder ->
            selectSource(ladder)
            optionBox.selectedItem = ladder.options.firstOrNull { it.number == link.option }
        }
        setDestination(link.destination)
    }

    // Read from the current list so the indicator updates before saving too.
    private fun linksFor(ladder: Ladder) = (0 until linkModel.size()).map(linkModel::getElementAt)
        .filter { it.objectId == ladder.id && it.source == ladder.point }

    private fun updateConnectionIndicator() {
        val ladder = source
        val option = optionBox.selectedItem as? Option
        val link = if(ladder == null || option == null) null else linksFor(ladder).firstOrNull { it.option == option.number }
        connectionLabel.text = if(link == null) "Not connected" else "Connected to: ${format(link.destination)}"
        connectionLabel.foreground = if(link == null) Color(125, 125, 125) else Color(30, 140, 55)
    }

    private fun addOrUpdate() {
        val world = data ?: return
        val ladder = source ?: return showError("Select a source ladder first.")
        val option = optionBox.selectedItem as? Option ?: return showError("Select a climb option.")
        val arrival = destination ?: return showError("Pick an arrival tile first.")
        val link = LadderLink(ladder.id, ladder.point, option.number, arrival)
        val error = validate(link, world)
        if(error != null) return showError(error)
        val existing = (0 until linkModel.size()).firstOrNull {
            val current = linkModel.getElementAt(it)
            current.objectId == link.objectId && current.source == link.source && current.option == link.option
        }
        if(existing == null) linkModel.addElement(link) else linkModel.setElementAt(link, existing)
        dirty = true
        updateConnectionIndicator()
        ladderList.repaint()
        status.text = "Connection ready. Click Save to project to keep it."
        map.repaint()
    }

    private fun removeLink() {
        val index = linkList.selectedIndex
        if(index < 0) return showError("Select a saved connection to remove.")
        linkModel.remove(index)
        dirty = true
        updateConnectionIndicator()
        ladderList.repaint()
        status.text = "Connection removed. Click Save to project to keep the change."
        map.repaint()
    }

    private fun saveLinks() {
        val world = data ?: return
        val links = (0 until linkModel.size()).map(linkModel::getElementAt)
        val keys = mutableSetOf<Triple<Int, LadderPoint, Int>>()
        for(link in links) {
            validate(link, world)?.let { return showError(it) }
            if(!keys.add(Triple(link.objectId, link.source, link.option))) {
                return showError("Two connections use the same ladder and click option.")
            }
        }
        try {
            LadderLinks.save(links)
            dirty = false
            status.text = "Saved ${links.size} connections to the project. Restart the server to use them."
        } catch(error: Exception) {
            showError("Could not save connections: ${error.message}")
        }
    }

    private fun validate(link: LadderLink, world: WorldData): String? {
        val ladder = world.ladders.firstOrNull { it.id == link.objectId && it.point == link.source }
            ?: return "The source ladder was not found in the game cache."
        if(ladder.options.none { it.number == link.option }) {
            return "That climb option is not available on this ladder."
        }
        val target = link.destination
        if(target.x !in 0..16383 || target.y !in 0..16383 || target.level !in 0..3) {
            return "The arrival tile must have valid coordinates and a floor from 0 to 3."
        }
        if(link.source == target) return "The arrival tile must differ from the source."
        val regionId = (target.x / 64 shl 8) or (target.y / 64)
        if(regionId !in world.regions) return "The arrival tile is outside the loaded game map."
        return null
    }

    private fun showError(message: String) {
        JOptionPane.showMessageDialog(this, message, "Check connection", JOptionPane.WARNING_MESSAGE)
        status.text = message
    }

    private fun currentFloor() = floor.selectedItem as Int
    private fun format(point: LadderPoint) = "${point.x}, ${point.y}, level ${point.level}"

    private inner class MapView : JPanel() {
        init {
            preferredSize = Dimension(680, 680)
            background = Color(25, 29, 35)
            border = BorderFactory.createTitledBorder("Map · click a ladder to select it, or pick an arrival tile")
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(event: MouseEvent) {
                    val tile = pointAt(event.x, event.y) ?: return
                    if(pickingDestination) {
                        setDestination(tile)
                    } else {
                        val ladder = data?.ladders?.firstOrNull { it.point == tile }
                        if(ladder != null) selectSource(ladder)
                        else status.text = "No climbable ladder at ${format(tile)}."
                    }
                }
            })
        }

        private fun geometry(): Triple<Int, Int, Int> {
            val size = min(width - 36, height - 48).coerceAtLeast(64)
            val cell = (size / 64).coerceAtLeast(1)
            return Triple((width - cell * 64) / 2, (height - cell * 64) / 2 + 8, cell)
        }

        private fun pointAt(mouseX: Int, mouseY: Int): LadderPoint? {
            val (left, top, cell) = geometry()
            val localX = (mouseX - left) / cell
            val localY = 63 - (mouseY - top) / cell
            if(mouseX < left || mouseY < top || localX !in 0..63 || localY !in 0..63) return null
            return LadderPoint(viewX + localX, viewY + localY, currentFloor())
        }

        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)
            val g = graphics.create() as Graphics2D
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val (left, top, cell) = geometry()
            val level = currentFloor()
            val regionId = (viewX / 64 shl 8) or (viewY / 64)
            val entry = data?.regions?.get(regionId)
            for(x in 0..63) for(y in 0..63) {
                val terrain = entry?.terrain?.get(entry.pack(x, y, level))
                g.color = when {
                    terrain == null -> Color(35, 38, 43)
                    terrain.collision and 1 != 0 -> Color(63, 69, 74)
                    else -> Color(84, 94, 82)
                }
                g.fillRect(left + x * cell, top + (63 - y) * cell, cell, cell)
            }
            g.color = Color(115, 126, 133, 105)
            for(n in 0..64 step 8) {
                g.drawLine(left + n * cell, top, left + n * cell, top + 64 * cell)
                g.drawLine(left, top + n * cell, left + 64 * cell, top + n * cell)
            }
            data?.ladders?.forEach { ladder ->
                if(ladder.point.level == level && ladder.point.x in viewX until viewX + 64 && ladder.point.y in viewY until viewY + 64) {
                    val color = if(linksFor(ladder).isEmpty()) Color(65, 205, 223) else Color(108, 232, 115)
                    drawMarker(g, ladder.point, color, left, top, cell)
                }
            }
            suggestion?.let { drawMarker(g, it, Color(255, 210, 79), left, top, cell) }
            destination?.let { drawMarker(g, it, Color(105, 150, 255), left, top, cell) }
            source?.let { drawMarker(g, it.point, Color(255, 150, 65), left, top, cell) }
            g.font = Font(Font.SANS_SERIF, Font.PLAIN, 12)
            g.color = Color.WHITE
            g.drawString("Green: connected   Cyan: unconnected   Orange: selected", left, top - 6)
            g.drawString("Region ${viewX / 64}, ${viewY / 64} · X $viewX–${viewX + 63} · Y $viewY–${viewY + 63} · floor $level", left, top + 64 * cell + 17)
            g.dispose()
        }

        private fun drawMarker(g: Graphics2D, point: LadderPoint, color: Color, left: Int, top: Int, cell: Int) {
            if(point.level != currentFloor() || point.x !in viewX until viewX + 64 || point.y !in viewY until viewY + 64) return
            val x = left + (point.x - viewX) * cell + cell / 2
            val y = top + (63 - point.y + 1) * cell - cell / 2
            val radius = (cell / 2).coerceAtLeast(4)
            g.color = Color(0, 0, 0, 160)
            g.fillOval(x - radius - 2, y - radius - 2, radius * 2 + 4, radius * 2 + 4)
            g.color = color
            g.fillOval(x - radius, y - radius, radius * 2, radius * 2)
            g.stroke = BasicStroke(1.5f)
            g.color = Color.WHITE
            g.drawOval(x - radius, y - radius, radius * 2, radius * 2)
        }
    }
}
