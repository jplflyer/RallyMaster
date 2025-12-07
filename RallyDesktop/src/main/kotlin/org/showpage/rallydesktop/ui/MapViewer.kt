package org.showpage.rallydesktop.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import org.jxmapviewer.JXMapViewer
import org.jxmapviewer.input.PanMouseInputListener
import org.jxmapviewer.painter.CompoundPainter
import org.jxmapviewer.painter.Painter
import org.jxmapviewer.viewer.*
import org.showpage.rallydesktop.service.ConfigurationService
import org.showpage.rallyserver.ui.UiBonusPoint
import org.slf4j.LoggerFactory
import java.awt.*
import java.net.URLConnection
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.event.MouseInputListener

private val logger = LoggerFactory.getLogger("MapViewer")

// Flag to ensure we only configure the User-Agent once
private var httpUserAgentConfigured = false

/**
 * Configure HTTP connections to include a proper User-Agent header.
 * This is required by OpenStreetMap's tile usage policy.
 */
private fun configureHttpUserAgent() {
    if (httpUserAgentConfigured) return

    try {
        // Configure connection properties through system properties
        System.setProperty("http.agent", "RallyMaster/1.0")

        // Also set it via URLConnection default request property (deprecated but still works)
        @Suppress("DEPRECATION")
        URLConnection.setDefaultRequestProperty("User-Agent", "RallyMaster/1.0 (contact: jpl@showpage.org)")

        logger.info("Configured HTTP User-Agent for tile requests")
        httpUserAgentConfigured = true
    } catch (e: Exception) {
        logger.warn("Could not configure HTTP User-Agent", e)
    }
}

/**
 * Composable wrapper for JXMapViewer2
 */
