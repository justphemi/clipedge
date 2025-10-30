package com.clipedge;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.awt.Toolkit;
import java.awt.datatransfer.*;

import javafx.scene.shape.Line;
import javafx.scene.layout.Region;


public class ClipboardModal {
    private Stage stage;
    private VBox root;
    private javafx.scene.control.ScrollPane scrollPane;
    private VBox itemsContainer;
    private StackPane toastContainer;
    private ClipboardManager clipboardManager;
    private SettingsManager settingsManager;
    private FloatingMenu floatingMenu;
    private Timeline autoCloseTimer;
    private double startX, startY;
    private String currentClipboardText = "";
    
    private double currentWidth;
    private double currentHeight;
    private double lastX = -1;  // Track last position
    private double lastY = -1;  // Track last position
    
    private static final double MIN_WIDTH = 400;
    private static final double MIN_HEIGHT = 500;
    private static final double MAX_WIDTH = 800;
    private static final double MAX_HEIGHT = 900;
    
    private ResizeMode resizeMode = ResizeMode.NONE;
    
    private enum ResizeMode {
        NONE, N, S, E, W, NE, NW, SE, SW
    }

    public ClipboardModal(ClipboardManager clipboardManager, SettingsManager settingsManager, FloatingMenu floatingMenu) {
        this.clipboardManager = clipboardManager;
        this.settingsManager = settingsManager;
        this.floatingMenu = floatingMenu;
        this.currentWidth = settingsManager.getModalWidth();
        this.currentHeight = settingsManager.getModalHeight();
        initUI();
        updateCurrentClipboard();
    }

