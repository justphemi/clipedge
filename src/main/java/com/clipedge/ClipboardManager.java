package com.clipedge;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class ClipboardManager {
    private List<ClipboardItem> items;
    private SettingsManager settingsManager;
    private Gson gson;
    private Path storageFile;

    public ClipboardManager(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
        this.items = new ArrayList<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        initStorage();
        loadFromFile();
    }

    private void initStorage() {
        String storageDir = getStorageDirectory();
        try {
            Files.createDirectories(Paths.get(storageDir));
            storageFile = Paths.get(storageDir, "clipboard_history.json");
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback to temp directory
            storageFile = Paths.get(System.getProperty("java.io.tmpdir"), "cedge_history.json");
        }
    }

    private String getStorageDirectory() {
        // Use default storage location only
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

    public void addClipboardItem(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        
        text = text.trim();
        
        // Check for duplicates if enabled
        if (!settingsManager.allowDuplicates()) {
            // Create a final copy for use in lambda
            final String finalText = text;
            items.removeIf(item -> item.getText().equals(finalText));
        }
        
        ClipboardItem newItem = new ClipboardItem(text);
        items.add(0, newItem); // Add to beginning
        
        // Limit to 100 items
        if (items.size() > 100) {
            items = new ArrayList<>(items.subList(0, 100));
        }
        
        saveToFile();
    }

    public void removeItem(ClipboardItem item) {
        items.remove(item);
        saveToFile();
    }

    public void clearAll() {
        items.clear();
        saveToFile();
    }

    public List<ClipboardItem> getItems() {
        return new ArrayList<>(items);
    }

    public void saveToFile() {
        try {
            String json = gson.toJson(items);
            Files.write(storageFile, json.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error saving clipboard history: " + e.getMessage());
        }
    }

    public void loadFromFile() {
        try {
            if (Files.exists(storageFile)) {
                String json = new String(Files.readAllBytes(storageFile));
                Type listType = new TypeToken<ArrayList<ClipboardItem>>(){}.getType();
                List<ClipboardItem> loaded = gson.fromJson(json, listType);
                if (loaded != null) {
                    items = loaded;
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading clipboard history: " + e.getMessage());
            items = new ArrayList<>();
        }
    }
}