@Composable
fun MapViewer(
    bonusPoints: List<UiBonusPoint>,
    combinations: List<org.showpage.rallyserver.ui.UiCombination> = emptyList(),
    centerLatitude: Double?,
    centerLongitude: Double?,
    modifier: Modifier = Modifier
) {
    val mapViewer = remember { createMapViewer() }

    // Update map center when rally location changes
    LaunchedEffect(centerLatitude, centerLongitude) {
        if (centerLatitude != null && centerLongitude != null) {
            val center = GeoPosition(centerLatitude, centerLongitude)
            mapViewer.addressLocation = center
            logger.info("Map centered at: {}, {}", centerLatitude, centerLongitude)
        }
    }

    // Update bonus point markers when they change
    LaunchedEffect(bonusPoints, combinations) {
        if (bonusPoints.isNotEmpty()) {
            logger.info("Processing {} bonus points and {} combinations for map display",
                bonusPoints.size, combinations.size)

            // Build a map of bonus point ID to combination color
            val bonusPointColors = mutableMapOf<Int, String>()
            combinations.forEach { combo ->
                combo.combinationPoints?.forEach { cp ->
                    // Only set color if not already set (first combination wins)
                    if (!bonusPointColors.containsKey(cp.bonusPointId)) {
                        combo.markerColor?.let { color ->
                            bonusPointColors[cp.bonusPointId] = color
                            logger.debug("Assigning color {} to bonus point {} from combination {}",
                                color, cp.bonusPointId, combo.name)
                        }
                    }
                }
            }

            logger.info("Assigned colors to {} bonus points from combinations", bonusPointColors.size)

            // Log first few bonus points to debug
            bonusPoints.take(5).forEach { bp ->
                logger.info("Sample BP: code={}, name={}, lat={}, lon={}",
                    bp.code, bp.name, bp.latitude, bp.longitude)
            }

            val waypoints = bonusPoints.mapNotNull { bp ->
                if (bp.latitude != null && bp.longitude != null) {
                    // Filter out invalid coordinates (0 or very close to 0 is invalid for USA locations)
                    if (bp.latitude == 0.0 || bp.longitude == 0.0 ||
                        Math.abs(bp.latitude) < 0.01 || Math.abs(bp.longitude) < 0.01) {
                        logger.warn("Bonus point {} ({}) has invalid coordinates: lat={}, lon={}",
                            bp.code, bp.name, bp.latitude, bp.longitude)
                        null
                    } else {
                        // Use combination color if available, otherwise use bonus point's own color
                        // If no color at all, default to red
                        val effectiveColor = bp.id?.let { bonusPointColors[it] } ?: bp.markerColor ?: "#FF0000"

                        logger.debug("Bonus point {} ({}): color={}", bp.code, bp.name, effectiveColor)

                        BonusPointWaypoint(
                            geoPosition = GeoPosition(bp.latitude, bp.longitude),
                            code = bp.code ?: "??",
                            name = bp.name ?: "Unnamed",
                            markerColor = effectiveColor,
                            markerIcon = bp.markerIcon,
                            isStart = bp.isStart ?: false,
                            isFinish = bp.isFinish ?: false
                        )
                    }
                } else {
                    logger.warn("Bonus point {} ({}) has null coordinates", bp.code, bp.name)
                    null
                }
            }

            logger.info("Created {} waypoints from bonus points", waypoints.size)

            val waypointPainter = WaypointPainter<BonusPointWaypoint>().apply {
                setWaypoints(waypoints.toSet())
                setRenderer(ColoredWaypointRenderer())
            }

            // val painters = mutableListOf<Painter<JXMapViewer>>(waypointPainter)
            // mapViewer.overlayPainter = CompoundPainter(painters)
            mapViewer.overlayPainter = CompoundPainter(waypointPainter)

            logger.info("Updated map with {} bonus point markers using ColoredWaypointRenderer", waypoints.size)

            // Always zoom to fit all points for now to debug positioning
            if (waypoints.isNotEmpty()) {
                zoomToFitWaypoints(mapViewer, waypoints)
            }
        }
    }

    // Create a Swing panel with zoom controls overlaid on the map
    SwingPanel(
        factory = {
            // Create container panel with BorderLayout
            val containerPanel = JPanel(BorderLayout())

            // Add map viewer to center
            containerPanel.add(mapViewer, BorderLayout.CENTER)

            // Create zoom control panel
            val zoomPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 10))
            zoomPanel.isOpaque = false

            // Zoom out button
            val zoomInButton = JButton("-").apply {
                preferredSize = Dimension(40, 40)
                addActionListener {
                    val currentZoom = mapViewer.zoom
                    // We only let you zoom out to 15 not 17 cause 16 and 17 are just ridiculous.
                    if (currentZoom < 15) {
                        mapViewer.zoom = currentZoom + 1
                        logger.info("Zoomed in to level {}", currentZoom + 1)
                    }
                }
            }

            // Zoom in button
            val zoomOutButton = JButton("+").apply {
                preferredSize = Dimension(40, 40)
                addActionListener {
                    val currentZoom = mapViewer.zoom
                    if (currentZoom > 0) {
                        mapViewer.zoom = currentZoom - 1
                        logger.info("Zoomed out to level {}", currentZoom - 1)
                    }
                }
            }

            zoomPanel.add(zoomInButton)
            zoomPanel.add(zoomOutButton)

            // Overlay zoom panel on top-right using OverlayLayout would be complex
            // Instead, use a simple approach with a north panel
            val topPanel = JPanel(BorderLayout())
            topPanel.isOpaque = false
            topPanel.add(zoomPanel, BorderLayout.EAST)

            containerPanel.add(topPanel, BorderLayout.NORTH)

            containerPanel
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Create and configure the JXMapViewer instance
 */
private fun createMapViewer(): JXMapViewer {
    val mapViewer = JXMapViewer()

    // Configure URLConnection to add User-Agent header for all OSM requests
    configureHttpUserAgent()

    // Set up tile cache directory
    val cacheDir = java.io.File(System.getProperty("user.home"), ".rallymaster/map-cache")
    try {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
            logger.info("Created tile cache directory: {}", cacheDir.absolutePath)
        }
    } catch (e: Exception) {
        logger.warn("Could not create tile cache directory", e)
    }

    // Load Mapbox API token from configuration
    val config = ConfigurationService.getInstance()
    val mapboxToken = config.getMapboxToken()

    if (mapboxToken == null) {
        logger.error("Mapbox token not found in configuration!")
        // Fall back to a simple error message on the map
        // For now, continue without tiles - user will see blank map
    }

    // Create a TileFactoryInfo for Mapbox
    // totalMapZoom MUST be >= maxZoom to avoid array bounds errors
    // minZoom=2 prevents zooming out too far (Mapbox zoom 15)
    val tileFactoryInfo = object : TileFactoryInfo(
        0,      // min zoom (JXMapViewer 2 = Mapbox 15)
        17,     // max zoom
        17,     // total map zoom - MUST equal max zoom
        256,    // tile size
        true,   // x left to right
        true,   // y top to bottom (XYZ style)
        "https://api.mapbox.com",
        "x", "y", "z"
    ) {
        override fun getTileUrl(x: Int, y: Int, zoom: Int): String {
            // Empirically determined: the coordinates need to be divided by 2
            // At zoom 8: we get x=119, need x=60 (119/2=59.5≈60)
            // At zoom 9: we get x=59, need x=119 (would need to multiply)
            //val tileX = x / 2
            //val tileY = y / 2
            val tileX = x
            val tileY = y
            val useZoom = maximumZoomLevel - zoom
            val factor = 1 shl (getTotalMapZoom() - zoom)  // For debugging

            val maxTileCoord = (1 shl zoom) - 1
            logger.info("getTileUrl: zoom={}, x={}, y={}, factor={} -> tile x={}, y={} (max={})",
                zoom, x, y, factor, tileX, tileY, maxTileCoord)

            // Validate tile coordinates
            /*
            if (tileX < 0 || tileY < 0 || tileX > maxTileCoord || tileY > maxTileCoord) {
                logger.warn("Tile coordinates out of range: zoom={}, tile x={}, y={} (max={})",
                    zoom, tileX, tileY, maxTileCoord)
                // Clamp to valid range
                val clampedX = tileX.coerceIn(0, maxTileCoord)
                val clampedY = tileY.coerceIn(0, maxTileCoord)
                val url = "https://api.mapbox.com/styles/v1/mapbox/streets-v12/tiles/256/$useZoom/$clampedX/$clampedY?access_token=$mapboxToken"
                return url
            }

             */

            // Mapbox Styles API - modern, well-supported
            // streets-v12 is the current standard street map style
            // Format: /styles/v1/{username}/{style_id}/tiles/{tileSize}/{z}/{x}/{y}
            val url = "https://api.mapbox.com/styles/v1/mapbox/streets-v12/tiles/256/$useZoom/$tileX/$tileY?access_token=$mapboxToken"
            logger.info("Requesting tile URL: {}", url)
            return url
        }
    }

    val tileFactory = DefaultTileFactory(tileFactoryInfo)
    tileFactory.setThreadPoolSize(8)  // Mapbox can handle more concurrent requests

    mapViewer.tileFactory = tileFactory

    // Set default center (USA center) and zoom
    mapViewer.addressLocation = GeoPosition(39.8283, -98.5795)
    mapViewer.zoom = 5

    // Add mouse controls
    val mia: MouseInputListener = PanMouseInputListener(mapViewer)
    mapViewer.addMouseListener(mia)
    mapViewer.addMouseMotionListener(mia)

    // Custom mouse wheel listener with reduced sensitivity
    // Accumulate wheel rotation and only zoom when threshold is reached
    var wheelRotationAccumulator = 0.0
    mapViewer.addMouseWheelListener { e ->
        wheelRotationAccumulator += e.preciseWheelRotation

        // Require 3 notches to zoom one level (adjust this value to change sensitivity)
        val threshold = 3.0

        if (Math.abs(wheelRotationAccumulator) >= threshold) {
            val zoomChange = if (wheelRotationAccumulator > 0) 1 else -1
            val newZoom = mapViewer.zoom + zoomChange

            // Respect zoom limits (0-15)
            if (newZoom in 0..15) {
                mapViewer.zoom = newZoom
                logger.info("Mouse wheel zoom to level {}", newZoom)
            }

            // Reset accumulator after zooming
            wheelRotationAccumulator = 0.0
        }
    }

    logger.info("Map viewer created with Mapbox tiles")

    return mapViewer
}

