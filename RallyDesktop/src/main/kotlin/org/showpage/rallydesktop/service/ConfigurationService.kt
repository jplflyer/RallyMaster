package org.showpage.rallydesktop.service

import org.slf4j.LoggerFactory
import java.util.Properties

private val logger = LoggerFactory.getLogger("ConfigurationService")

/**
 * Service for loading application configuration from properties file
 */
class ConfigurationService {
    private val properties = Properties()

    init {
        try {
            // Load properties from classpath
            val inputStream = javaClass.classLoader.getResourceAsStream("rallymaster.properties")
            if (inputStream != null) {
                properties.load(inputStream)
                logger.info("Loaded configuration from rallymaster.properties")
            } else {
                logger.warn("Could not find rallymaster.properties file")
            }
        } catch (e: Exception) {
            logger.error("Failed to load configuration", e)
        }
    }

    /**
     * Get the Mapbox API token
     */
    fun getMapboxToken(): String? {
        return properties.getProperty("mapbox.token")
    }

    companion object {
        private var instance: ConfigurationService? = null

        fun getInstance(): ConfigurationService {
            if (instance == null) {
                instance = ConfigurationService()
            }
            return instance!!
        }
    }
}
