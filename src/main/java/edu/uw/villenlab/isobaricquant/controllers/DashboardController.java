package edu.uw.villenlab.isobaricquant.controllers;

import edu.uw.villenlab.isobaricquant.IsobaricQuant;
import edu.uw.villenlab.isobaricquant.Quantification;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.xml.sax.SAXException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;

public class DashboardController implements Initializable {

    private String configPath;
    private String hitsPath;
    private String mzPath;

    private String pepPath;
    private String pepExtraPath;
    private String labelPath;
    private String fragmentPath;

    private String projectConfigFilePath;

    private String saveDir;

    private final FileChooser fc = new FileChooser();
    private final DirectoryChooser dc = new DirectoryChooser();
    private String lastDir;

    @FXML
    private TextArea logTextArea;
    @FXML
    private TextField configPathInput;
    @FXML
    private TextField configHitsInput;
    @FXML
    private TextField configMzInput;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Button viewResultsBtn;
    @FXML
    private MenuItem saveResultsMenuItem;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        viewResultsBtn.setDisable(true);
        //saveResultsMenuItem.setDisable(true);
    }

    public void enableResultComponents() {
        viewResultsBtn.setDisable(false);
        saveResultsMenuItem.setDisable(false);
    }

    public void updateResultPaths(String pepPath, String pepExtraPath, String labelPath, String fragmentPath) {
        this.pepPath = pepPath;
        this.pepExtraPath = pepExtraPath;
        this.labelPath = labelPath;
        this.fragmentPath = fragmentPath;
    }

    public class QuantifyThread extends Thread {

        private final Quantification isoQuant;
        private final DashboardController controller;

        public QuantifyThread(Quantification isoQuant, DashboardController controller) {
            this.isoQuant = isoQuant;
            this.controller = controller;
        }

        @Override
        public void run() {
            try {
                isoQuant.quantify(this.controller);
            } catch (ParserConfigurationException | SAXException | IOException | DataFormatException | MzMLUnmarshallerException ex) {
                Logger.getLogger(IsobaricQuant.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @FXML
    void openProjectChooser(ActionEvent event) {
        File f = fc.showOpenDialog(null);
        if (f != null) {
            projectConfigFilePath = f.getAbsolutePath();
            fc.setInitialDirectory(f.getParentFile());
            String projectDir = f.getParentFile().getAbsolutePath();
            try {
                String content = new String(Files.readAllBytes(Paths.get(projectConfigFilePath)));
                JSONObject obj = new JSONObject(content);
                configPath = projectDir + "/" + obj.getString("configFile");
                hitsPath = projectDir + "/" + obj.getString("hitsFile");
                mzPath = projectDir + "/" + obj.getString("mzFile");
                pepPath = projectDir + "/" + "viewerisopep.csv";
                pepExtraPath = projectDir + "/" + "viewerisopep_extra.csv";
                labelPath = projectDir + "/" + "viewerisolab.csv";
                fragmentPath = projectDir + "/" + "viewerisofrag.csv";
                logTextArea.appendText("Loading config file \n");
                openResultsView(event);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void openResultsView(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader();
            URL xmlUrl =  this.getClass().getClassLoader().getResource("views/PeptideViewer.fxml");
            //URL xmlUrl = IsobaricQuant.class.getResource("/isobaricquant/views/PeptideViewer.fxml");
            loader.setLocation(xmlUrl);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("IsobaricQuant");
            Scene pepScene = new Scene(root);
            pepScene.getStylesheets().add(this.getClass().getClassLoader().getResource("views/peptide.css").toExternalForm());
            stage.setScene(pepScene);
            PeptideController controller = loader.getController();

            Service loadControllerData = new Service() {
                @Override
                protected Task createTask() {
                    return new Task() {
                        @Override
                        protected Object call() throws Exception {
                            disableInputFields(true);
                            logTextArea.appendText("Loading data \n");
                            controller.initData(hitsPath, mzPath, pepPath, pepExtraPath, labelPath, fragmentPath, configPath);
                            return null;
                        }
                    };
                }
            };

            loadControllerData.setOnSucceeded(e -> {
                logTextArea.appendText("Data loaded succesfully \n");
                stage.show();
                resetState();
            });

            loadControllerData.setOnFailed(e -> {
                System.err.println("The task failed with the following exception:");
                loadControllerData.getException().printStackTrace(System.err);
                resetState();
            });

            progressBar.progressProperty().bind(loadControllerData.progressProperty());

            loadControllerData.start();
        } catch (IOException ex) {
            Logger.getLogger(DashboardController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void resetState() {
        progressBar.progressProperty().unbind();
        progressBar.setProgress(1);
        disableInputFields(false);
    }

    void disableInputFields(Boolean disable) {
        configPathInput.setDisable(disable);
        configHitsInput.setDisable(disable);
        configMzInput.setDisable(disable);
    }

    @FXML
    void fileChooser(ActionEvent event) {
        Button btn = (Button) event.getSource();
        File f = fc.showOpenDialog(null);

        if (f != null) {
            System.out.println(f.getAbsolutePath());
            switch (btn.getId()) {

                case "configBrowse":
                    configPath = f.getAbsolutePath();
                    configPathInput.setText(configPath);
                    fc.setInitialDirectory(f.getParentFile());
                    logTextArea.appendText("Configuration file selected: " + configPath + "\n");
                    break;
                case "hitsBrowse":
                    hitsPath = f.getAbsolutePath();
                    configHitsInput.setText(hitsPath);
                    fc.setInitialDirectory(f.getParentFile());
                    logTextArea.appendText("Hits file selected: " + hitsPath + "\n");
                    break;
                case "mzBrowse":
                    mzPath = f.getAbsolutePath();
                    configMzInput.setText(mzPath);
                    fc.setInitialDirectory(f.getParentFile());
                    logTextArea.appendText("Spectometry file selected: " + mzPath + "\n");
                    break;
            }
        }
    }

    @FXML
    public void saveResultsLocationChooser(ActionEvent event) {
        dc.setTitle("Save quantification results");
        File location = dc.showDialog(null);
        saveDir = location.getAbsolutePath();
        System.out.println(location.getAbsoluteFile());

        String output = new File(IsobaricQuant.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
        String outputDir = output + "/output/";

        File isoFragFile = new File(outputDir + "viewerisofrag.csv");
        File isoLabFile = new File(outputDir + "viewerisolab.csv");
        File isoPepFile = new File(outputDir + "viewerisopep.csv");
        File isoPepExtraFile = new File(outputDir + "viewerisopep_extra.csv");
        File hitsFile = new File(hitsPath);
        File mzFile = new File(mzPath);
        File configPepFile = new File(configPath);

        if (isoFragFile.exists() && isoLabFile.exists() && isoPepFile.exists() && isoPepExtraFile.exists()) {
            FileWriter configFile = null;
            try {
                //Move files
                FileUtils.copyFile(isoFragFile, new File(saveDir + "/" + "viewerisofrag.csv"));
                FileUtils.copyFile(isoLabFile, new File(saveDir + "/" + "viewerisolab.csv"));
                FileUtils.copyFile(isoPepFile, new File(saveDir + "/" + "viewerisopep.csv"));
                FileUtils.copyFile(isoPepExtraFile, new File(saveDir + "/" + "viewerisopep_extra.csv"));
                //Move hits, mzFile and configFile
                FileUtils.copyFile(hitsFile, new File(saveDir + "/" + hitsFile.getName()));
                FileUtils.copyFile(mzFile, new File(saveDir + "/" + mzFile.getName()));
                FileUtils.copyFile(configPepFile, new File(saveDir + "/" + configPepFile.getName()));
                //Create json project file
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("hitsFile", hitsFile.getName());
                jsonObject.put("mzFile", mzFile.getName());
                jsonObject.put("configFile", configPepFile.getName());
                configFile = new FileWriter(saveDir + "/" + "project.json");
                configFile.write(jsonObject.toString());
                configFile.close();
            } catch (IOException ex) {
                Logger.getLogger(DashboardController.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    configFile.close();
                } catch (IOException ex) {
                    Logger.getLogger(DashboardController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public void reportAndLogException(String logMessage) {
        Platform.runLater(() -> {
            logTextArea.appendText(logMessage + "\n");
        });
    }

    public void updateProgress(double progress) {
        Platform.runLater(() -> {
            progressBar.setProgress(progress / 100);
        });
    }

    @FXML
    public void guiQuantify() {
        Quantification isoQuant = new Quantification();
        isoQuant.setQuantId(33);
        isoQuant.setMzMLFilePath(mzPath);
        isoQuant.setPeptideFilePath(hitsPath);
        isoQuant.setToken("viewer");
        String output = new File(IsobaricQuant.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
        isoQuant.setOutputDirectory(output + "/output/");

        try {
            isoQuant.readConfFile(configPath, "");
        } catch (IOException ex) {
            Logger.getLogger(IsobaricQuant.class.getName()).log(Level.SEVERE, null, ex);
        }
        logTextArea.appendText("Starting quantification \n");

        QuantifyThread t = new QuantifyThread(isoQuant, this);
        t.start();
    }
}