/**
 * Waypoint representing a bonus point on the map
 */
class BonusPointWaypoint(
    private val geoPosition: GeoPosition,
    val code: String,
    val name: String,
    val markerColor: String? = null,
    val markerIcon: String? = null,
    val isStart: Boolean = false,
    val isFinish: Boolean = false
) : Waypoint {
    override fun getPosition(): GeoPosition = geoPosition
}

/**
 * Zoom the map to fit all waypoints
 */
private fun zoomToFitWaypoints(mapViewer: JXMapViewer, waypoints: List<BonusPointWaypoint>) {
    if (waypoints.isEmpty()) return

    var minLat = Double.POSITIVE_INFINITY
    var maxLat = Double.NEGATIVE_INFINITY
    var minLon = Double.POSITIVE_INFINITY
    var maxLon = Double.NEGATIVE_INFINITY

    for (wp in waypoints) {
        val lat = wp.getPosition().latitude
        val lon = wp.getPosition().longitude
        minLat = minOf(minLat, lat)
        maxLat = maxOf(maxLat, lat)
        minLon = minOf(minLon, lon)
        maxLon = maxOf(maxLon, lon)
    }

    val centerLat = (minLat + maxLat) / 2.0
    val centerLon = (minLon + maxLon) / 2.0
    val center = GeoPosition(centerLat, centerLon)

    logger.info("Waypoint bounds: lat=[{}, {}], lon=[{}, {}]", minLat, maxLat, minLon, maxLon)
    logger.info("Setting center to: lat={}, lon={}", centerLat, centerLon)

    mapViewer.addressLocation = center

    // Calculate appropriate zoom level based on bounds
    // This is a simple heuristic - could be improved
    val latSpan = maxLat - minLat
    val lonSpan = maxLon - minLon
    val maxSpan = maxOf(latSpan, lonSpan)

    val zoom = when {
        maxSpan > 10.0 -> 5
        maxSpan > 5.0 -> 6
        maxSpan > 2.0 -> 7
        maxSpan > 1.0 -> 8
        maxSpan > 0.5 -> 9
        maxSpan > 0.2 -> 10
        maxSpan > 0.1 -> 11
        else -> 12
    }

    mapViewer.zoom = zoom
    logger.info("Zoomed to fit {} waypoints at zoom level {}", waypoints.size, zoom)
}

