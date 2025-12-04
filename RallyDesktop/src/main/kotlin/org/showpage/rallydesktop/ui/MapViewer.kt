package org.showpage.rallydesktop.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import org.jxmapviewer.JXMapViewer
import org.jxmapviewer.OSMTileFactoryInfo
import org.jxmapviewer.input.PanMouseInputListener
import org.jxmapviewer.input.ZoomMouseWheelListenerCenter
import org.jxmapviewer.painter.CompoundPainter
import org.jxmapviewer.painter.Painter
import org.jxmapviewer.viewer.*
import org.showpage.rallydesktop.service.ConfigurationService
import org.showpage.rallyserver.ui.UiBonusPoint
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import javax.imageio.ImageIO
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
    LaunchedEffect(bonusPoints) {
        if (bonusPoints.isNotEmpty()) {
            logger.info("Processing {} bonus points for map display", bonusPoints.size)

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
                        BonusPointWaypoint(
                            geoPosition = GeoPosition(bp.latitude, bp.longitude),
                            code = bp.code ?: "??",
                            name = bp.name ?: "Unnamed"
                        )
                    }
                } else {
                    logger.warn("Bonus point {} ({}) has null coordinates", bp.code, bp.name)
                    null
                }
            }

            val waypointPainter = WaypointPainter<BonusPointWaypoint>().apply {
                setWaypoints(waypoints.toSet())
            }

            val painters = mutableListOf<Painter<JXMapViewer>>(waypointPainter)
            mapViewer.overlayPainter = CompoundPainter(painters)

            logger.info("Updated map with {} bonus point markers", waypoints.size)

            // Always zoom to fit all points for now to debug positioning
            if (waypoints.isNotEmpty()) {
                zoomToFitWaypoints(mapViewer, waypoints)
            }
        }
    }

    SwingPanel(
        factory = { mapViewer },
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
    val tileFactoryInfo = object : TileFactoryInfo(
        0,      // min zoom
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
    mapViewer.addMouseWheelListener(ZoomMouseWheelListenerCenter(mapViewer))

    logger.info("Map viewer created with Mapbox tiles")

    return mapViewer
}

/**
 * Waypoint representing a bonus point on the map
 */
class BonusPointWaypoint(
    private val geoPosition: GeoPosition,
    val code: String,
    val name: String
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
