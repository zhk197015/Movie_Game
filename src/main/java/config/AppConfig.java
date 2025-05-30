package config;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application Configuration Class
 */
@Slf4j
public class AppConfig {
    private static final Properties properties = new Properties();
    private static final AppConfig INSTANCE = new AppConfig();

    private AppConfig() {
        loadProperties();
    }

    /**
     * Get a configuration instance
     */
    public static AppConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Loading a Configuration File
     */
    private void loadProperties() {
        try (InputStream input = AppConfig.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                log.error("Can not find application.properties file");
                return;
            }
            properties.load(input);
        } catch (IOException e) {
            log.error("Failed to load configuration file", e);
        }
    }

    /**
     * Get configuration items
     *
     * @param key Configuration Keys
     * @return Configuration values
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Get the configuration item and return the default value if it does not exist
     *
     * @param key          Configuration Keys
     * @param defaultValue defaultValue values
     * @return Configuration values
     */
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