/**
 * Custom renderer for colored waypoints
 */
class ColoredWaypointRenderer : WaypointRenderer<BonusPointWaypoint> {
    /*
    override fun paintWaypoint(g: Graphics2D, map: JXMapViewer, waypoint: BonusPointWaypoint) {
        // val p = map.convertGeoPositionToPoint(waypoint.position)
        val p = map.tileFactory.geoToPixel(waypoint.position, map.zoom)
        g.color = Color.RED
        g.fillRect(p.x.toInt() - 2, p.y.toInt() - 2, 4, 4)
    }
     */

    override fun paintWaypoint(g: Graphics2D, map: JXMapViewer, waypoint: BonusPointWaypoint) {
        // Use "world pixel" coordinates; WaypointPainter handles viewport translation.
        val p = map.tileFactory.geoToPixel(waypoint.position, map.zoom)

        // The tip of the pin should be at the exact GPS coordinate
        val tipX = p.x.toInt()
        val tipY = p.y.toInt()

        // Parse the color from the hex string
        val color = parseColor(waypoint.markerColor)

        // Enable anti-aliasing for smoother rendering
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Determine which icon to draw
        val icon = waypoint.markerIcon?.lowercase() ?: "circle"

        when {
            waypoint.isFinish -> drawCheckeredFlagPin(g, tipX, tipY, color)
            waypoint.isStart -> drawStartFlagPin(g, tipX, tipY)
            else -> when (icon) {
                "circle" -> drawCirclePin(g, tipX, tipY, color)
                "square" -> drawSquarePin(g, tipX, tipY, color)
                "star" -> drawStarPin(g, tipX, tipY, color)
                "triangle" -> drawTrianglePin(g, tipX, tipY, color)
                "diamond" -> drawDiamondPin(g, tipX, tipY, color)
                "flag" -> drawFlagPin(g, tipX, tipY, color)
                else -> drawCirclePin(g, tipX, tipY, color) // Default to circle
            }
        }
    }

