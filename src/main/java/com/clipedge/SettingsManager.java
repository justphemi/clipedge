package com.clipedge;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class SettingsManager {
    private Properties properties;
    private Path settingsFile;

    public SettingsManager() {
        properties = new Properties();
        initSettings();
        loadSettings();
    }

    private void initSettings() {
        String storageDir = getStorageDirectory();
        try {
            Files.createDirectories(Paths.get(storageDir));
            settingsFile = Paths.get(storageDir, "settings.properties");
        } catch (IOException e) {
            e.printStackTrace();
            settingsFile = Paths.get(System.getProperty("java.io.tmpdir"), "cedge_settings.properties");
        }

        // Set defaults
        setDefaultIfMissing("autoCloseDelay", "5");
        setDefaultIfMissing("allowDuplicates", "false");
        setDefaultIfMissing("theme", "dark");
        setDefaultIfMissing("soundEnabled", "true");
        setDefaultIfMissing("modalWidth", "400");
        setDefaultIfMissing("modalHeight", "500");
    }

    private String getStorageDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        if (os.contains("win")) {
            return System.getenv("APPDATA") + File.separator + "Cedge";
        } else if (os.contains("mac")) {
            return home + File.separator + "Library" + File.separator + "Application Support" + File.separator + "Cedge";
        } else {
            return home + File.separator + ".config" + File.separator + "Cedge";
        }
    }

    private void setDefaultIfMissing(String key, String value) {
        if (!properties.containsKey(key)) {
            properties.setProperty(key, value);
        }
    }

    public void loadSettings() {
        try {
            if (Files.exists(settingsFile)) {
                try (InputStream input = Files.newInputStream(settingsFile)) {
                    properties.load(input);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading settings: " + e.getMessage());
        }
    }

    public void saveSettings() {
        try {
            try (OutputStream output = Files.newOutputStream(settingsFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                properties.store(output, "Cedge Settings");
            }
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }

    public int getAutoCloseDelay() {
        return Integer.parseInt(properties.getProperty("autoCloseDelay", "5"));
    }

    public void setAutoCloseDelay(int value) {
        properties.setProperty("autoCloseDelay", String.valueOf(value));
        saveSettings();
    }

    public boolean allowDuplicates() {
        return Boolean.parseBoolean(properties.getProperty("allowDuplicates", "false"));
    }

    public void setAllowDuplicates(boolean value) {
        properties.setProperty("allowDuplicates", String.valueOf(value));
        saveSettings();
    }

    public String getTheme() {
        return properties.getProperty("theme", "dark");
    }

    public void setTheme(String value) {
        properties.setProperty("theme", value);
        saveSettings();
    }

    public boolean isSoundEnabled() {
        return Boolean.parseBoolean(properties.getProperty("soundEnabled", "true"));
    }

    public void setSoundEnabled(boolean value) {
        properties.setProperty("soundEnabled", String.valueOf(value));
        saveSettings();
    }

    public double getModalWidth() {
        return Double.parseDouble(properties.getProperty("modalWidth", "400"));
    }

    public void setModalWidth(double value) {
        properties.setProperty("modalWidth", String.valueOf(value));
        saveSettings();
    }

    public double getModalHeight() {
        return Double.parseDouble(properties.getProperty("modalHeight", "500"));
    }

    public void setModalHeight(double value) {
        properties.setProperty("modalHeight", String.valueOf(value));
        saveSettings();
    }
}