    private void initUI() {
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        
        root = new VBox();
        root.setStyle(getGlassmorphicStyle());
        root.setEffect(new DropShadow(30, Color.rgb(196, 181, 224, 0.3)));
        
        // Header
        HBox header = createHeader();
        
        // Items container with toast overlay
        StackPane contentStack = new StackPane();
        
        itemsContainer = new VBox(8);
        itemsContainer.setPadding(new javafx.geometry.Insets(16));
        
        scrollPane = new javafx.scene.control.ScrollPane(itemsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; " +
                           "-fx-background: transparent; " +
                           "-fx-border-color: transparent;");
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Dark theme scrollbar styling - wait for skin to be ready
        scrollPane.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                Platform.runLater(() -> {
                    javafx.scene.Node scrollBar = scrollPane.lookup(".scroll-bar:vertical");
                    if (scrollBar != null) {
                        scrollBar.setStyle("-fx-background-color: transparent;");
                        javafx.scene.Node track = scrollPane.lookup(".scroll-bar:vertical .track");
                        if (track != null) {
                            track.setStyle("-fx-background-color: rgba(30, 30, 40, 0.5); -fx-background-radius: 5px;");
                        }
                        javafx.scene.Node thumb = scrollPane.lookup(".scroll-bar:vertical .thumb");
                        if (thumb != null) {
                            thumb.setStyle("-fx-background-color: rgba(196, 181, 224, 0.5); -fx-background-radius: 5px;");
                        }
                    }
                });
            }
        });
        
        // Toast container overlay
        toastContainer = new StackPane();
        toastContainer.setMouseTransparent(true);
        toastContainer.setPickOnBounds(false);
        
        contentStack.getChildren().addAll(scrollPane, toastContainer);
        VBox.setVgrow(contentStack, Priority.ALWAYS);
        
        // Footer with drag icon and auto-close slider
        HBox footer = createFooter();
        
        root.getChildren().addAll(header, contentStack, footer);
        
        Scene scene = new Scene(root, currentWidth, currentHeight);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        
        setupEventHandlers();
        setupResizeHandlers();
    }

    private HBox createHeader() {
        HBox header = new HBox(16);
        header.setPadding(new javafx.geometry.Insets(16, 20, 16, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: rgba(15, 15, 20, 0.8); -fx-background-radius: 10 10 0 0;");
        
        try {
            ImageView logo = new ImageView(new Image(getClass().getResourceAsStream("/cledge.png")));
            logo.setFitHeight(40);
            logo.setFitWidth(85);
            logo.setStyle("-fx-effect: dropshadow(gaussian, rgba(196, 181, 224, 0.5), 10, 0, 0, 0);");
            header.getChildren().add(logo);
        } catch (Exception e) {
            javafx.scene.control.Label title = new javafx.scene.control.Label("ClipEdge");
            title.setStyle("-fx-text-fill: #C4B5E0; -fx-font-size: 20px; -fx-font-weight: bold;");
            header.getChildren().add(title);
        }
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        javafx.scene.control.Button clearBtn = new javafx.scene.control.Button("Clear");
        clearBtn.setStyle(getButtonStyle());
        clearBtn.setOnAction(e -> showClearConfirmation());
        
        javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("Close");
        closeBtn.setStyle(getButtonStyle());
        closeBtn.setOnAction(e -> closeApplication());
        
        header.getChildren().addAll(spacer, clearBtn, closeBtn);
        return header;
    }
private HBox createFooter() {
    HBox footer = new HBox(12);
    footer.setPadding(new javafx.geometry.Insets(16, 20, 20, 20));
    footer.setAlignment(Pos.CENTER);
    footer.setStyle("-fx-background-color: rgba(15, 15, 20, 0.8); -fx-background-radius: 0 0 10 10;");

    // Drag icon (crossed arrows)
    Pane dragIcon = new Pane();
    dragIcon.setPrefSize(24, 24);
    dragIcon.setCursor(javafx.scene.Cursor.OPEN_HAND);

    Line arrow1 = new Line(4, 4, 20, 20);
    Line arrow2 = new Line(20, 4, 4, 20);

    for (Line arrow : new Line[]{arrow1, arrow2}) {
        arrow.setStroke(javafx.scene.paint.Color.web("rgba(196,181,224,0.7)"));
        arrow.setStrokeWidth(2);
        arrow.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
    }

    dragIcon.getChildren().addAll(arrow1, arrow2);

    dragIcon.setOnMousePressed(e -> {
        dragIcon.setCursor(javafx.scene.Cursor.CLOSED_HAND);
    });

    dragIcon.setOnMouseDragged(e -> {
        stage.setX(e.getScreenX());
        stage.setY(e.getScreenY());
    });

    dragIcon.setOnMouseReleased(e -> {
        dragIcon.setCursor(javafx.scene.Cursor.OPEN_HAND);
    });

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    javafx.scene.control.Label autoCloseLabel = new javafx.scene.control.Label("Auto-close:");
    autoCloseLabel.setStyle("-fx-text-fill: #C4B5E0; -fx-font-size: 12px;");

    Slider autoCloseSlider = new Slider(1, 30, settingsManager.getAutoCloseDelay());
    autoCloseSlider.setPrefWidth(150);
    autoCloseSlider.setStyle(getSliderStyle());

    javafx.scene.control.Label timeLabel = new javafx.scene.control.Label(settingsManager.getAutoCloseDelay() + "s");
    timeLabel.setStyle("-fx-text-fill: #C4B5E0; -fx-font-size: 12px; -fx-min-width: 30;");

    autoCloseSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
        int seconds = newVal.intValue();
        timeLabel.setText(seconds + "s");
        settingsManager.setAutoCloseDelay(seconds);
    });

    footer.getChildren().addAll(dragIcon, spacer, autoCloseLabel, autoCloseSlider, timeLabel);
    return footer;
}

    public void refreshItems() {
        itemsContainer.getChildren().clear();
        updateCurrentClipboard();
        
        for (ClipboardItem item : clipboardManager.getItems()) {
            HBox itemBox = createItemBox(item);
            itemsContainer.getChildren().add(itemBox);
        }
        
        if (clipboardManager.getItems().isEmpty()) {
            javafx.scene.control.Label emptyLabel = new javafx.scene.control.Label("No clipboard history yet\nCopy something to get started!");
            emptyLabel.setStyle("-fx-text-fill: rgba(196, 181, 224, 0.6); -fx-font-size: 14px; -fx-text-alignment: center;");
            emptyLabel.setAlignment(Pos.CENTER);
            emptyLabel.setPrefHeight(100);
            itemsContainer.getChildren().add(emptyLabel);
        }
    }

    private void updateCurrentClipboard() {
        try {
            java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                currentClipboardText = (String) clipboard.getData(DataFlavor.stringFlavor);
            }
        } catch (Exception e) {
            currentClipboardText = "";
        }
    }

    private HBox createItemBox(ClipboardItem item) {
        HBox box = new HBox(12);
        box.setPadding(new javafx.geometry.Insets(12));
        box.setAlignment(Pos.CENTER_LEFT);
        
        boolean isCurrentClipboard = item.getText().equals(currentClipboardText);
        
        String baseStyle = "-fx-background-color: rgba(30, 30, 40, 0.6); " +
                          "-fx-background-radius: 8px; " +
                          "-fx-border-radius: 8px; " +
                          "-fx-border-width: 2px;";
        
        if (isCurrentClipboard) {
            box.setStyle(baseStyle + "-fx-border-color: #4CAF50;"); // Green border for current clipboard
        } else {
            box.setStyle(baseStyle + "-fx-border-color: rgba(196, 181, 224, 0.2);");
        }

        String preview = item.getText();
        if (preview.length() > 80) {
            preview = preview.substring(0, 80) + "...";
        }
        preview = preview.replace("\n", " ").replace("\r", " ");
        
        javafx.scene.control.Label textLabel = new javafx.scene.control.Label(preview);
        textLabel.setStyle("-fx-text-fill: #E8E8E8; -fx-font-size: 13px;");
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(Double.MAX_VALUE);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox.setHgrow(textLabel, Priority.ALWAYS);
        
        javafx.scene.control.Button copyBtn = new javafx.scene.control.Button("C");
        copyBtn.setStyle(getSmallButtonStyle());
        copyBtn.setTooltip(new Tooltip("Copy"));
        copyBtn.setOnAction(e -> {
            copyToClipboard(item.getText());
            showToast("Copied to clipboard!");
            refreshItems(); // Update to show new current item
        });
        
        javafx.scene.control.Button deleteBtn = new javafx.scene.control.Button("D");
        deleteBtn.setStyle(getSmallButtonStyle());
        deleteBtn.setTooltip(new Tooltip("Delete"));
        deleteBtn.setOnAction(e -> {
            clipboardManager.removeItem(item);
            refreshItems();
        });
        
        HBox buttonBox = new HBox(8, copyBtn, deleteBtn);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        box.getChildren().addAll(textLabel, spacer, buttonBox);
        
        final String finalBaseStyle = baseStyle;
        box.setOnMouseEntered(e -> {
            if (isCurrentClipboard) {
                box.setStyle(finalBaseStyle + "-fx-border-color: #66BB6A; -fx-background-color: rgba(40, 40, 50, 0.8);");
            } else {
                box.setStyle(finalBaseStyle + "-fx-border-color: rgba(196, 181, 224, 0.4); -fx-background-color: rgba(40, 40, 50, 0.8);");
            }
        });
        
        box.setOnMouseExited(e -> {
            if (isCurrentClipboard) {
                box.setStyle(finalBaseStyle + "-fx-border-color: #4CAF50;");
            } else {
                box.setStyle(finalBaseStyle + "-fx-border-color: rgba(196, 181, 224, 0.2);");
            }
        });
        
        return box;
    }

    private void copyToClipboard(String text) {
        try {
            StringSelection selection = new StringSelection(text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            currentClipboardText = text;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showClearConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Clear History");
        alert.setHeaderText("Clear all clipboard history?");
        alert.setContentText("This action cannot be undone.");
        
        // Style the alert dialog with dark theme
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: rgba(15, 15, 20, 0.98); " +
                           "-fx-border-color: #C4B5E0; " +
                           "-fx-border-width: 2px; " +
                           "-fx-border-radius: 10px; " +
                           "-fx-background-radius: 10px;");
        
        // Style header
        javafx.scene.Node header = dialogPane.lookup(".header-panel");
        if (header != null) {
            header.setStyle("-fx-background-color: rgba(15, 15, 20, 0.95); -fx-background-radius: 10px 10px 0 0;");
        }
        
        // Style content
        dialogPane.lookup(".content.label").setStyle("-fx-text-fill: #E8E8E8; -fx-font-size: 13px;");
        dialogPane.lookup(".header-panel .label").setStyle("-fx-text-fill: #C4B5E0; -fx-font-size: 16px; -fx-font-weight: bold;");
        
        // Style buttons
        for (ButtonType buttonType : dialogPane.getButtonTypes()) {
            javafx.scene.control.Button button = (javafx.scene.control.Button) dialogPane.lookupButton(buttonType);
            if (button != null) {
                if (buttonType == ButtonType.OK) {
                    button.setStyle("-fx-background-color: rgba(244, 67, 54, 0.3); " +
                                  "-fx-text-fill: #E8E8E8; " +
                                  "-fx-background-radius: 6px; " +
                                  "-fx-padding: 8px 16px; " +
                                  "-fx-cursor: hand; " +
                                  "-fx-border-color: rgba(244, 67, 54, 0.5); " +
                                  "-fx-border-width: 1px;");
                } else {
                    button.setStyle(getButtonStyle());
                }
            }
        }
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                clipboardManager.clearAll();
                refreshItems();
                showToast("History cleared");
            }
        });
    }

    private void showToast(String message) {
        javafx.scene.control.Label toast = new javafx.scene.control.Label(message);
        toast.setStyle("-fx-background-color: rgba(15, 15, 20, 0.95); " +
                      "-fx-text-fill: #C4B5E0; " +
                      "-fx-padding: 12px 20px; " +
                      "-fx-background-radius: 8px; " +
                      "-fx-font-size: 13px; " +
                      "-fx-border-color: #C4B5E0; " +
                      "-fx-border-width: 1px; " +
                      "-fx-border-radius: 8px;");
        
        toastContainer.getChildren().clear();
        toastContainer.getChildren().add(toast);
        toastContainer.setAlignment(Pos.TOP_CENTER);
        StackPane.setMargin(toast, new javafx.geometry.Insets(20, 0, 0, 0));
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.millis(2000));
        fadeOut.setOnFinished(e -> toastContainer.getChildren().clear());
        
        fadeIn.setOnFinished(e -> fadeOut.play());
        fadeIn.play();
    }

    private void closeApplication() {
        clipboardManager.saveToFile();
        Platform.exit();
        System.exit(0);
    }

    private void setupEventHandlers() {
        root.setOnMouseEntered(e -> {
            if (autoCloseTimer != null) {
                autoCloseTimer.stop();
            }
        });
        
        root.setOnMouseExited(e -> startAutoCloseTimer());
    }

    private void setupResizeHandlers() {
        final int RESIZE_MARGIN = 8;
        
        root.setOnMouseMoved(e -> {
            double x = e.getX();
            double y = e.getY();
            double width = root.getWidth();
            double height = root.getHeight();
            
            resizeMode = ResizeMode.NONE;
            
            if (x < RESIZE_MARGIN && y < RESIZE_MARGIN) {
                resizeMode = ResizeMode.NW;
                root.setCursor(javafx.scene.Cursor.NW_RESIZE);
            } else if (x > width - RESIZE_MARGIN && y < RESIZE_MARGIN) {
                resizeMode = ResizeMode.NE;
                root.setCursor(javafx.scene.Cursor.NE_RESIZE);
            } else if (x < RESIZE_MARGIN && y > height - RESIZE_MARGIN) {
                resizeMode = ResizeMode.SW;
                root.setCursor(javafx.scene.Cursor.SW_RESIZE);
            } else if (x > width - RESIZE_MARGIN && y > height - RESIZE_MARGIN) {
                resizeMode = ResizeMode.SE;
                root.setCursor(javafx.scene.Cursor.SE_RESIZE);
            } else if (x < RESIZE_MARGIN) {
                resizeMode = ResizeMode.W;
                root.setCursor(javafx.scene.Cursor.W_RESIZE);
            } else if (x > width - RESIZE_MARGIN) {
                resizeMode = ResizeMode.E;
                root.setCursor(javafx.scene.Cursor.E_RESIZE);
            } else if (y < RESIZE_MARGIN) {
                resizeMode = ResizeMode.N;
                root.setCursor(javafx.scene.Cursor.N_RESIZE);
            } else if (y > height - RESIZE_MARGIN) {
                resizeMode = ResizeMode.S;
                root.setCursor(javafx.scene.Cursor.S_RESIZE);
            } else {
                root.setCursor(javafx.scene.Cursor.DEFAULT);
            }
        });
        
        root.setOnMousePressed(e -> {
            startX = e.getScreenX();
            startY = e.getScreenY();
        });
        
        root.setOnMouseDragged(e -> {
            if (resizeMode == ResizeMode.NONE) return;
            
            double deltaX = e.getScreenX() - startX;
            double deltaY = e.getScreenY() - startY;
            
            double newWidth = stage.getWidth();
            double newHeight = stage.getHeight();
            double newX = stage.getX();
            double newY = stage.getY();
            
            switch (resizeMode) {
                case E:
                    newWidth += deltaX;
                    break;
                case W:
                    newWidth -= deltaX;
                    newX += deltaX;
                    break;
                case S:
                    newHeight += deltaY;
                    break;
                case N:
                    newHeight -= deltaY;
                    newY += deltaY;
                    break;
                case SE:
                    newWidth += deltaX;
                    newHeight += deltaY;
                    break;
                case SW:
                    newWidth -= deltaX;
                    newX += deltaX;
                    newHeight += deltaY;
                    break;
                case NE:
                    newWidth += deltaX;
                    newHeight -= deltaY;
                    newY += deltaY;
                    break;
                case NW:
                    newWidth -= deltaX;
                    newX += deltaX;
                    newHeight -= deltaY;
                    newY += deltaY;
                    break;
            }
            
            newWidth = Math.max(MIN_WIDTH, Math.min(newWidth, MAX_WIDTH));
            newHeight = Math.max(MIN_HEIGHT, Math.min(newHeight, MAX_HEIGHT));
            
            if (newWidth >= MIN_WIDTH && newWidth <= MAX_WIDTH) {
                stage.setWidth(newWidth);
                if (resizeMode == ResizeMode.W || resizeMode == ResizeMode.NW || resizeMode == ResizeMode.SW) {
                    stage.setX(newX);
                }
            }
            
            if (newHeight >= MIN_HEIGHT && newHeight <= MAX_HEIGHT) {
                stage.setHeight(newHeight);
                if (resizeMode == ResizeMode.N || resizeMode == ResizeMode.NE || resizeMode == ResizeMode.NW) {
                    stage.setY(newY);
                }
            }
            
            startX = e.getScreenX();
            startY = e.getScreenY();
            
            currentWidth = stage.getWidth();
            currentHeight = stage.getHeight();
        });
        
        root.setOnMouseReleased(e -> {
            if (resizeMode != ResizeMode.NONE) {
                settingsManager.setModalWidth(currentWidth);
                settingsManager.setModalHeight(currentHeight);
            }
        });
    }

    private void startAutoCloseTimer() {
        if (autoCloseTimer != null) {
            autoCloseTimer.stop();
        }
        
        int closeDelay = settingsManager.getAutoCloseDelay();
        autoCloseTimer = new Timeline(new KeyFrame(Duration.seconds(closeDelay), e -> hide()));
        autoCloseTimer.play();
    }

    
    public void showNearMenu(FloatingMenu menu) {
    refreshItems();

    // Ensure stage has valid dimensions before showing
    if (stage.getWidth() == 0 || stage.getHeight() == 0) {
        stage.setWidth(currentWidth);
        stage.setHeight(currentHeight);
    }

    // Show the modal first so JavaFX can compute layout
    stage.show();

    // Run positioning after layout pass
    Platform.runLater(() -> {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double menuX = menu.getX();
        double menuY = menu.getY();

        double modalX, modalY;

        if (menuX < screenBounds.getWidth() / 2) {
            // Position to the right of the menu
            modalX = menuX + menu.getWidth() + 10;
        } else {
            // Position to the left of the menu using known width
            modalX = menuX - currentWidth - 10;
        }

        // Prevent it from going off-screen vertically
        modalY = Math.max(10, Math.min(menuY, screenBounds.getMaxY() - currentHeight - 10));

        stage.setX(modalX);
        stage.setY(modalY);
        startAutoCloseTimer();

        if (floatingMenu != null) {
            floatingMenu.setModalOpen(true);
        }
    });
}


    public void repositionNearMenu(FloatingMenu menu) {
        if (menu == null || !stage.isShowing()) return;

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double menuX = menu.getX();
        double menuY = menu.getY();

        double modalX, modalY;

        if (menuX < screenBounds.getWidth() / 2) {
            // Keep to the right of the menu
            modalX = menuX + menu.getWidth() + 10;
        } else {
            // Keep to the left using tracked modal width
            modalX = menuX - currentWidth - 10;
        }

        modalY = Math.max(10, Math.min(menuY, screenBounds.getMaxY() - currentHeight - 10));

        // Use runLater to avoid layout timing glitches
        Platform.runLater(() -> {
            stage.setX(modalX);
            stage.setY(modalY);
        });
    }



    public void hide() {
        stage.hide();
        if (autoCloseTimer != null) {
            autoCloseTimer.stop();
        }
        // Notify floating menu that modal is closed
        if (floatingMenu != null) {
            floatingMenu.setModalOpen(false);
        }
    }
    
    public boolean isShowing() {
        return stage.isShowing();
    }

    private String getGlassmorphicStyle() {
        return "-fx-background-color: rgba(10, 10, 15, 0.85); " +
               "-fx-background-radius: 12px; " +
               "-fx-border-color: rgba(196, 181, 224, 0.3); " +
               "-fx-border-radius: 12px; " +
               "-fx-border-width: 1px; " +
               "-fx-background-insets: 0;";
    }

    private String getButtonStyle() {
        return "-fx-background-color: rgba(196, 181, 224, 0.2); " +
               "-fx-text-fill: #C4B5E0; " +
               "-fx-background-radius: 6px; " +
               "-fx-padding: 8px 16px; " +
               "-fx-cursor: hand; " +
               "-fx-font-size: 12px; " +
               "-fx-border-color: rgba(196, 181, 224, 0.3); " +
               "-fx-border-width: 1px;";
    }

    private String getSmallButtonStyle() {
        return "-fx-background-color: rgba(196, 181, 224, 0.15); " +
               "-fx-text-fill: #C4B5E0; " +
               "-fx-background-radius: 4px; " +
               "-fx-padding: 6px 10px; " +
               "-fx-cursor: hand; " +
               "-fx-font-size: 11px; " +
               "-fx-font-weight: bold; " +
               "-fx-min-width: 30px; " +
               "-fx-border-color: rgba(196, 181, 224, 0.2); " +
               "-fx-border-width: 1px;";
    }

    private String getSliderStyle() {
        return "-fx-background-color: rgba(30, 30, 40, 0.8); " +
               "-fx-background-radius: 3px; " +
               "-fx-control-inner-background: rgba(196, 181, 224, 0.3);";
    }
}