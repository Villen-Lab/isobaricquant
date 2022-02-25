package edu.uw.villenlab.isobaricquant.controllers;

//import com.sun.javafx.webkit.WebConsoleListener;
import edu.uw.VillenLab.Elements.Peak;
import edu.uw.VillenLab.MZParser.Elements.Precursor;
import edu.uw.VillenLab.MZParser.Elements.Scan;
import edu.uw.VillenLab.MathUtils;
import edu.uw.VillenLab.ProteomeUtils.IsoUtils;
import edu.uw.VillenLab.ProteomeUtils.Masses;
import edu.uw.VillenLab.ProteomeUtils.PeptideUtils;
import edu.uw.villenlab.isobaricquant.models.FragmentModel;
import edu.uw.villenlab.isobaricquant.models.LabelModel;
import edu.uw.villenlab.isobaricquant.models.PeptideModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.commons.math3.util.Precision;
import org.json.JSONArray;
import org.json.JSONObject;
import villeninputs.input.spectroscopy.MzFile;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;

public class IsobaricController implements Initializable {

    @FXML
    private TableView labelsTable;

    @FXML
    private TableView fragmentsTable;

    @FXML
    private WebView webView;
    private WebEngine webengine;

    private final ObservableList<LabelModel> dataLabelList
            = FXCollections.observableArrayList();

    private final ObservableList<FragmentModel> dataFragmentList
            = FXCollections.observableArrayList();

    private JSObject javascriptConnector;

    private int searchID;
    private int peptideID;

    private int ms1ScanNumber;
    private int ms2ScanNumber;
    private int msnScanNumber;

    private double minMZ_msN;
    private double maxMZ_msN;

    private double minMZ_ms2;
    private double maxMZ_ms2;

    private double minMZ_ms1;
    private double maxMZ_ms1;

    private double precMz;

    private double searchHitMz;

    private PeptideModel peptideData;

    private Scan ms1Scan;
    private Scan ms2Scan;
    private Scan msnScan;

    private int PPMTolerance = 10;
    private int MS1PPMTolerance = 10;
    private int MS2PPMTolerance = 1000;
    private boolean ms2NeutralLosses = false;

    private double ms1RT;
    private double ms2RT;
    private double msNRT;
    private double ms2PrecWindowWideness = 1;
    private double ms3PrecWindowWideness = 5;
    private double ms3NoiseMassTolerance = 1000;

    private String titleMsN;
    private String subtitleMsN;
    private String titleMs2;
    private String subtitleMs2;
    private String titleMs1;
    private String subtitleMs1;

    private int msLevel = 2;

    private ArrayList<Color> colors;

    private HashMap<String, Double> fragmentIonsMap;
    private ArrayList<LabelModel> peptideLabels;

    private String ms3PNoiseData;

    private String labelPath;
    private String fragmentPath;
    private String fragmentCsvFile = "";
    private String labelsCsvFile = "";

    DecimalFormat mzDf = new DecimalFormat("#.####");
    DecimalFormat rtDf = new DecimalFormat("#.##");

    private JSONObject ms1Data() {
        calcMS1Limits();
        JSONArray ms1PeaksData = new JSONArray(genMS1Series());
        JSONArray scoreBands = new JSONArray(genMS1Bands());
        JSONObject json = new JSONObject();
        json.put("titleMs1", titleMs1);
        json.put("subtitleMs1", subtitleMs1);
        json.put("minMZ_ms1", minMZ_ms1);
        json.put("maxMZ_ms1", maxMZ_ms1);
        json.put("scoreBands", scoreBands);
        json.put("searchID", searchID);
        json.put("peptideID", peptideID);
        json.put("ms1PeaksData", ms1PeaksData);

        return json;
    }

    private JSONObject ms2Data() {
        JSONArray ms2PeaksData = new JSONArray(genMS2Series());
        JSONArray precursorBands = new JSONArray(genPrecursorBands());

        JSONObject json = new JSONObject();
        json.put("titleMs2", titleMs2);
        json.put("subtitleMs2", subtitleMs2);
        json.put("minMZ_ms2", minMZ_ms2);
        json.put("maxMZ_ms2", maxMZ_ms2);
        json.put("precursorBands", precursorBands);
        json.put("searchID", searchID);
        json.put("peptideID", peptideID);
        json.put("ms2PeaksData", ms2PeaksData);

        return json;
    }

