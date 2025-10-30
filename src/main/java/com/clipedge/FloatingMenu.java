package com.clipedge;

import javafx.animation.*;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class FloatingMenu {
    private Stage stage;
    private StackPane root;
    private Circle backgroundCircle;
    private Circle borderCircle;
    private StackPane logoContainer;
    private SettingsManager settingsManager;
    private Runnable onClickHandler;
    private Runnable onCloseHandler;
    private double xOffset = 0;
    private double yOffset = 0;
    private boolean isDragging = false;
    private boolean isModalOpen = false;
    private static final double MENU_SIZE = 60;
    private static final double IDLE_OPACITY = 0.7;
    private static final double HOVER_OPACITY = 1.0;

    public FloatingMenu(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
        initUI();
    }

    private void initUI() {
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        root = new StackPane();
        root.setStyle("-fx-background-color: transparent;");

        backgroundCircle = new Circle(MENU_SIZE / 2);
        backgroundCircle.setFill(Color.rgb(10, 10, 15, 0.9));
        backgroundCircle.setEffect(new DropShadow(20, Color.rgb(196, 181, 224, 0.3)));

        borderCircle = new Circle(MENU_SIZE / 2);
        borderCircle.setFill(Color.TRANSPARENT);
        borderCircle.setStroke(Color.rgb(196, 181, 224, 0.4));
        borderCircle.setStrokeWidth(2);

        // Create stylized E logo
        logoContainer = createStylizedE();

        root.getChildren().addAll(backgroundCircle, borderCircle, logoContainer);
        updateIconForState();

        Scene scene = new Scene(root, MENU_SIZE, MENU_SIZE);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX(screenBounds.getMaxX() - MENU_SIZE - 20);
        stage.setY(screenBounds.getHeight() / 2 - MENU_SIZE / 2);
        stage.setOpacity(IDLE_OPACITY);

        setupEventHandlers();
    }

    private StackPane createStylizedE() {
        StackPane container = new StackPane();
        Text eText = new Text("E");

        // Use a clean, elegant font. If Playwrite ES is not installed locally,
        // fallback will still look refined.
        eText.setFont(Font.font("Playwrite ES", FontWeight.NORMAL, FontPosture.REGULAR, 28));
        eText.setFill(Color.rgb(196, 181, 224, 0.9));

        eText.setEffect(new DropShadow(10, Color.rgb(196, 181, 224, 0.4)));

        container.getChildren().add(eText);
        return container;
    }

    private void setupEventHandlers() {
        root.setOnMouseEntered(e -> {
            stage.setOpacity(HOVER_OPACITY);
            ScaleTransition st = new ScaleTransition(Duration.millis(200), root);
            st.setToX(1.15);
            st.setToY(1.15);
            st.play();

            borderCircle.setStroke(Color.rgb(196, 181, 224, 0.8));
            borderCircle.setStrokeWidth(3);
        });

        root.setOnMouseExited(e -> {
            if (!isDragging) {
                stage.setOpacity(IDLE_OPACITY);
                ScaleTransition st = new ScaleTransition(Duration.millis(200), root);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();

                borderCircle.setStroke(Color.rgb(196, 181, 224, 0.4));
                borderCircle.setStrokeWidth(2);
            }
        });

        root.setOnMouseClicked(e -> {
            if (!isDragging) {
                if (isModalOpen && onCloseHandler != null) {
                    onCloseHandler.run();
                } else if (!isModalOpen && onClickHandler != null) {
                    onClickHandler.run();
                }
                playAnimation();
            }
        });

        root.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
            isDragging = false;
        });

        root.setOnMouseDragged(e -> {
            isDragging = true;
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });

        root.setOnMouseReleased(e -> {
            if (isDragging) {
                snapToEdge();
            }
            isDragging = false;
        });
    }

    private void snapToEdge() {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double x = stage.getX();
        double y = stage.getY();

        double distLeft = x;
        double distRight = screenBounds.getMaxX() - (x + MENU_SIZE);
        double distTop = y;
        double distBottom = screenBounds.getMaxY() - (y + MENU_SIZE);

        double minDist = Math.min(Math.min(distLeft, distRight), Math.min(distTop, distBottom));

        javafx.beans.property.DoubleProperty xProperty = new javafx.beans.property.SimpleDoubleProperty(stage.getX());
        javafx.beans.property.DoubleProperty yProperty = new javafx.beans.property.SimpleDoubleProperty(stage.getY());

        xProperty.addListener((obs, oldVal, newVal) -> stage.setX(newVal.doubleValue()));
        yProperty.addListener((obs, oldVal, newVal) -> stage.setY(newVal.doubleValue()));

        Timeline timeline = new Timeline();

        if (minDist == distLeft) {
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(300),
                    new KeyValue(xProperty, 20)));
        } else if (minDist == distRight) {
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(300),
                    new KeyValue(xProperty, screenBounds.getMaxX() - MENU_SIZE - 20)));
        } else if (minDist == distTop) {
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(300),
                    new KeyValue(yProperty, 20)));
        } else {
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(300),
                    new KeyValue(yProperty, screenBounds.getMaxY() - MENU_SIZE - 20)));
        }
        timeline.play();
    }

    public void playAnimation() {
        ScaleTransition st1 = new ScaleTransition(Duration.millis(100), root);
        st1.setToX(0.9);
        st1.setToY(0.9);

        ScaleTransition st2 = new ScaleTransition(Duration.millis(100), root);
        st2.setToX(1.0);
        st2.setToY(1.0);

        SequentialTransition sequence = new SequentialTransition(st1, st2);
        sequence.play();
    }

    public void setOnClick(Runnable handler) {
        this.onClickHandler = handler;
    }

    public void setOnClose(Runnable handler) {
        this.onCloseHandler = handler;
    }

    public void setModalOpen(boolean open) {
        this.isModalOpen = open;
        updateIconForState();
    }

    private void updateIconForState() {
        root.getChildren().clear();
        root.getChildren().addAll(backgroundCircle, borderCircle);

        if (isModalOpen) {
            root.getChildren().add(createXIcon());
        } else {
            root.getChildren().add(logoContainer);
        }
    }

    private StackPane createXIcon() {
        StackPane xPane = new StackPane();

        // Use Region objects with fully rounded ends (circular)
        Region line1 = new Region();
        line1.setPrefSize(20, 3);
        line1.setStyle("-fx-background-color: rgba(196, 181, 224, 0.9); -fx-background-radius: 1.5;");
        line1.setRotate(45);

        Region line2 = new Region();
        line2.setPrefSize(20, 3);
        line2.setStyle("-fx-background-color: rgba(196, 181, 224, 0.9); -fx-background-radius: 1.5;");
        line2.setRotate(-45);

        line1.setEffect(new DropShadow(10, Color.rgb(196, 181, 224, 0.4)));
        line2.setEffect(new DropShadow(10, Color.rgb(196, 181, 224, 0.4)));

        xPane.getChildren().addAll(line1, line2);

        return xPane;
    }

    public void show() {
        stage.show();
    }

    public double getX() {
        return stage.getX();
    }

    public double getY() {
        return stage.getY();
    }

    public double getWidth() {
        return MENU_SIZE;
    }

    public double getHeight() {
        return MENU_SIZE;
    }
}