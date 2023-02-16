package alanisia.m3u8downloader;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collections;
import java.util.Enumeration;

public class App extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private final Input mInput = new Input();
    private final M3U8Handler mM3U8Handler = new M3U8Handler(mInput);

    private Stage mPrimaryStage;
    private final GridPane gridPane = new GridPane();
    private TextField mTfHostUrl;
    private Button mBtnPickFile, mBtnPickDir, mBtnDownload;
    private Label mLM3u8FilePath, mLSavePath, mLDownloadProgress;
    private ProgressBar mPbDownloadProgress;

    @Override
    public void start(Stage primaryStage) {
        mPrimaryStage = primaryStage;

        Scene scene = new Scene(initView());
        primaryStage.setScene(scene);
        primaryStage.setTitle("m3u8 Downloader");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private Group initView() {
        mTfHostUrl = new TextField();
        mTfHostUrl.setMaxWidth(Double.MAX_VALUE);

        mBtnPickFile = new Button("Pick file");
        mBtnPickDir = new Button("Pick directory");
        mLM3u8FilePath = new Label("");
        mLSavePath = new Label("");
        mPbDownloadProgress = new ProgressBar();
        mLDownloadProgress = new Label("0%");
        mLDownloadProgress.setVisible(false);

        gridPane.setMinWidth(600.0f);
        gridPane.setPadding(new Insets(4));
        gridPane.setHgap(4.0f);
        gridPane.setVgap(4.0f);

        gridPane.add(new Label("Host URL: "), 0, 0);
        GridPane.setHgrow(mTfHostUrl, Priority.ALWAYS);
        gridPane.add(mTfHostUrl, 1, 0);

        gridPane.add(new Label("m3u8 Path: "), 0, 1);
        HBox m3u8PathHBox = new HBox();
        m3u8PathHBox.getChildren().add(mBtnPickFile);
        m3u8PathHBox.getChildren().add(mLM3u8FilePath);
        gridPane.add(m3u8PathHBox, 1, 1);

        gridPane.add(new Label("Save Path: "), 0, 2);
        HBox savePathHBox = new HBox();
        savePathHBox.getChildren().add(mBtnPickDir);
        savePathHBox.getChildren().add(mLSavePath);
        gridPane.add(savePathHBox, 1, 2);

        mBtnDownload = new Button("Download");
        gridPane.add(mBtnDownload, 0, 3);
        mPbDownloadProgress.setVisible(false);
        gridPane.add(mPbDownloadProgress, 1, 3);
        gridPane.add(mLDownloadProgress, 2, 3);

        addListener();
        return new Group(gridPane);
    }

    private void addListener() {
        mBtnPickFile.setOnMouseClicked(e -> {
            File m3u8File = new FileChooser().showOpenDialog(mPrimaryStage);
            if (m3u8File != null) {
                mInput.setM3u8File(m3u8File);
                mLM3u8FilePath.setText(m3u8File.getPath());
                mLM3u8FilePath.setTextFill(Color.BLACK);
            }
        });
        mBtnPickDir.setOnMouseClicked(e -> {
            File directory = new DirectoryChooser().showDialog(mPrimaryStage);
            if (directory != null) {
                String savePath = directory.getPath();
                mInput.setSavePath(savePath);
                mLSavePath.setText(savePath);
                mLSavePath.setTextFill(Color.BLACK);
            }
        });
        mBtnDownload.setOnMouseClicked(e -> {
            mInput.setHostUrl(mTfHostUrl.getText());
            if (mInput.getHostUrl() == null || mInput.getHostUrl().isEmpty()) {
                showErrorAlert("The host URL is empty or invalid");
            } else if (mInput.getM3u8File() == null) {
                showErrorAlert("The host URL is empty or invalid");
            } else if (mInput.getSavePath() == null || mInput.getSavePath().isEmpty()) {
                showErrorAlert("Please select a directory to save file");
            } else {
                mPbDownloadProgress.setVisible(true);
                mLDownloadProgress.setVisible(true);
                mM3U8Handler.setInput(mInput).restart();
                mM3U8Handler.setOnRunning(event -> {
                    double progress = event.getSource().getProgress();
                    mLDownloadProgress.setText(progress * 100 + "%");
                });
                mM3U8Handler.setOnSucceeded(event -> {
                    LOGGER.debug("Success");
                    Enumeration<InputStream> enumeration = Collections.enumeration(mM3U8Handler.getInputStreams());
                    try (SequenceInputStream sequenceInputStream = new SequenceInputStream(enumeration);
                         FileOutputStream fileOutputStream = new FileOutputStream(String.format("%s/out_%d.mp4",
                                 mInput.getSavePath(), System.currentTimeMillis()))) {
                        fileOutputStream.write(sequenceInputStream.readAllBytes());
                    } catch (IOException exception) {
                        LOGGER.error(exception.getMessage());
                    }
                });
            }
        });
    }

    private void showErrorAlert(String contentText) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setResizable(false);
        alert.setContentText(contentText);
        alert.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
