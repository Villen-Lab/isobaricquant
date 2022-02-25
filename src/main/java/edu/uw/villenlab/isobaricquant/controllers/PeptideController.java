package edu.uw.villenlab.isobaricquant.controllers;

import edu.uw.villenlab.isobaricquant.models.PeptideModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.controlsfx.control.tableview2.FilteredTableColumn;
import org.controlsfx.control.tableview2.FilteredTableView;
import org.controlsfx.control.tableview2.cell.TextField2TableCell;
import org.controlsfx.control.tableview2.filter.popupfilter.PopupFilter;
import org.controlsfx.control.tableview2.filter.popupfilter.PopupNumberFilter;
import org.controlsfx.control.tableview2.filter.popupfilter.PopupStringFilter;
import villeninputs.Peptide;
import villeninputs.input.FactoryProvider;
import villeninputs.input.peptidesequence.PeptideFile;
import villeninputs.input.peptidesequence.PeptideFileFactory;
import villeninputs.input.spectroscopy.MzFile;
import villeninputs.input.spectroscopy.MzFileFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PeptideController implements Initializable {

    @FXML
    private FilteredTableView<PeptideModel> peptideTable;

    private final ObservableList<PeptideModel> dataList
            = FXCollections.observableArrayList();

    private HashMap<String, String> columNameTypes;

    private String peptideFile = "";
    private String peptideExtraFile = "";

    private String pepPath;
    private String pepExtraPath;
    private String labelPath;
    private String fragmentPath;

    private final String[] columnNames = new String[]{
        "QuantID",
        "PeptideID",
        "SearchID",
        "ScanNumber",
        "reference",
        "sequence",
        "mz",
        "charge",
        "Noise",
        "Score",
        "precSignal",
        "IntensityScore",
        "topXPeptidePeaksRatio",
        "topXPeptideIntensityScore",
        "topXIntensityFromTotalScore",
        "topXPeptideIntensityFromTotalScore",
        "topPeakIntensityScore",
        "isTopPeakFromPeptide",
        "isTopPeakFromPeptideNeutralLoss",
        "topPeakIntensityTopXScore",
        "topPeakMass",
        "msnTotalSignal",
        "precTPIntRatio",
        "precRepIntRatio",
        "precTPNumRatio",
        "precTotalSignal",
        "totalSignalSPSWind",
        "scanLevel",
        "ms1ScanNumber",
        "ms2ScanNumber",
        "ms3ScanNumber",
        "ms1RetentionTime",
        "ms2RetentionTime",
        "ms3RetentionTime",
        "SPSMasses"
    };

    private void createColumns() {

        columNameTypes = new HashMap<>();
        columNameTypes.put("QuantID", "Integer");
        columNameTypes.put("PeptideID", "Integer");
        columNameTypes.put("SearchID", "Integer");
        columNameTypes.put("ScanNumber", "Integer");
        columNameTypes.put("reference", "String");
        columNameTypes.put("sequence", "String");
        columNameTypes.put("mz", "Double");
        columNameTypes.put("charge", "Integer");
        columNameTypes.put("Noise", "Double");
        columNameTypes.put("Score", "Double");
        columNameTypes.put("precSignal", "Double");
        columNameTypes.put("IntensityScore", "Double");
        columNameTypes.put("topXPeptidePeaksRatio", "Double");
        columNameTypes.put("topXPeptideIntensityScore", "Double");
        columNameTypes.put("topXIntensityFromTotalScore", "Double");
        columNameTypes.put("topXPeptideIntensityFromTotalScore", "Double");
        columNameTypes.put("topPeakIntensityScore", "Double");
        columNameTypes.put("isTopPeakFromPeptide", "Boolean");
        columNameTypes.put("isTopPeakFromPeptideNeutralLoss", "Boolean");
        columNameTypes.put("topPeakIntensityTopXScore", "Double");
        columNameTypes.put("topPeakMass", "Double");
        columNameTypes.put("msnTotalSignal", "Double");
        columNameTypes.put("precTPIntRatio", "String");
        columNameTypes.put("precRepIntRatio", "String");
        columNameTypes.put("precTPNumRatio", "String");
        columNameTypes.put("precTotalSignal", "Double");
        columNameTypes.put("totalSignalSPSWind", "Double");
        columNameTypes.put("scanLevel", "Integer");
        columNameTypes.put("ms1ScanNumber", "Integer");
        columNameTypes.put("ms2ScanNumber", "Integer");
        columNameTypes.put("ms3ScanNumber", "Integer");
        columNameTypes.put("ms1RetentionTime", "Double");
        columNameTypes.put("ms2RetentionTime", "Double");
        columNameTypes.put("ms3RetentionTime", "Double");
        columNameTypes.put("SPSMasses", "String");

        for (String columnName : columnNames) {
            createColumn(columnName, columNameTypes.get(columnName));
        }
    }

    private void createColumn(String columName, String type) {

        switch (type) {
            case "Integer":
                FilteredTableColumn<PeptideModel, Integer> fIntTableCol = new FilteredTableColumn<>(columName);
                fIntTableCol.setCellValueFactory(new PropertyValueFactory<>(columName));
                fIntTableCol.setCellFactory(TextField2TableCell.forTableColumn(new StringConverter<Integer>() {
                    @Override
                    public String toString(Integer t) {
                        return String.valueOf(t);
                    }

                    @Override
                    public Integer fromString(String string) {
                        return Integer.parseInt(string);
                    }
                }));
                peptideTable.getColumns().add(fIntTableCol);
                PopupFilter<PeptideModel, Integer> popupFilter = new PopupNumberFilter<>(fIntTableCol);
                fIntTableCol.setOnFilterAction(e -> popupFilter.showPopup());
                break;
            case "Double":
                FilteredTableColumn<PeptideModel, Double> fDoubleTableCol = new FilteredTableColumn<>(columName);
                fDoubleTableCol.setCellValueFactory(new PropertyValueFactory<>(columName));

                fDoubleTableCol.setCellFactory(TextField2TableCell.forTableColumn(new StringConverter<Double>() {
                    @Override
                    public String toString(Double t) {
                        return String.valueOf(t);
                    }

                    @Override
                    public Double fromString(String string) {
                        return Double.parseDouble(string);
                    }
                }));

                peptideTable.getColumns().add(fDoubleTableCol);
                PopupFilter<PeptideModel, Double> popupDoubleFilter = new PopupNumberFilter<>(fDoubleTableCol);
                fDoubleTableCol.setOnFilterAction(e -> popupDoubleFilter.showPopup());
                break;
            case "Boolean":
                FilteredTableColumn<PeptideModel, Boolean> fBooleanTableCol = new FilteredTableColumn<>(columName);
                fBooleanTableCol.setCellValueFactory(new PropertyValueFactory<>(columName));
                fBooleanTableCol.setCellFactory(TextField2TableCell.forTableColumn(new StringConverter<Boolean>() {
                    @Override
                    public String toString(Boolean t) {
                        return String.valueOf(t);
                    }

                    @Override
                    public Boolean fromString(String string) {
                        return Boolean.parseBoolean(string);
                    }
                }));
                peptideTable.getColumns().add(fBooleanTableCol);
                PopupFilter<PeptideModel, Boolean> popupBooleanFilter = new PopupStringFilter<>(fBooleanTableCol);
                fBooleanTableCol.setOnFilterAction(e -> popupBooleanFilter.showPopup());
                break;
            default:
                FilteredTableColumn<PeptideModel, String> fStringTableCol = new FilteredTableColumn<>(columName);
                fStringTableCol.setCellValueFactory(new PropertyValueFactory<>(columName));
                fStringTableCol.setCellFactory(TextField2TableCell.forTableColumn());
                peptideTable.getColumns().add(fStringTableCol);
                PopupFilter<PeptideModel, String> popupStringFilter = new PopupStringFilter<>(fStringTableCol);
                fStringTableCol.setOnFilterAction(e -> popupStringFilter.showPopup());
                break;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    private MzFile mzFile;

    public void initData(String hitsPath, String mzFilePath, String pepPath, String pepExtraPath, String labelPath, String fragmentPath, String configPath) {
        this.pepPath = pepPath;
        this.pepExtraPath = pepExtraPath;
        this.labelPath = labelPath;
        this.fragmentPath = fragmentPath;

        if (pepPath != null && !"".equals(pepPath)) {
            peptideFile = pepPath;
        }
        if (pepExtraPath != null && !"".equals(pepExtraPath)) {
            peptideExtraFile = pepExtraPath;
        }

        parseHits(hitsPath);
        readCSV();
        peptideTable.setItems(dataList);
        addButtonToTable();
        createColumns();

        MzFileFactory mzFileFactory = (MzFileFactory) FactoryProvider.getFactory("MzFile");
        mzFile = mzFileFactory.create(mzFilePath);
    }

    private HashMap<Integer, Peptide> peptidesMap;

    private void parseHits(String hitsPath) {
        peptidesMap = new HashMap<>();
        PeptideFileFactory peptideFileFactory = (PeptideFileFactory) FactoryProvider.getFactory("PeptideFile");
        PeptideFile peptideFile = peptideFileFactory.create(hitsPath);
        List<Peptide> peptideList = peptideFile.getPeptides();

        peptideList.forEach((pep) -> {
            peptidesMap.put(pep.getPeptideID(), pep);
        });
    }

    private void addButtonToTable() {
        TableColumn<PeptideModel, Void> colBtn = new TableColumn("Labels");

        Callback<TableColumn<PeptideModel, Void>, TableCell<PeptideModel, Void>> cellFactory = new Callback<TableColumn<PeptideModel, Void>, TableCell<PeptideModel, Void>>() {
            @Override
            public TableCell<PeptideModel, Void> call(final TableColumn<PeptideModel, Void> param) {
                final TableCell<PeptideModel, Void> cell = new TableCell<PeptideModel, Void>() {

                    private final Button btn = new Button();

                    {
                        btn.getStyleClass().add("icon-button");
                        btn.setPickOnBounds(true);
                        /*Region icon = new Region();
                        icon.getStyleClass().add("icon");
                        btn.setGraphic(icon);*/
                        btn.setOnAction((ActionEvent event) -> {
                            PeptideModel data = getTableView().getItems().get(getIndex());
                            System.out.println("selectedData: " + data.getPeptideID());

                            try {
                                FXMLLoader loader = new FXMLLoader();
                                URL xmlUrl =  this.getClass().getClassLoader().getResource("views/IsobaricViewer.fxml");
                                //URL xmlUrl = IsobaricQuant.class.getResource("/isobaricquant/views/IsobaricViewer.fxml");
                                loader.setLocation(xmlUrl);
                                Parent root = loader.load();
                                Stage stage = new Stage();
                                stage.setTitle("IsobaricQuant");
                                stage.setScene(new Scene(root));
                                IsobaricController controller = loader.getController();
                                controller.findFragments(data, mzFile, labelPath, fragmentPath);
                                stage.show();
                            } catch (IOException ex) {
                                Logger.getLogger(DashboardController.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };

        colBtn.setCellFactory(cellFactory);

        peptideTable.getColumns().add(colBtn);

    }

    private void readCSV() {
        String FieldDelimiter = ",";

        BufferedReader peptideBr;
        BufferedReader peptideExtraBr;

        try {
            peptideBr = new BufferedReader(new FileReader(peptideFile));
            peptideExtraBr = new BufferedReader(new FileReader(peptideExtraFile));

            String peptideLine;
            String peptideExtraLine;

            //Skip header line
            peptideLine = peptideBr.readLine();
            peptideExtraLine = peptideExtraBr.readLine();

            while ((peptideLine = peptideBr.readLine()) != null && (peptideExtraLine = peptideExtraBr.readLine()) != null) {
                String[] peptideFields = peptideLine.split(FieldDelimiter, -1);
                String[] peptideExtraFields = peptideExtraLine.split(FieldDelimiter, -1);
                int peptideID = Integer.valueOf(peptideFields[1]);
                Peptide pep = peptidesMap.get(peptideID);

                PeptideModel record = new PeptideModel(
                        //pep
                        Integer.valueOf(peptideFields[0]),
                        peptideID,
                        Integer.valueOf(peptideFields[2]),
                        Integer.valueOf(peptideFields[3]),
                        Double.valueOf(peptideFields[4]),
                        Double.valueOf(peptideFields[5]),
                        //pepExtra
                        Double.valueOf(peptideExtraFields[2]),
                        Double.valueOf(peptideExtraFields[3]),
                        Double.valueOf(peptideExtraFields[4]),
                        Double.valueOf(peptideExtraFields[5]),
                        Double.valueOf(peptideExtraFields[6]),
                        Double.valueOf(peptideExtraFields[7]),
                        Double.valueOf(peptideExtraFields[8]),
                        Boolean.valueOf(peptideExtraFields[9]),
                        Boolean.valueOf(peptideExtraFields[10]),
                        Double.valueOf(peptideExtraFields[11]),
                        Double.valueOf(peptideExtraFields[12]),
                        Double.valueOf(peptideExtraFields[13]),
                        peptideExtraFields[14],
                        peptideExtraFields[15],
                        peptideExtraFields[16],
                        Double.valueOf(peptideExtraFields[17]),
                        Double.valueOf(peptideExtraFields[18]),
                        Integer.valueOf(peptideExtraFields[19]),
                        Integer.valueOf(peptideExtraFields[20]),
                        Integer.valueOf(peptideExtraFields[21]),
                        Integer.valueOf(peptideExtraFields[22]),
                        Double.valueOf(peptideExtraFields[23]),
                        Double.valueOf(peptideExtraFields[24]),
                        Double.valueOf(peptideExtraFields[25]),
                        peptideExtraFields[26],
                        // Hits
                        pep.getReference(),
                        pep.getSequence(),
                        Double.valueOf(pep.getMZ()),
                        pep.getCharge()
                );
                dataList.add(record);

            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(PeptideController.class.getName())
                    .log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PeptideController.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

    }

}
