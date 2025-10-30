package com.clipedge;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.awt.*;
import java.awt.datatransfer.*;

public class ClipEdgeApp extends Application {
    private FloatingMenu floatingMenu;
    private ClipboardModal clipboardModal;
    private ClipboardManager clipboardManager;
    private SettingsManager settingsManager;
    private Thread clipboardMonitor;
    private volatile boolean monitoring = true;
    private String lastClipboard = "";
    
    @Override
    public void start(Stage primaryStage) {
        // Initialize managers
        settingsManager = new SettingsManager();
        clipboardManager = new ClipboardManager(settingsManager);

        // Create floating menu
        floatingMenu = new FloatingMenu(settingsManager);
        floatingMenu.show();

        // Create clipboard modal
        clipboardModal = new ClipboardModal(clipboardManager, settingsManager, floatingMenu);

        // Set click handler on floating menu to toggle modal
        floatingMenu.setOnClick(() -> {
            if (clipboardModal.isShowing()) {
                // Modal is already open, reposition it next to menu
                clipboardModal.repositionNearMenu(floatingMenu);
            } else {
                // Modal is closed, open it
                clipboardModal.showNearMenu(floatingMenu);
                floatingMenu.setModalOpen(true);
            }
        });

        // Set close handler on floating menu
        floatingMenu.setOnClose(() -> {
            clipboardModal.hide();
            floatingMenu.setModalOpen(false);
        });

        // Show modal on startup
        clipboardModal.showNearMenu(floatingMenu);
        floatingMenu.setModalOpen(true);

        // Start clipboard monitoring
        startClipboardMonitoring();

        // Hide primary stage (we only use floating menu and modal)
        primaryStage.initStyle(StageStyle.UTILITY);
        primaryStage.setOpacity(0);
        primaryStage.show();
        primaryStage.hide();

        // Cleanup on exit
        primaryStage.setOnCloseRequest(e -> cleanup());
    }

    private void startClipboardMonitoring() {
        clipboardMonitor = new Thread(() -> {
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                while (monitoring) {
                    try {
                        if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                            String data = (String) clipboard.getData(DataFlavor.stringFlavor);
                            if (data != null && !data.isEmpty() && !data.equals(lastClipboard)) {
                                lastClipboard = data;
                                // Add to clipboard manager on JavaFX thread
                                Platform.runLater(() -> {
                                    clipboardManager.addClipboardItem(data);
                                    if (settingsManager.isSoundEnabled()) {
                                        // Play animation
                                        floatingMenu.playAnimation();
                                    }
                                    // Update modal in real-time if it's showing
                                    if (clipboardModal.isShowing()) {
                                        clipboardModal.refreshItems();
                                    }
                                });
                            }
                        }
                        Thread.sleep(500); // Check every 500ms
                    } catch (Exception e) {
                        // Ignore clipboard access errors
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        clipboardMonitor.setDaemon(true);
        clipboardMonitor.start();
    }

    private void cleanup() {
        monitoring = false;
        if (clipboardMonitor != null) {
            clipboardMonitor.interrupt();
        }
        clipboardManager.saveToFile();
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}