    /**
     * Draw a pushpin with a circular head
     */
    private fun drawCirclePin(g: Graphics2D, tipX: Int, tipY: Int, color: Color) {
        val headSize = 16
        val headRadius = headSize / 2
        val pinLength = 8

        // Center of the circle head
        val headX = tipX
        val headY = tipY - pinLength - headRadius

        // Draw pin stem (thin line from head to tip)
        g.color = color.darker()
        g.stroke = BasicStroke(2f)
        g.drawLine(tipX, tipY, headX, headY + headRadius)

        // Draw shadow for the head
        g.color = Color(0, 0, 0, 50)
        g.fillOval(headX - headRadius + 1, headY - headRadius + 1, headSize, headSize)

        // Draw the circular head
        g.color = color
        g.fillOval(headX - headRadius, headY - headRadius, headSize, headSize)

        // Draw white border
        g.color = Color.WHITE
        g.stroke = BasicStroke(2f)
        g.drawOval(headX - headRadius, headY - headRadius, headSize, headSize)

        // Draw inner dark border
        g.color = Color(0, 0, 0, 100)
        g.stroke = BasicStroke(1f)
        g.drawOval(headX - headRadius, headY - headRadius, headSize, headSize)
    }

    /**
     * Draw a pushpin with a square head
     */
    private fun drawSquarePin(g: Graphics2D, tipX: Int, tipY: Int, color: Color) {
        val headSize = 14
        val halfSize = headSize / 2
        val pinLength = 8

        val headX = tipX - halfSize
        val headY = tipY - pinLength - headSize

        // Draw pin stem
        g.color = color.darker()
        g.stroke = BasicStroke(2f)
        g.drawLine(tipX, tipY, tipX, headY + headSize)

        // Draw shadow
        g.color = Color(0, 0, 0, 50)
        g.fillRect(headX + 1, headY + 1, headSize, headSize)

        // Draw square head
        g.color = color
        g.fillRect(headX, headY, headSize, headSize)

        // Draw border
        g.color = Color.WHITE
        g.stroke = BasicStroke(2f)
        g.drawRect(headX, headY, headSize, headSize)

        g.color = Color(0, 0, 0, 100)
        g.stroke = BasicStroke(1f)
        g.drawRect(headX, headY, headSize, headSize)
    }

    /**
     * Draw a pushpin with a star head
     */
    private fun drawStarPin(g: Graphics2D, tipX: Int, tipY: Int, color: Color) {
        val outerRadius = 10
        val innerRadius = 4
        val pinLength = 8

        val centerX = tipX
        val centerY = tipY - pinLength - outerRadius

        // Draw pin stem
        g.color = color.darker()
        g.stroke = BasicStroke(2f)
        g.drawLine(tipX, tipY, centerX, centerY + outerRadius)

        // Create star shape
        val star = createStarShape(centerX, centerY, outerRadius, innerRadius, 5)

        // Draw shadow
        g.color = Color(0, 0, 0, 50)
        g.translate(1, 1)
        g.fill(star)
        g.translate(-1, -1)

        // Draw star
        g.color = color
        g.fill(star)

        // Draw border
        g.color = Color.WHITE
        g.stroke = BasicStroke(2f)
        g.draw(star)

        g.color = Color(0, 0, 0, 100)
        g.stroke = BasicStroke(1f)
        g.draw(star)
    }