    private JSONObject msnData() {
        calcMSNLimits();
        JSONArray msNPeaksData = new JSONArray(genMSNSeries());
        JSONObject json = new JSONObject();
        json.put("titleMsN", titleMsN);
        json.put("subtitleMsN", subtitleMsN);
        json.put("minMZ_msN", minMZ_msN);
        json.put("maxMZ_msN", maxMZ_msN);
        JSONArray noiseband = new JSONArray(genNoiseBand());
        json.put("noiseband", noiseband);
        json.put("searchID", searchID);
        json.put("peptideID", peptideID);
        json.put("msNPeaksData", msNPeaksData);
        return json;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    private void calcMSNLimits() {

        minMZ_msN = Integer.MAX_VALUE;
        maxMZ_msN = 0;

        for (LabelModel label : peptideLabels) {
            if (label.getMz() != 0 && minMZ_msN > label.getMz()) {
                minMZ_msN = label.getMz();
            }
            if (maxMZ_msN < label.getMz()) {
                maxMZ_msN = label.getMz();
            }
        }

        if (maxMZ_msN == 0) {
            maxMZ_msN = 1600;
            return;
        }

        minMZ_msN -= 1;
        maxMZ_msN += 1;

    }

    private void readCSV(int peptideID) throws IOException {
        String FieldDelimiter = ",";
        fragmentIonsMap = new HashMap<>();
        peptideLabels = new ArrayList<>();

        BufferedReader fragmentBr;

        try {
            fragmentBr = new BufferedReader(new FileReader(fragmentCsvFile));

            fragmentBr.readLine();
            String line;
            line = fragmentBr.readLine();
            while ((line = fragmentBr.readLine()) != null) {
                String[] fields = line.split(FieldDelimiter, -1);

                if (Integer.valueOf(fields[1]) == peptideID) {
                    FragmentModel record = new FragmentModel(Integer.valueOf(fields[0]), Integer.valueOf(fields[1]), Double.valueOf(fields[2]),
                            fields[3], Integer.valueOf(fields[4]), Double.valueOf(fields[5]), Double.valueOf(fields[6]), Double.valueOf(fields[7]), fields[8]);
                    dataFragmentList.add(record);
                    fragmentIonsMap.put(record.getType() + record.getPosition(), record.getMz());
                }
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(IsobaricController.class.getName())
                    .log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(IsobaricController.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

        BufferedReader labelsBr;

        try {
            labelsBr = new BufferedReader(new FileReader(labelsCsvFile));

            labelsBr.readLine();
            String line;
            line = labelsBr.readLine();
            while ((line = labelsBr.readLine()) != null) {
                String[] fields = line.split(FieldDelimiter, -1);

                if (Integer.valueOf(fields[0]) == peptideID) {
                    //labLine = String.format(templateLab, peptideId, quantId, label.getId(), labMz[j], labPPMVar[j], scan.getScanNumber(), scan.getScanNumber(), 1, 0.0, 0.0,labIntensities[j], ms2scan, retT);
                    /*int quantID, int peptideID, String labelName, double mz, double incMZ, int firstScan, int lastScan, int numScans, double score, double area, double maxIntensity, int maxIntensityScan, double maxIntensityRetT*/
                    //LabelModel record = new LabelModel(Integer.valueOf(fields[1]), Integer.valueOf(fields[0]), fields[2],  Double.valueOf(fields[3]), Double.valueOf(fields[4]), Integer.valueOf(fields[5]), Integer.valueOf(fields[6]), Integer.valueOf(fields[7]), Double.valueOf(fields[8]), Double.valueOf(fields[9]), Double.valueOf(fields[10]), Integer.valueOf(fields[11]), Double.valueOf(fields[12]));
                    LabelModel record = new LabelModel(Integer.valueOf(fields[1]),
                            Integer.valueOf(fields[0]),
                            fields[2],
                            Double.valueOf(fields[3]),
                            Double.valueOf(fields[4]),
                            Integer.valueOf(fields[5]),
                            Integer.valueOf(fields[6]),
                            Integer.valueOf(fields[7]),
                            Double.valueOf(fields[8]),
                            Double.valueOf(fields[9]),
                            Double.valueOf(fields[10]),
                            Integer.valueOf(fields[11]),
                            Double.valueOf(fields[12]));
                    dataLabelList.add(record);
                    peptideLabels.add(record);
                }
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(IsobaricController.class.getName())
                    .log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(IsobaricController.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

    }

    private void genTitle() {

        if (msnScan != null) {
            msLevel = msnScan.getMsLevel();
        } else if (ms2Scan != null) {
            msnScan = ms2Scan;
            msnScanNumber = ms2Scan.getScanNumber();
        } else {
            msnScan = ms1Scan;
            ms2Scan = ms1Scan;
            ms2ScanNumber = ms1Scan.getScanNumber();
            msnScanNumber = ms1Scan.getScanNumber();
        }

        msNRT = msnScan.getRetentionTime();
        List<Precursor> msnScanPrecursors = msnScan.getPrecursors();

        titleMsN = "MS" + msLevel + " Scan " + msnScanNumber + " | RT: " + rtDf.format(msNRT);

        subtitleMsN = "[";
        for (Precursor ms2P : msnScanPrecursors) {
            subtitleMsN += mzDf.format(ms2P.getMZ());
            subtitleMsN += ",";
        }
        subtitleMsN = subtitleMsN.substring(0, subtitleMsN.length() - 1) + "]<br/>";
        if (msLevel == 3) {
            int number = peptideData.getScanNumber();
            subtitleMsN += "<b>MS2 (" + number + ") :</b> " + subtitleMs2 + "<br/>";
        }

        subtitleMsN += "<b>" + peptideData.getSequence() + "<b/><br>" + "(" + peptideData.getSearchID() + ") ";// + search.getName();

        String ms1num = (ms1ScanNumber != -1) ? ms1ScanNumber + "" : "";
        if(ms1RT == 0.0 && ms1Scan != null) {
            ms1RT = ms1Scan.getRetentionTime();
        }
        titleMs1 = "MS1 Scan " + ms1num + " | RT: " + rtDf.format(ms1RT);
        subtitleMs1 = "";
    }

    private void calcMS1Limits() {

        precMz = minMZ_ms1 = maxMZ_ms1 = searchHitMz;

        minMZ_ms1 -= 4; // 4: 1 is the window used + 3 for showing purposes
        maxMZ_ms1 += 4;

    }

    private void initChartColors() {
        colors = new ArrayList<>();

        colors.add(new Color(0, 128, 255));
        colors.add(new Color(255, 51, 102));
        colors.add(new Color(46, 184, 0));
        colors.add(new Color(184, 0, 245));
        colors.add(new Color(255, 255, 0));
        colors.add(new Color(128, 0, 255));
        colors.add(new Color(0, 255, 0));
        colors.add(new Color(0, 0, 255));
        colors.add(new Color(255, 0, 0));
        colors.add(new Color(0, 0, 0));
        colors.add(new Color(172, 102, 255));
        colors.add(new Color(102, 178, 255));
        colors.add(new Color(255, 102, 102));
        colors.add(new Color(184, 138, 0));

        colors.add(new Color(21, 255, 240));
        colors.add(new Color(150, 255, 51));
        colors.add(new Color(131, 60, 67));
        colors.add(new Color(131, 120, 60));
    }

    private int getNearestPeakIndex(List<Peak> peaks, double mz, int startIndex, int endIndex) {

        if (peaks.isEmpty()) {
            return -1;
        }

        if (startIndex == endIndex) {
            return startIndex;
        }

        int halfIndex = (startIndex + endIndex) / 2;

        if (Math.abs(mz - peaks.get(halfIndex + 1).getMZ()) < Math.abs(mz - peaks.get(halfIndex).getMZ())) {
            return getNearestPeakIndex(peaks, mz, halfIndex + 1, endIndex);
        } else {
            return getNearestPeakIndex(peaks, mz, startIndex, halfIndex);
        }
    }

    private Peak getNearestPeak(List<Peak> peaks, double mz, int ppmTolerance) {

        if (peaks.isEmpty()) {
            return null;
        }

        int nearestPeakIndex = getNearestPeakIndex(peaks, mz, 0, peaks.size() - 1);

        double realPpm = Math.abs(1000000 * (mz - peaks.get(nearestPeakIndex).getMZ()) / mz);

        if (realPpm < ppmTolerance) {
            return peaks.get(nearestPeakIndex);
        } else {
            return null;
        }

    }

    private String genMS1Series() {

        StringBuilder sb = new StringBuilder();

        if (ms1Scan != null) {
            try {
                List<Peak> peaks = new ArrayList<>(ms1Scan.getPeaks());
                ms1RT = ms1Scan.getRetentionTime();
                sb.append("[");

                double incIsoMZ = Masses.MASS_DIFF_C13C12 / peptideData.getCharge();

                Color c = colors.get(7);
                double mz = precMz;

                Peak peak = getNearestPeak(peaks, mz, MS1PPMTolerance);
                if (peak != null) {

                    sb.append("{");
                    sb.append("name: '").append("Detected peptide', ");
                    sb.append("color: 'rgba(").append(c.getRed()).append(", ").append(c.getGreen()).append(", ").append(c.getBlue()).append(", 1)', ");
                    sb.append("zIndex: 50, ");
                    sb.append("data: [");

                    for (int j = 0; j < 5; j++) {

                        Peak peak2 = getNearestPeak(peaks, mz + incIsoMZ * j, MS1PPMTolerance);
                        if (peak2 != null) {
                            sb.append("[").append(peak2.getMZ()).append(",").append(peak2.getIntensity()).append("],");
                            peaks.remove(peak2);
                        }
                    }

                    sb.append("]");
                    sb.append("},");
                }

                // Rest of the peaks
                sb.append("{");
                sb.append("name: 'MS1 peaks', ");
                sb.append("data: [");
                for (Peak p : peaks) {
                    if (p.getMZ() >= minMZ_ms1 && p.getMZ() <= maxMZ_ms1) {
                        sb.append("[").append(p.getMZ()).append(",").append(p.getIntensity()).append("],");
                    }
                }
                sb.append("]");
                sb.append("},");

                c = colors.get(2);

                //Here we create new empty series elution window so they will appear in the legend
                sb.append("{");
                sb.append("id: '").append("Elution Window', ");
                sb.append("name: '").append("Elution Window', ");
                sb.append("color: 'rgba(").append(c.getRed()).append(", ").append(c.getGreen()).append(", ").append(c.getBlue()).append(", 0.2)', ");
                sb.append("data: []");
                sb.append("},");

                sb.deleteCharAt(sb.length() - 1);
                sb.append("]");
            } catch (DataFormatException ex) {
                Logger.getLogger(IsobaricController.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            sb.append("[]");
        }

        return sb.toString();

    }

    private String genMS1Bands() {

        StringBuilder sb = new StringBuilder();

        Color c = colors.get(6);
        Color pC = colors.get(10);

        String score = String.format("%.2f", peptideData.getScore() * 100);

        double ms1MzTolerance = PeptideUtils.massToMZ(ms2PrecWindowWideness, peptideData.getCharge());

        sb.append("[");
        sb.append("{");
        sb.append("label: { text: 'Score: ").append(score).append("%' },");
        sb.append("id: '").append("Elution Window', ");
        sb.append("from: ").append(precMz - 1).append(", ");
        sb.append("to: ").append(precMz + 1).append(", ");
        sb.append("color: 'rgba(").append(c.getRed()).append(", ").append(c.getGreen()).append(", ").append(c.getBlue()).append(", 0.3)', ");
        sb.append("}");

        int cont = 0;
        for (Precursor p : ms2Scan.getPrecursors()) {
            // Highlight  precursor area
            sb.append(",{");
            sb.append("id: '").append(cont++).append("', ");
            sb.append("from: ").append(p.getMZ() - ms1MzTolerance).append(", ");
            sb.append("to: ").append(p.getMZ() + ms1MzTolerance).append(", ");
            sb.append("color: 'rgba(").append(pC.getRed()).append(", ").append(pC.getGreen()).append(", ").append(pC.getBlue()).append(", 0.5)', ");
            sb.append("}");
        }

        sb.append("]");

        return sb.toString();
    }

    private List<Peak> getNearestPeaksInWindow(List<Peak> peaks, double mz, int ppmTolerance, double daWindow, int charge) {
        List<Peak> mPeaks = new ArrayList<>();
        if (peaks.isEmpty()) {
            return null;
        }

        // precursor
        int nearestPeakIndex = getNearestPeakIndex(peaks, mz, 0, peaks.size() - 1);

        double realPpm = Math.abs(1000000 * (mz - peaks.get(nearestPeakIndex).getMZ()) / mz);

        if (realPpm <= ppmTolerance) {
            // precursor will always be on the first position
            Peak peak = peaks.get(nearestPeakIndex);
            double precursorMz = peak.getMZ();
            double mzWind = PeptideUtils.massToMZ(daWindow, charge);
            double precursorMass = PeptideUtils.mzToMass(peak.getMZ(), charge);
            double lMass = precursorMass - daWindow;
            double lMz = precursorMz - mzWind;//PeptideUtils.massToMZ(lMass, charge);
            mPeaks.add(peak);
            int ind = nearestPeakIndex - 1;
            boolean found = true;
            while (ind >= 0 && found) {
                peak = peaks.get(ind);
                if (peak.getMZ() >= lMz) {
                    mPeaks.add(peak);
                } else {
                    found = false;
                }
                --ind;
            }
            ind = nearestPeakIndex + 1;
            double hMass = precursorMass + daWindow;
            double hMz = precursorMz + mzWind; //PeptideUtils.massToMZ(hMass, charge);
            found = true;
            while (ind < peaks.size() && found) {
                peak = peaks.get(ind);
                if (peak.getMZ() <= hMz) {
                    mPeaks.add(peak);
                } else {
                    found = false;
                }
                ++ind;
            }
            return mPeaks;
        } else {
            return null;
        }
    }

    private String genMS2Series() {

        StringBuilder sb = new StringBuilder();
        StringBuilder sbN = new StringBuilder();
        //Scan scan = access.getRunManager().getScan(quant.getRunID(), ms2Scan);
        ms2RT = ms2Scan.getRetentionTime();

        //searchMods.setSearchModifications(search.getVarModsParams(), search.getConsModsParams(), search.getVarTermParams());
        //String sequence = peptideData.getSequence();
        //List<Modification> peptideVarMods = IsoUtils.getPeptideVarModifications(sequence, searchMods.variableModifications);
        //String cleanSeq = PeptideUtils.cleanSequence(sequence);
        //double nMod = IsoUtils.calcNTermMass(sequence, searchMods);//(IsoUtils.hasTermMod(sequence, 'n')) ? searchMods.nTermVarModification : 0;
        //double cMod = IsoUtils.calcCTermMass(sequence, searchMods);//(IsoUtils.hasTermMod(sequence, 'c')) ? searchMods.cTermVarModification : 0;
        //int charge = peptideData.getCharge();
        //FragmentIons fIons = new FragmentIons(cleanSeq, charge, cMod, nMod, peptideVarMods, searchMods.constantModifications, FragmentIons.MassType.MONO);
        Color cNLosses = colors.get(9);
        Color cPeaks = colors.get(10);
        Color cFb = colors.get(11);
        Color cFy = colors.get(12);
        Color cFa = colors.get(13);
        Color cFc = colors.get(14);
        Color cFx = colors.get(15);
        Color cFz = colors.get(16);

        if (ms2Scan != null) {

            try {

                titleMs2 = "MS2 Scan " + ms2Scan.getScanNumber() + " | RT: " + ms2RT;
                subtitleMs2 = "[";
                for (Precursor ms2P : ms2Scan.getPrecursors()) {
                    subtitleMs2 += mzDf.format(ms2P.getMZ());
                    subtitleMs2 += ",";
                }
                subtitleMs2 = subtitleMs2.substring(0, subtitleMs2.length() - 1) + "]<br/>";

                List<Peak> peaks = new ArrayList<>(ms2Scan.getPeaks());

                sb.append("[");

                double mz = precMz;
                HashMap<String, FragmentGroup> fragGroup = new HashMap<>();
                Peak auxPeak;

                for (FragmentModel fm : dataFragmentList) {
                    if (fm.getMatched().equalsIgnoreCase("true")) {
                        boolean found = false;
                        double wind = fm.getMzDiff() + 0.5;
                        for (int i = 0; !found && i < peaks.size(); ++i) {
                            auxPeak = peaks.get(i);
                            // we are narrowing the search to avoid doing expensive operations for all peaks
                            if (auxPeak.getMZ() <= fm.getMz() + wind && auxPeak.getMZ() >= fm.getMz() - wind) {
                                if (Precision.equals(auxPeak.getMZ(), fm.getMz() + fm.getMzDiff(), 1e-5) || Precision.equals(auxPeak.getMZ(), fm.getMz() - fm.getMzDiff(), 1e-5)) {
                                    found = true;
                                    // fragment ions will be grouped by type (b, a, etc) and charge
                                    String fId = ((int) fm.getCharge()) + fm.getType();
                                    FragmentGroup auxFg = fragGroup.get(fId);
                                    if (auxFg == null) {
                                        auxFg = new FragmentGroup(fId);
                                    }
                                    auxFg.addData("{ x:" + auxPeak.getMZ() + ", y:" + auxPeak.getIntensity() + ", lName: '" + fId + fm.getPosition() + "'},");
                                    fragGroup.put(fId, auxFg);
                                }
                            }
                        }
                    }

                }

                for (String ionType : fragGroup.keySet()) {

                    FragmentGroup fg = fragGroup.get(ionType);
                    String legendLabelName = fg.getIonType() + "(" + fg.getCount() + ")";

                    sb.append("{");

                    sb.append("name: '").append(legendLabelName).append("',");

                    if (ionType.contains("b")) {
                        sb.append("color: 'rgba(")
                                .append(cFb.getRed()).append(", ")
                                .append(cFb.getGreen()).append(", ")
                                .append(cFb.getBlue()).append(", 1)', ");
                    } else if (ionType.contains("y")) {
                        sb.append("color: 'rgba(")
                                .append(cFy.getRed()).append(", ")
                                .append(cFy.getGreen()).append(", ")
                                .append(cFy.getBlue()).append(", 1)', ");
                    } else if (ionType.contains("a")) {
                        sb.append("color: 'rgba(")
                                .append(cFa.getRed()).append(", ")
                                .append(cFa.getGreen()).append(", ")
                                .append(cFa.getBlue()).append(", 1)', ");
                    } else if (ionType.contains("c")) {
                        sb.append("color: 'rgba(")
                                .append(cFc.getRed()).append(", ")
                                .append(cFc.getGreen()).append(", ")
                                .append(cFc.getBlue()).append(", 1)', ");
                    } else if (ionType.contains("x")) {
                        sb.append("color: 'rgba(")
                                .append(cFx.getRed()).append(", ")
                                .append(cFx.getGreen()).append(", ")
                                .append(cFx.getBlue()).append(", 1)', ");
                    } else if (ionType.contains("z")) {
                        sb.append("color: 'rgba(")
                                .append(cFz.getRed()).append(", ")
                                .append(cFz.getGreen()).append(", ")
                                .append(cFz.getBlue()).append(", 1)', ");
                    }

                    sb.append("dataLabels: [{format: '({point.lName})'}], ");
                    sb.append("zIndex: 50, ");
                    sb.append("data: [");
                    sb.append(fg.getData());
                    sb.append("]");
                    sb.append("},");

                }

                int pId = 1;
                sbN.append("[");
                String nData = "";
                String pData = "";
                for (Precursor ms3P : msnScan.getPrecursors()) {
                    if (ms3P.getPrecursorScanLevel() != 1) {    // skip ms1 prec if present
                        List<Peak> peaksInWindow = getNearestPeaksInWindow(peaks, ms3P.getMZ(), MS2PPMTolerance, ms3PrecWindowWideness, peptideData.getCharge());
                        if (peaksInWindow != null) {
                            List<Double> intensities = new ArrayList<>();
                            for (Peak p : peaksInWindow) {
                                intensities.add(p.getIntensity());
                            }
                            double noise = MathUtils.Median(intensities).doubleValue();
                            Peak precursorPeak = peaksInWindow.get(0);//getNearestPeak(peaks, ms3P.getMZ(), MS2PPMTolerance);
                            if (precursorPeak != null) {
                                nData += "{ x:" + precursorPeak.getMZ() + ", y:" + noise + ", lName: 'N" + pId + "'},";

                                pData += "{ x:" + precursorPeak.getMZ() + ", y:" + precursorPeak.getIntensity() + ", lName: 'P" + pId + "'},";

                                peaks.remove(precursorPeak);

                                pId++;
                            }
                        }
                    }
                }

                String legendLabelName = "Noise";
                sb.append("{");
                sb.append("name: '").append(legendLabelName).append("',");
                sb.append("color: 'rgba(186,60,61,.9)',");
                sb.append("dataLabels: [{format: '({point.lName})'}], ");
                sb.append("zIndex: 50, ");
                sb.append("pointPadding: 0.3,");
                sb.append("pointPlacement: -0.2,");
                sb.append("data: [");
                sb.append(nData);
                sb.append("]");
                sb.append("},");

                legendLabelName = "Precursors(" + (pId - 1) + ")";
                sb.append("{");
                sb.append("name: '").append(legendLabelName).append("',");
                sb.append("color: 'rgba(248,161,63,1)',");
                sb.append("dataLabels: [{format: '({point.lName})'}], ");
                sb.append("zIndex: 50, ");
                sb.append("pointPadding: 0.4,");
                sb.append("pointPlacement: -0.2,");
                sb.append("data: [");
                sb.append(pData);
                sb.append("]");
                sb.append("},");

                sbN.append("]");
                ms3PNoiseData = sbN.toString();
                // Rest of the peaks
                sb.append("{");
                sb.append("name: 'MS2 peaks', ");
                sb.append("dataLabels:{enabled: false},");
                sb.append("color: 'rgba(").append(cPeaks.getRed()).append(", ").append(cPeaks.getGreen()).append(", ").append(cPeaks.getBlue()).append(", 0.5)', ");
                sb.append("data: [");
                if (peaks.size() > 0) {
                    minMZ_ms2 = peaks.get(0).getMZ();
                    maxMZ_ms2 = peaks.get(peaks.size() - 1).getMZ();
                }
                for (Peak p : peaks) {
                    sb.append("[").append(p.getMZ()).append(",").append(p.getIntensity()).append("],");
                }
                sb.append("]");
                sb.append("},");
                sb.append("]");
            } catch (DataFormatException ex) {
                Logger.getLogger(IsobaricController.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            sb.append("[]");
        }

        return sb.toString();

    }

    private String genPrecursorBands() {
        StringBuilder sb = new StringBuilder();

        Color c = colors.get(6);

        //String score = String.format("%.2f", quantPeptide.getScore() * 100);
        double ms3PrecMzTolerance = PeptideUtils.massToMZ(ms3PrecWindowWideness, peptideData.getCharge());
        sb.append("[");
        for (Precursor ms3Prec : msnScan.getPrecursors()) {
            if (ms3Prec.getPrecursorScanLevel() != 1) { // skip ms1 precursor if present
                sb.append("{");
                sb.append("id: '").append("Precursor Window', ");
                sb.append("from: ").append(ms3Prec.getMZ() - ms3PrecMzTolerance).append(", ");
                sb.append("to: ").append(ms3Prec.getMZ() + ms3PrecMzTolerance).append(", ");
                sb.append("color: 'rgba(").append(c.getRed()).append(", ").append(c.getGreen()).append(", ").append(c.getBlue()).append(", 0.3)', ");
                sb.append("},");
            }
        }
        sb.append("]");

        return sb.toString();
    }

    private String genMSNSeries() {

        StringBuilder sb = new StringBuilder();

        if (msnScan != null) {
            try {
                msLevel = msnScan.getMsLevel();
                List<Peak> peaks = new ArrayList<>(msnScan.getPeaks());

                sb.append("[");

                int i = 0;

                // label peak
                for (LabelModel label : peptideLabels) {

                    Color c = colors.get(i++);

                    double mz = label.getMz();

                    Peak labelPeak = getNearestPeak(peaks, mz, PPMTolerance);
                    if (labelPeak == null) {
                        continue;
                    }

                    sb.append("{");
                    sb.append("name: '").append(label.getLabelName()).append("', ");
                    sb.append("zIndex: 100, ");
                    sb.append("dataLabels: [{enabled: true, format: '" + label.getLabelName() + "'}], ");
                    //sb.append("dataLabels: { enabled: true, formatter: function() { return '").append(label.getLabelName()).append("'; } }, ");
                    sb.append("color: 'rgba(").append(c.getRed()).append(", ").append(c.getGreen()).append(", ").append(c.getBlue()).append(", 1)', ");
                    sb.append("data: [[").append(labelPeak.getMZ()).append(",").append(labelPeak.getIntensity()).append("]]");
                    sb.append("},");

                    // removing the peak in order to don't show it twice
                    peaks.remove(labelPeak);

                }

                // Rest of the peaks
                sb.append("{");
                sb.append("name: 'Noise peaks', ");
                sb.append("data: [");
                for (Peak p : peaks) {
                    if (p.getMZ() >= minMZ_msN && p.getMZ() <= maxMZ_msN) {
                        sb.append("[").append(p.getMZ()).append(",").append(p.getIntensity()).append("],");
                    }
                }
                sb.append("]");
                sb.append("},");

                // Creates an empty series for the noise so it will appear in the legend
                sb.append("{");
                sb.append("name: 'Noise level', ");
                sb.append("color: 'rgba(184, 138, 0, .2)', ");
                sb.append("data: [");
                sb.append("]");
                sb.append("}");

                sb.append("]");
            } catch (DataFormatException ex) {
                Logger.getLogger(IsobaricController.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            sb.append("[]");
        }

        return sb.toString();

    }

    private String genNoiseBand() {

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        sb.append("{");
        sb.append("id: 'Noise level', ");
        sb.append("from: 0, ");
        sb.append("to: ").append(peptideData.getNoise()).append(", ");
        sb.append("color: 'rgba(184, 138, 0, 0.8)', ");
        sb.append("label: {text:'Noise level'}");
        sb.append("}");
        sb.append("]");

        return sb.toString();

    }

    void findFragments(PeptideModel data, MzFile mzFile, String labelPath, String fragmentPath) {

        this.labelPath = labelPath;
        this.fragmentPath = fragmentPath;

        if (labelPath != null && !"".equals(labelPath)) {
            labelsCsvFile = labelPath;
        }
        if (fragmentPath != null && !"".equals(fragmentPath)) {
            fragmentCsvFile = fragmentPath;
        }

        Map<Integer, Scan> scansMap = mzFile.getScansMap();

        this.peptideData = data;
        this.searchID = data.getSearchID();
        this.peptideID = data.getPeptideID();
        this.ms1ScanNumber = data.getMs1ScanNumber();
        this.ms2ScanNumber = data.getMs2ScanNumber();
        this.msnScanNumber = data.getMs3ScanNumber();
        this.ms1Scan = scansMap.get(ms1ScanNumber);
        this.ms2Scan = scansMap.get(ms2ScanNumber);
        this.msnScan = scansMap.get(msnScanNumber);

        this.searchHitMz = data.getMz();

        try {
            readCSV(data.getPeptideID());
        } catch (IOException ex) {
            Logger.getLogger(IsobaricController.class.getName()).log(Level.SEVERE, null, ex);
        }

        initChartColors();
        calcMS1Limits();

        genTitle();

        webengine = webView.getEngine();
        webengine.setJavaScriptEnabled(true);
        //URL htmlURL = IsobaricQuant.class.getResource("/resources/html/charts.html");
        URL htmlURL = this.getClass().getClassLoader().getResource("html/charts.html");
        webengine.load(htmlURL.toExternalForm());

        //webengine.load("https://www.highcharts.com/demo");

       /* WebConsoleListener.setDefaultListener((webView, message, lineNumber, sourceId) -> {
            System.out.println(message + "[at " + lineNumber + "]");
        });*/

        webengine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (Worker.State.SUCCEEDED == newValue) {
                javascriptConnector = (JSObject) webengine.executeScript("getJsConnector()");

                /*JSONArray result = new JSONArray();

                JSONObject json = new JSONObject();
                json.put("name", "Year 1800");
                json.put("data", Arrays.asList(107, 31, 635, 203, 2));

                result.put(json);

                json = new JSONObject();
                json.put("name", "Year 1900");
                json.put("data", Arrays.asList(133, 156, 947, 408, 6));

                result.put(json);

                json = new JSONObject();
                json.put("name", "Year 2000");
                json.put("data", Arrays.asList(814, 841, 3714, 727, 31));

                result.put(json);

                json = new JSONObject();
                json.put("name", "Year 2016");
                json.put("data", Arrays.asList(1216, 1001, 4436, 738, 40));

                result.put(json);*/

                JSONObject res = new JSONObject();

                res.put("ms1Data", ms1Data());
                res.put("ms2Data", ms2Data());
                res.put("msnData", msnData());
                //res.put("testData", result);
                javascriptConnector.call("showResult", res);
            }
        });

        //System.out.println(mzFile.getScansMap().get(data.getMs3ScanNumber()).getFilterLine());

        labelsTable.setItems(dataLabelList);

        TableColumn quantIDCol = new TableColumn("QuantID");
        quantIDCol.setCellValueFactory(
                new PropertyValueFactory<>("QuantID"));
        TableColumn peptideIDCol = new TableColumn("PeptideID");
        peptideIDCol.setCellValueFactory(
                new PropertyValueFactory<>("PeptideID"));
        TableColumn labelNameCol = new TableColumn("labelName");
        labelNameCol.setCellValueFactory(
                new PropertyValueFactory<>("labelName"));
        TableColumn mzCol = new TableColumn("mz");
        mzCol.setCellValueFactory(
                new PropertyValueFactory<>("mz"));
        TableColumn incMZCol = new TableColumn("incMZ");
        incMZCol.setCellValueFactory(
                new PropertyValueFactory<>("incMZ"));
        TableColumn firstScanCol = new TableColumn("firstScan");
        firstScanCol.setCellValueFactory(
                new PropertyValueFactory<>("firstScan"));
        TableColumn lastScanCol = new TableColumn("lastScan");
        lastScanCol.setCellValueFactory(
                new PropertyValueFactory<>("lastScan"));
        TableColumn numScansCol = new TableColumn("numScans");
        numScansCol.setCellValueFactory(
                new PropertyValueFactory<>("numScans"));
        TableColumn scoreCol = new TableColumn("score");
        scoreCol.setCellValueFactory(
                new PropertyValueFactory<>("score"));

        TableColumn areaCol = new TableColumn("area");
        areaCol.setCellValueFactory(
                new PropertyValueFactory<>("area"));

        TableColumn maxIntensityCol = new TableColumn("maxIntensity");
        maxIntensityCol.setCellValueFactory(
                new PropertyValueFactory<>("maxIntensity"));

        TableColumn maxIntensityScanCol = new TableColumn("maxIntensityScan");
        maxIntensityScanCol.setCellValueFactory(
                new PropertyValueFactory<>("maxIntensityScan"));

        TableColumn maxIntensityRetTCol = new TableColumn("maxIntensityRetT");
        maxIntensityRetTCol.setCellValueFactory(
                new PropertyValueFactory<>("maxIntensityRetT"));

        labelsTable.getColumns().addAll(quantIDCol, peptideIDCol, labelNameCol, mzCol, incMZCol, firstScanCol, lastScanCol, numScansCol, scoreCol, areaCol, maxIntensityCol, maxIntensityScanCol, maxIntensityRetTCol);

        fragmentsTable.setItems(dataFragmentList);
        TableColumn fQuantIDCol = new TableColumn("quantID");
        fQuantIDCol.setCellValueFactory(
                new PropertyValueFactory<>("quantID"));
        TableColumn fPeptideIDCol = new TableColumn("peptideID");
        fPeptideIDCol.setCellValueFactory(
                new PropertyValueFactory<>("peptideID"));
        TableColumn fChargeCol = new TableColumn("charge");
        fChargeCol.setCellValueFactory(
                new PropertyValueFactory<>("charge"));
        TableColumn fTypeCol = new TableColumn("type");
        fTypeCol.setCellValueFactory(
                new PropertyValueFactory<>("type"));
        TableColumn fPositionCol = new TableColumn("position");
        fPositionCol.setCellValueFactory(
                new PropertyValueFactory<>("position"));
        TableColumn fMzCol = new TableColumn("mz");
        fMzCol.setCellValueFactory(
                new PropertyValueFactory<>("mz"));
        TableColumn fMzDiffCol = new TableColumn("mzDiff");
        fMzDiffCol.setCellValueFactory(
                new PropertyValueFactory<>("mzDiff"));
        TableColumn fIntensityCol = new TableColumn("intensity");
        fIntensityCol.setCellValueFactory(
                new PropertyValueFactory<>("intensity"));
        TableColumn fMatchedCol = new TableColumn("matched");
        fMatchedCol.setCellValueFactory(
                new PropertyValueFactory<>("matched"));

        fragmentsTable.getColumns().addAll(fQuantIDCol, fPeptideIDCol, fChargeCol, fTypeCol, fPositionCol, fMzCol, fMzDiffCol, fIntensityCol, fMatchedCol);
    }
}
