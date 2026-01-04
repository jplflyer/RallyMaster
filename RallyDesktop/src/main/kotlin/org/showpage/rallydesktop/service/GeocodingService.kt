package org.showpage.rallydesktop.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private val logger = LoggerFactory.getLogger("GeocodingService")

/**
 * Result from a geocoding search
 */
data class GeocodingResult(
    val placeName: String,
    val latitude: Double,
    val longitude: Double,
    val relevance: Double
)

/**
 * Service for geocoding place names to coordinates using Mapbox Geocoding API
 */
class GeocodingService(private val mapboxToken: String?) {

    private val httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val objectMapper = ObjectMapper()

    /**
     * Search for a place by name and return geocoding results
     *
     * @param query The place name to search for (e.g., "Musky House Longville")
     * @param limit Maximum number of results to return (default 5)
     * @param proximityLatitude Optional latitude for proximity bias
     * @param proximityLongitude Optional longitude for proximity bias
     * @return List of geocoding results, or empty list if search fails
     */
    suspend fun searchPlace(
        query: String,
        limit: Int = 5,
        proximityLatitude: Double? = null,
        proximityLongitude: Double? = null
    ): List<GeocodingResult> {
        if (mapboxToken.isNullOrBlank()) {
            logger.error("Cannot geocode: Mapbox token is not configured")
            return emptyList()
        }

        if (query.isBlank()) {
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")

                // Mapbox Geocoding API v5
                // https://docs.mapbox.com/api/search/geocoding/
                var url = "https://api.mapbox.com/geocoding/v5/mapbox.places/$encodedQuery.json" +
                        "?access_token=$mapboxToken" +
                        "&limit=$limit" +
                        "&types=poi,address,place"  // Search points of interest, addresses, and places

                // Add proximity parameter if coordinates provided
                // Format: longitude,latitude (note the order!)
                if (proximityLatitude != null && proximityLongitude != null) {
                    url += "&proximity=$proximityLongitude,$proximityLatitude"
                    logger.info("Geocoding search for: {} with proximity bias at ({}, {})",
                        query, proximityLatitude, proximityLongitude)
                } else {
                    logger.info("Geocoding search for: {}", query)
                }

                val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "RallyMaster/1.0")
                    .GET()
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() != 200) {
                    logger.error("Geocoding API returned status {}: {}", response.statusCode(), response.body())
                    return@withContext emptyList()
                }

                @Suppress("UNCHECKED_CAST")
                val json = objectMapper.readValue<Map<String, Any>>(response.body())
                val features = json["features"] as? List<Map<String, Any>> ?: return@withContext emptyList()

                val results = mutableListOf<GeocodingResult>()

                for (feature in features) {
                    val placeName = feature["place_name"] as? String ?: "Unknown"
                    val relevance = (feature["relevance"] as? Number)?.toDouble() ?: 0.0

                    val geometry = feature["geometry"] as? Map<String, Any>
                    if (geometry != null && geometry["type"] == "Point") {
                        val coordinates = geometry["coordinates"] as? List<Number>
                        if (coordinates != null && coordinates.size >= 2) {
                            // Mapbox returns [longitude, latitude]
                            val longitude = coordinates[0].toDouble()
                            val latitude = coordinates[1].toDouble()

                            results.add(GeocodingResult(
                                placeName = placeName,
                                latitude = latitude,
                                longitude = longitude,
                                relevance = relevance
                            ))

                            logger.info("Found: {} at ({}, {}) relevance={}",
                                placeName, latitude, longitude, relevance)
                        }
                    }
                }

                logger.info("Geocoding returned {} results for query: {}", results.size, query)
                results

            } catch (e: Exception) {
                logger.error("Failed to geocode query: {}", query, e)
                emptyList()
            }
        }
    }
}