    /**
     * Draw a pushpin with a triangle head
     */
    private fun drawTrianglePin(g: Graphics2D, tipX: Int, tipY: Int, color: Color) {
        val size = 16
        val pinLength = 8

        val topY = tipY - pinLength - size
        val bottomY = tipY - pinLength

        // Triangle points
        val xPoints = intArrayOf(tipX, tipX - size/2, tipX + size/2)
        val yPoints = intArrayOf(topY, bottomY, bottomY)

        // Draw pin stem
        g.color = color.darker()
        g.stroke = BasicStroke(2f)
        g.drawLine(tipX, tipY, tipX, bottomY)

        // Draw shadow
        g.color = Color(0, 0, 0, 50)
        g.fillPolygon(intArrayOf(xPoints[0] + 1, xPoints[1] + 1, xPoints[2] + 1),
                      intArrayOf(yPoints[0] + 1, yPoints[1] + 1, yPoints[2] + 1), 3)

        // Draw triangle
        g.color = color
        g.fillPolygon(xPoints, yPoints, 3)

        // Draw border
        g.color = Color.WHITE
        g.stroke = BasicStroke(2f)
        g.drawPolygon(xPoints, yPoints, 3)

        g.color = Color(0, 0, 0, 100)
        g.stroke = BasicStroke(1f)
        g.drawPolygon(xPoints, yPoints, 3)
    }

    /**
     * Draw a pushpin with a diamond head
     */
    private fun drawDiamondPin(g: Graphics2D, tipX: Int, tipY: Int, color: Color) {
        val size = 12
        val pinLength = 8

        val centerY = tipY - pinLength - size

        // Diamond points (rotated square)
        val xPoints = intArrayOf(tipX, tipX - size, tipX, tipX + size)
        val yPoints = intArrayOf(centerY - size, centerY, centerY + size, centerY)

        // Draw pin stem
        g.color = color.darker()
        g.stroke = BasicStroke(2f)
        g.drawLine(tipX, tipY, tipX, yPoints[2])

        // Draw shadow
        g.color = Color(0, 0, 0, 50)
        g.fillPolygon(intArrayOf(xPoints[0] + 1, xPoints[1] + 1, xPoints[2] + 1, xPoints[3] + 1),
                      intArrayOf(yPoints[0] + 1, yPoints[1] + 1, yPoints[2] + 1, yPoints[3] + 1), 4)

        // Draw diamond
        g.color = color
        g.fillPolygon(xPoints, yPoints, 4)

        // Draw border
        g.color = Color.WHITE
        g.stroke = BasicStroke(2f)
        g.drawPolygon(xPoints, yPoints, 4)

        g.color = Color(0, 0, 0, 100)
        g.stroke = BasicStroke(1f)
        g.drawPolygon(xPoints, yPoints, 4)
    }

    /**
     * Draw a pushpin with a flag head
     */
    private fun drawFlagPin(g: Graphics2D, tipX: Int, tipY: Int, color: Color) {
        val flagWidth = 14
        val flagHeight = 10
        val poleLength = 20

        val poleTop = tipY - poleLength
        val flagTop = poleTop

        // Draw pole
        g.color = Color(100, 100, 100)
        g.stroke = BasicStroke(2f)
        g.drawLine(tipX, tipY, tipX, poleTop)

        // Flag shape (triangular)
        val xPoints = intArrayOf(tipX, tipX + flagWidth, tipX)
        val yPoints = intArrayOf(flagTop, flagTop + flagHeight/2, flagTop + flagHeight)

        // Draw shadow
        g.color = Color(0, 0, 0, 50)
        g.fillPolygon(intArrayOf(xPoints[0] + 1, xPoints[1] + 1, xPoints[2] + 1),
                      intArrayOf(yPoints[0] + 1, yPoints[1] + 1, yPoints[2] + 1), 3)

        // Draw flag
        g.color = color
        g.fillPolygon(xPoints, yPoints, 3)

        // Draw border
        g.color = Color.WHITE
        g.stroke = BasicStroke(1.5f)
        g.drawPolygon(xPoints, yPoints, 3)
    }

    /**
     * Draw a pushpin with a green flag for start locations
     */
    private fun drawStartFlagPin(g: Graphics2D, tipX: Int, tipY: Int) {
        drawFlagPin(g, tipX, tipY, Color(0, 180, 0)) // Green flag
    }

    /**
     * Draw a pushpin with a checkered flag for finish locations
     */
    private fun drawCheckeredFlagPin(g: Graphics2D, tipX: Int, tipY: Int, color: Color) {
        val flagWidth = 14
        val flagHeight = 10
        val poleLength = 20
        val checkSize = 3

        val poleTop = tipY - poleLength
        val flagTop = poleTop
        val flagLeft = tipX

        // Draw pole
        g.color = Color(100, 100, 100)
        g.stroke = BasicStroke(2f)
        g.drawLine(tipX, tipY, tipX, poleTop)

        // Draw checkered pattern
        for (row in 0 until flagHeight / checkSize) {
            for (col in 0 until flagWidth / checkSize) {
                val isBlack = (row + col) % 2 == 0
                g.color = if (isBlack) Color.BLACK else Color.WHITE
                g.fillRect(flagLeft + col * checkSize, flagTop + row * checkSize, checkSize, checkSize)
            }
        }

        // Draw flag border
        g.color = Color.DARK_GRAY
        g.stroke = BasicStroke(1.5f)
        g.drawRect(flagLeft, flagTop, flagWidth, flagHeight)
    }

    /**
     * Create a star shape with the given parameters
     */
    private fun createStarShape(cx: Int, cy: Int, outerRadius: Int, innerRadius: Int, points: Int): java.awt.Polygon {
        val xPoints = IntArray(points * 2)
        val yPoints = IntArray(points * 2)

        val angleStep = Math.PI / points

        for (i in 0 until points * 2) {
            val angle = i * angleStep - Math.PI / 2 // Start at top
            val radius = if (i % 2 == 0) outerRadius else innerRadius

            xPoints[i] = (cx + radius * Math.cos(angle)).toInt()
            yPoints[i] = (cy + radius * Math.sin(angle)).toInt()
        }

        return java.awt.Polygon(xPoints, yPoints, points * 2)
    }


    /*
    override fun paintWaypoint(g: Graphics2D, map: JXMapViewer, waypoint: BonusPointWaypoint) {
        // Draw the marker as a filled circle with a border
        val markerSize = 12
        val halfSize = markerSize / 2

        //val point = map.tileFactory.geoToPixel(waypoint.position, map.zoom)
        //val rect = map.viewportBounds
        //val x = (point.x - rect.x).toInt()
        //val y = (point.y - rect.y).toInt()

        val point = map.convertGeoPositionToPoint(waypoint.position)
        val x = point.x - markerSize / 2
        val y = point.y - markerSize / 2

        logger.info("Painting waypoint {} ({}) with color {}. Point: ({}, {}) mapped to ({}, {})",
            waypoint.code, waypoint.name, waypoint.markerColor,
            point.x, point.y, x, y )

        // Parse the color from the hex string
        val color = parseColor(waypoint.markerColor)

        // Enable anti-aliasing for smoother circles
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)


        // Draw shadow for depth
        g.color = Color(0, 0, 0, 50)
        g.fillOval((x - halfSize + 1).toInt(), (y - halfSize + 1).toInt(), markerSize, markerSize)

        // Draw the main marker
        g.color = color
        g.fillOval((x - halfSize).toInt(), (y - halfSize).toInt(), markerSize, markerSize)

        // Draw border
        g.color = Color.WHITE
        g.stroke = BasicStroke(2f)
        g.drawOval((x - halfSize).toInt(), (y - halfSize).toInt(), markerSize, markerSize)

        // Draw inner border for definition
        g.color = Color(0, 0, 0, 100)
        g.stroke = BasicStroke(1f)
        g.drawOval((x - halfSize).toInt(), (y - halfSize).toInt(), markerSize, markerSize)
    }

     */

    /**
     * Parse a hex color string like "#FF0000" into a Color object.
     * Returns a default red color if parsing fails.
     */
    private fun parseColor(colorStr: String?): Color {
        // Default to red if no color provided
        val effectiveColorStr = colorStr?.takeIf { it.isNotBlank() } ?: "#FF0000"

        return try {
            val hex = if (effectiveColorStr.startsWith("#")) {
                effectiveColorStr.substring(1)
            } else {
                effectiveColorStr
            }

            Color(
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16)
            )
        } catch (e: Exception) {
            logger.warn("Failed to parse color '{}', using default red", effectiveColorStr, e)
            Color.RED
        }
    }
}
