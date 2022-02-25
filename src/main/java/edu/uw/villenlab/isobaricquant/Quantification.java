package edu.uw.villenlab.isobaricquant;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.uw.VillenLab.Elements.Peak;
import edu.uw.VillenLab.Elements.SearchModifications;
import edu.uw.VillenLab.MZParser.Elements.Precursor;
import edu.uw.VillenLab.MZParser.Elements.Scan;
import edu.uw.VillenLab.MathUtils;
import edu.uw.VillenLab.ProteomeUtils.IsoUtils;
import edu.uw.VillenLab.ProteomeUtils.IsobaricLabels;
import edu.uw.VillenLab.ProteomeUtils.Masses;
import edu.uw.VillenLab.ProteomeUtils.PeptideUtils;
import edu.uw.villenlab.isobaricquant.controllers.DashboardController;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import static java.lang.Math.abs;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.math3.util.Pair;
import org.apache.commons.math3.util.Precision;
import org.xml.sax.SAXException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;
import villeninputs.Peptide;
import villeninputs.input.FactoryProvider;
import villeninputs.input.peptidesequence.PeptideFile;
import villeninputs.input.peptidesequence.PeptideFileFactory;
import villeninputs.input.spectroscopy.MzFile;
import villeninputs.input.spectroscopy.MzFileFactory;

public class Quantification {

    private static final String ALGORITHM = "IsobaricQuant";
    private static final String ALGORITHM_VERSION = "2.3";
    private static int PPMTolerance = 10;
    private static int MS1PPMTolerance = 10;
    private static int MS2PPMTolerance = 10;
    private static double mzTolerance = Double.NaN;
    private static int scanLevel = 2;
    private static double ms1PrecWindowDaltons = 10;
    private static boolean debug = false;

    private boolean findInPeaks(Precursor csP, List<Peak> peaks) {
        //double precursorMz = Precision.round(csP.getMZ(), 3, BigDecimal.ROUND_UP);
        double precursorMz = csP.getMZ();
        double peakMz;
        double diff = 0;
        for (Peak p : peaks) {
            //double peakMz = Precision.round(p.getMZ(), 3, BigDecimal.ROUND_UP);
            peakMz = p.getMZ();
            diff = peakMz - precursorMz;
            if (diff >= 0.001f) {
                return false;
            }
            if (abs(diff) < 0.001f) {
                return true;
            }

            /*if (peakMz > precursorMz) {
                return false;
            }
            if (precursorMz == peakMz) {
                return true;
            }*/
        }
        return false;
    }

    private enum MassType {
        HCD_FRAGMENTATION, NEUTRAL_MASS
    };
    private static MassType massType = MassType.HCD_FRAGMENTATION;

    private enum ScoreType {
        ISOTOPE_DISTRIBUTION, REPORTERS_INTENSITY, REPORTERS_FOUND
    }
    private static ScoreType scoreType = ScoreType.REPORTERS_INTENSITY;
    private static SearchModifications searchMods = new SearchModifications();
    private ArrayList<LabelData> labels;
    private Set<String> filteredLabels;
    private String confLabels;
    private int searchId;
    private int quantId;
    private int runId;
    private int confId;
    private String mzXMLFilePath;
    private String mzMLFilePath;
    private String peptideFilePath;
    private String token;
    private String outputDirectory;
    private String isoMethod;
    private int topXNum;
    private int ms1DepthSearch;
    private boolean recalcIntensities;
    private String dataSheetName;
    private ProductDataSheet productDataSheet;
    private double ms2PrecursorWindowTolerance;
    private boolean generateFragmentIonsFile;
    private String hitImpurity;
    private Scan.PeakSelection searchMethod = Scan.PeakSelection.MOST_INTENSE;
    private String mzFileType;
    private Set<Character> additionalFragmentIons;

    private Map<String, IsobaricLabels.IsobaricLabel> isoLabels;
    private ArrayList<IsobaricLabels.ComplementIonRange> complementIonsRanges;

    private double getReporterMass(IsobaricLabels.IsobaricLabel il) {
        if (massType == MassType.HCD_FRAGMENTATION) // 1 electron less
        {
            return il.neutralMass - Masses.MASS_ELECTRON;
        } else if (massType == MassType.NEUTRAL_MASS) {
            return il.neutralMass;
        }

        return 0;
    }

    /**
     * Initializes the isobaric labels using the method that has been provided
     * This method is used in order to obtain a better performance
     * pre-calculating the low and high mz of every label
     */
    private void initializeIsobaricLabels() {

        labels = new ArrayList<>();
        String[] labIds = confLabels.split(",");

        isoLabels = null;
        complementIonsRanges = new ArrayList<>();

        // For a better performance, the high and low mz ar pre-calculated
        if (isoMethod.equalsIgnoreCase("TMTDuplex")) {
            isoLabels = IsobaricLabels.TMTDuplex;
        } else if (isoMethod.equalsIgnoreCase("iTRAQ4plex")) {
            isoLabels = IsobaricLabels.iTRAQ4plex;
        } else if (isoMethod.equalsIgnoreCase("TMT6plex")) {
            isoLabels = IsobaricLabels.TMT6plex;
        } else if (isoMethod.equalsIgnoreCase("iTRAQ8plex")) {
            isoLabels = IsobaricLabels.iTRAQ8plex;
        } else if (isoMethod.equalsIgnoreCase("TMT10plex")) {
            isoLabels = IsobaricLabels.TMT10plex;
            complementIonsRanges = IsobaricLabels.complementIonsTMT10plex;
        } else if (isoMethod.equalsIgnoreCase("TMT11plex")) {
            isoLabels = IsobaricLabels.TMT11plex;
            complementIonsRanges = IsobaricLabels.complementIonsTMT11plex;
        } else if (isoMethod.equalsIgnoreCase("TMTpro")) {
            isoLabels = IsobaricLabels.TMTpro;
            complementIonsRanges = IsobaricLabels.complementIonsTMTpro;
        }

        if (isoLabels != null) {
            filteredLabels = new HashSet<>();
            if (!confLabels.equalsIgnoreCase("all")) {
                String id;
                for (int i = 0; i < labIds.length; ++i) {
                    id = labIds[i];
                    IsobaricLabels.IsobaricLabel il = isoLabels.get(id);
                    if (il != null) {
                        filteredLabels.add(id);
                    } else {
                        System.out.println("Label " + id + " does not exist");
                        System.exit(1);
                    }
                }
            } else {
                isoLabels.keySet().forEach((id) -> {
                    IsobaricLabels.IsobaricLabel il = isoLabels.get(id);
                    if (il != null) {
                        filteredLabels.add(id);
                    }
                });
            }

            isoLabels.keySet().forEach((id) -> {
                IsobaricLabels.IsobaricLabel il = isoLabels.get(id);
                if (il != null) {
                    labels.add(new LabelData(getReporterMass(il), id, PPMTolerance));
                }
            });

        }

        Collections.sort(labels);
    }

    private void prepareDataSheet() throws IOException {
        String[] tagOrder = new String[labels.size()];
        for (int i = 0; i < tagOrder.length; ++i) {
            tagOrder[i] = labels.get(i).getId();
        }
        System.out.println("Preparing data sheet matrix...");
        productDataSheet.prepareMatrix(tagOrder);
        productDataSheet.printMatrix(tagOrder);
    }

    public void readConfFile(String confPath, String dataSheetPath) throws IOException {
        JsonElement jsonElement = JsonParser.parseReader(new FileReader(confPath));
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        ms2PrecursorWindowTolerance = jsonObject.get("ms2NoiseWindowTolerance").getAsDouble();
        generateFragmentIonsFile = jsonObject.get("generateFragmentIonsFile").getAsBoolean();
        isoMethod = jsonObject.get("isoMethod").getAsString();
        confLabels = jsonObject.get("confLabels").getAsString();
        PPMTolerance = jsonObject.get("PPMTolerance").getAsInt();
        MS1PPMTolerance = jsonObject.get("MS1PPMTolerance").getAsInt();
        MS2PPMTolerance = jsonObject.get("MS2PPMTolerance").getAsInt();
        String massT = jsonObject.get("massType").getAsString();
        massType = (massT.equals("HCD fragmentation")) ? MassType.HCD_FRAGMENTATION : MassType.NEUTRAL_MASS;
        scanLevel = jsonObject.get("scanLevel").getAsInt();
        String scoreT = jsonObject.get("scoreType").getAsString();
        switch (scoreT) {
            case "REPORTERS_INTENSITY":
                scoreType = ScoreType.REPORTERS_INTENSITY;
                break;
            case "ISOTOPE_DISTRIBUTION":
                scoreType = ScoreType.ISOTOPE_DISTRIBUTION;
                break;
            case "REPORTERS_FOUND":
                scoreType = ScoreType.REPORTERS_FOUND;
                break;
            default:
                break;
        }
        ms1PrecWindowDaltons = jsonObject.get("ms1PrecWindowDaltons").getAsDouble();

        String nlFI = jsonObject.get("neutralLossesFI").getAsString();
        String nl = jsonObject.get("neutralLosses").getAsString();
        searchMods.setNeutralLosses(nlFI, nl);
        topXNum = jsonObject.get("topXNum").getAsInt();
        ms1DepthSearch = jsonObject.get("ms1DepthSearch").getAsInt();
        recalcIntensities = jsonObject.get("recalcIntensities").getAsBoolean();
        dataSheetName = jsonObject.get("dataSheetName").toString();

        additionalFragmentIons = new HashSet<>();
        if (jsonObject.has("additionalFragmentIons")) {
            String frgs[] = jsonObject.get("additionalFragmentIons").toString().split(",");
            for (int i = 0; i < frgs.length; ++i) {
                String frg = frgs[i].replace(" ", "");
                additionalFragmentIons.add(frg.charAt(0));
            }
        }

        hitImpurity = jsonObject.get("hitImpurity").toString();

        String searchMethod = jsonObject.get("searchMethod").toString();
        if (searchMethod.equalsIgnoreCase("lower_ppm_error")) {
            this.searchMethod = Scan.PeakSelection.LOWER_PPM_ERROR;
        } else if (searchMethod.equalsIgnoreCase("least_intense")) {
            this.searchMethod = Scan.PeakSelection.LEAST_INTENSE;
        } else {
            this.searchMethod = Scan.PeakSelection.MOST_INTENSE;
        }

        if (recalcIntensities) {

            JsonElement pdJsonElement = jsonObject.get("productDataSheet");
            JsonObject pdJsonObject = pdJsonElement.getAsJsonObject();

            int userID = pdJsonObject.get("userID").getAsInt();
            String dsName = pdJsonObject.get("name").getAsString();
            String dsDate = pdJsonObject.get("creationDate").getAsString();

            productDataSheet = new ProductDataSheet(dsName, userID, dsDate);
            if (dataSheetPath.isEmpty()) {
                InputStream in = IsobaricQuant.class.getResourceAsStream("/resources/data/dataSheet.csv");
                productDataSheet.setCSVContent(in);
            } else {
                File dataSheetFile = new File(dataSheetPath);
                InputStream in = new FileInputStream(dataSheetFile);
                productDataSheet.setCSVContent(in);
            }

        }

        JsonElement modJsonElement = jsonObject.get("modifications");
        JsonObject modJsonObject = modJsonElement.getAsJsonObject();

        String varMods = modJsonObject.get("varMods").getAsString();
        String consMods = modJsonObject.get("consMods").getAsString();
        String varTerm = modJsonObject.get("varTermParams").getAsString();

        searchMods.setSearchModifications(varMods, consMods, varTerm);
    }

    /**
     * Gets the average of the given array
     *
     * @param intensities list of data to be avg
     * @return avg
     */
    private double avg(List<Double> intensities) {

        double avg = 0;

        for (int i = 0; i < intensities.size(); ++i) {
            avg = intensities.get(i);
        }

        return avg / intensities.size();

    }

    /**
     * Calculates the MS2 noise as the median of the intensities found in an mz
     * window The isobaric labels intensities are deleted for this noise
     * calculation
     *
     * @param scan Scan where the noise will be calculated
     * @param isobIntensities Isobaric labels intensities
     * @return Noise
     * @throws DataFormatException
     */
    private double calculateNoise(Scan scan, List<Double> isobIntensities) throws DataFormatException {

        double lowMz = labels.get(0).getLowMz();
        double highMz;
        double noise = 0;

        if (labels.size() > 1) {
            highMz = labels.get(labels.size() - 1).getHighMz();
        } else {
            highMz = labels.get(0).getHighMz();
        }

        List<Double> intensities = scan.getIntensities(lowMz - 1, highMz + 1);

        // Using median
        //if (intensities.size() > 0) noise = MathUtils.Median(intensities);
        // Deleting isobaric label intensities
        for (int i = 0; i < isobIntensities.size(); ++i) {
            intensities.remove(isobIntensities.get(i));
        }

        // Using average
        if (intensities.size() > 0) {
            noise = avg(intensities);
        }

        return noise;

    }

    /**
     * Gets the isotope distribution mz that are compressed within a window of
     * mz
     *
     * @param lowMzLimit low mz limit
     * @param highMzLimit high mz limit
     * @param charge peptide charge
     * @param mz monoisotopic mz
     * @return List of mz
     */
    private List<Double> getIsotopeDistributionMz(double lowMzLimit, double highMzLimit, int charge, double mz) {

        double originalMz = mz;
        List<Double> isoDistMzs = new ArrayList<>();

        double mzDistance = (Double) ((Masses.MASS_ELEMENT_13C - Masses.MASS_ELEMENT_12C) / charge);
        mz -= mzDistance;

        if (mz >= lowMzLimit && mz <= highMzLimit) {
            isoDistMzs.add(mz);
        }

        boolean exceededMz = false;

        while (!exceededMz) {

            mz += mzDistance;
            if (mz >= lowMzLimit && mz <= highMzLimit) {
                isoDistMzs.add(mz);
            } else {
                exceededMz = true;
            }

        }

        // ascending mz
        return isoDistMzs;
    }

    /**
     * Gets the isotope distribution intensities that are compressed within a
     * window of mz and the intensities that are not part of the isotope
     * distribution. This is presented as a pair where the first is the isotope
     * intensities and the second the non-isotope
     *
     * @param peaks list of peaks
     * @param lowMzLimit low mz limit
     * @param highMzLimit high mz limit
     * @param charge peptide charge
     * @param mz monoisotopic mz
     * @return List of mz
     */
    private Pair<Double, Double> getIsotopeDistributionIntensities(List<Peak> peaks, double lowMz, double highMz, int precCharge, double precMz) {
        double isobIntensities = 0;
        double nonIsobIntensities = 0;

        boolean exceededMz = false;

        Peak peak;

        List<Double> isoDistMz = getIsotopeDistributionMz(lowMz, highMz, precCharge, precMz);

        List<Peak> mPeaks = new ArrayList<>(peaks);
        int methodOption = Scan.PeakSelection.peakSelectionToOptionValue(searchMethod);

        if (debug) {
            System.out.println("* Isotopic distribution in window " + lowMz + " to " + highMz + " + peak tolerance");
        }

        // obtainig the most similar peak to the isotopic
        for (Double isoMz : isoDistMz) {
            if (debug) {
                System.out.print(isoMz + " ");
            }
            peak = IsoUtils.getNearestPeakBy(methodOption, IsoUtils.getLowMz(isoMz, MS1PPMTolerance), IsoUtils.getHighMz(isoMz, MS1PPMTolerance), isoMz, mPeaks);
            if (peak != null) {
                isobIntensities += peak.getIntensity();
                mPeaks.remove(peak);    // removing already summed peak
                if (debug) {
                    System.out.print("found peak " + peak.getMZ() + " (" + peak.getIntensity() + ")");
                }
            }
            if (debug) {
                System.out.println("");
            }
        }

        // all remaining peaks within the window are non iso
        if (debug) {
            System.out.println("* Remaining peaks are:");
        }
        for (int i = 0; !exceededMz && i < mPeaks.size(); ++i) {
            peak = mPeaks.get(i);
            if (peak.getMZ() >= lowMz && peak.getMZ() <= highMz) {
                nonIsobIntensities += peak.getIntensity();
                if (debug) {
                    System.out.println(peak.getMZ() + " (" + peak.getIntensity() + ")");
                }
            } else if (peak.getMZ() > highMz) {
                exceededMz = true;
            }
        }

        if (debug) {
            System.out.println("iso intensities sum: " + isobIntensities + " non: " + nonIsobIntensities);
        }
        return new Pair(isobIntensities, nonIsobIntensities);
    }

    /**
     * Calculates the peptide score
     *
     * @param scans ms Scans
     * @param ms2Scan ms2 scans (where the precursor will be found)
     * @return score
     * @throws DataFormatException
     */
    private double calculateIsotopeDistributionScore(Map<Integer, Scan> scans, Scan ms2Scan) throws DataFormatException {

        double score = 0;

        List<Precursor> precursors = ms2Scan.getPrecursors();

        if (precursors.size() > 0) {

            double precMz = precursors.get(0).getMZ();
            int precCharge = precursors.get(0).getCharge();

            // Gets the MS1 scan
            Scan scan = scans.get(precursors.get(0).getPrecursorScanNum());

            if (scan != null) {

                List<Peak> peaks = scan.getPeaks();

                double lowMz = IsoUtils.getLowMz(precMz, MS1PPMTolerance) - 1;
                double highMz = IsoUtils.getHighMz(precMz, MS1PPMTolerance) + 1;

                Pair<Double, Double> isoIntensities = getIsotopeDistributionIntensities(peaks, lowMz, highMz, precCharge, precMz);
                double isobIntensities = isoIntensities.getFirst();
                double nonIsobIntensities = isoIntensities.getSecond();

                if (isobIntensities == 0) {
                    score = 0;
                } else {
                    score = isobIntensities / (isobIntensities + nonIsobIntensities);
                }

            }

        }

        return score;

    }

    /**
     * Calculates the reporters intensity score
     *
     * @param msnScan msn scan
     * @return score
     * @throws DataFormatException
     */
    private double calculateReportersIntensityScore(Scan msnScan, List<Double> isobIntensitiesList) throws DataFormatException {

        double lowMz = labels.get(0).getLowMz();
        double highMz;
        double score = 0;

        if (labels.size() > 1) {
            highMz = labels.get(labels.size() - 1).getHighMz();
        } else {
            highMz = labels.get(0).getHighMz();
        }

        List<Peak> peaks = msnScan.getPeaks();
        Peak peak;

        double isobIntensities = 0;
        double allIntensities = 0;

        boolean exceededMz = false;
        boolean found;

        for (int i = 0; !exceededMz && i < peaks.size(); ++i) {

            peak = peaks.get(i);

            if (peak.getMZ() >= lowMz) {

                if (peak.getMZ() <= highMz) {
                    allIntensities += peak.getIntensity();
                } else {
                    exceededMz = true;
                }

            }

        }

        for (Double intensity : isobIntensitiesList) {
            isobIntensities += intensity;
        }

        if (isobIntensities == 0) {
            score = 0;
        } else {
            score = isobIntensities / allIntensities;
        }

        return score;

    }

    /**
     * Calculates the isobaric score
     *
     * @param scans ms Scans
     * @param ms2Scan ms2 scans (where the precursor will be found)
     * @return score
     * @throws DataFormatException
     */
    private double calculateReportersFoundScore(Scan msnScan) throws DataFormatException {

        int cnt = 0;
        double mz;

        int ppm = 0;
        if (msnScan.getMsLevel() == 1) {
            ppm = MS1PPMTolerance;
        } else if (msnScan.getMsLevel() == 2) {
            ppm = MS2PPMTolerance;
        } else {
            ppm = PPMTolerance;
        }

        for (IsobaricLabels.IsobaricLabel il : isoLabels.values()) {
            if (il != null) {
                mz = getReporterMass(il);
                Peak peak = msnScan.getNearestPeakBy(searchMethod, IsoUtils.getLowMz(mz, ppm), IsoUtils.getHighMz(mz, ppm), mz);
                if (peak != null) {
                    ++cnt;
                }

            }
        }

        if (cnt == 0) {
            return 0;
        }
        return ((double) cnt / isoLabels.size());

    }

    /**
     * Gets the next scan of the given level (2 or 3)
     *
     * @param scans run scans
     * @param currentScan current scan
     * @param level scan level that we are looking for
     * @return scan
     */
    private Scan getNextScan(Map<Integer, Scan> scans, int currentScan, int level) {
        Scan scan;
        for (int i = currentScan + 1; i < scans.size(); ++i) {
            scan = scans.get(i);
            if (scan != null) {
                if (scan.getMsLevel() == level) {
                    return scan;
                }
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * Gets the precursor from the scan filterLine
     *
     * @param scan scan the look at
     * @param level scan level
     * @return precursor value
     */
    private String getPrecursor(Scan scan, int level) {

        String fLine = scan.getFilterLine();
        String data[] = fLine.split("ms" + level);

        // ITMS + c NSI r d Full ms2 789.7575@cid30.00 [212.0000-2000.0000]
        // FTMS + c NSI sps d Full ms3 789.7575@cid30.00 531.4671@hcd55.00 [110.0000-2000.0
        if (data.length > 1) {
            String aux[] = data[1].trim().split("@");
            if (aux.length > 0) {
                return aux[0];		// first precursor
            }
        }

        return "";

    }

    /**
     * Looks for the scan in the given level that has the same precursor as the
     * one in the level above (ex: MS3 corresponding to the MS2 precursor)
     *
     * @param scans run scans
     * @param currentScan current scan (level before than the one that we are
     * looking for)
     * @param level level that we are expecting
     * @return scan
     */
    private Scan getSamePrecursorNextScan(Map<Integer, Scan> scans, int currentScan, int level) throws DataFormatException {

        Scan scan;
        Scan cScan = scans.get(currentScan);
        String filterLine = cScan.getFilterLine();

        String originalPrecursor = "";
        String currentPrecursor;

        int ms1DepthSearchCounter = 0;

        if (filterLine != null && !filterLine.equals("")) {
            originalPrecursor = getPrecursor(cScan, level - 1);
        }

        List<Precursor> cScanPrecursors = cScan.getPrecursors();

        for (int i = currentScan + 1; i < scans.size(); ++i) {
            scan = scans.get(i);
            if (scan != null) {

                if (scan.getMsLevel() == level) {
                    if (filterLine == null || filterLine.equals("")) {

                        if (scan.getMasterScan() == cScan.getScanNumber()) {
                            return scan;
                        }

                        List<Precursor> scanPrecursors = scan.getPrecursors();
                        for (Precursor csP : scanPrecursors) {
                            if (findInPeaks(csP, cScan.getPeaks())) {
                                return scan;
                            }
                        }
                    } else {
                        if (scan.getMasterScan() == cScan.getScanNumber()) {
                            return scan;
                        }

                        if (scan.getMasterScan() == 0) {
                            currentPrecursor = getPrecursor(scan, level);
                            if (currentPrecursor.equals(originalPrecursor)) {
                                return scan;
                            }
                        }
                    }
                } else if (scan.getMsLevel() == 1 && ms1DepthSearchCounter < ms1DepthSearch) {
                    ms1DepthSearchCounter++;

                } else if (scan.getMsLevel() == 1) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * Gets the MS1 scan of a given MS2 scan number
     *
     * @param scans run scans
     * @param ms2ScanNum int MS2 scan number
     * @return int MS1 scan number if found, otherwise -1
     */
    private Scan getMS1ScanFromMS2(Map<Integer, Scan> scans, int ms2ScanNum) {

        if (mzFileType.equals("mzML")) {
            Scan ms2Scan = scans.get(ms2ScanNum);
            return scans.get(ms2Scan.getMasterScan());
        }

        if (scans.size() > 0) {
            for (int i = ms2ScanNum - 1; i > 0; i--) {
                Scan s = scans.get(i);
                if (s != null && s.getMsLevel() == 1) {
                    return s;
                }
            }
        }

        return null;
    }

    private Scan getPreMS1Scan(Map<Integer, Scan> scans, Scan currentScan) {
        if (scans.size() > 0) {
            for (int i = currentScan.getScanNumber() - 1; i > 0; i--) {
                Scan s = scans.get(i);
                if (s != null && s.getMsLevel() == 1) {
                    return s;
                }
            }
        }

        return null;
    }

    private Scan getPostMS1Scan(Map<Integer, Scan> scans, Scan currentScan) {
        if (scans.size() > 0) {
            for (int i = currentScan.getScanNumber() + 1; i < scans.size(); i++) {
                Scan s = scans.get(i);
                if (s != null && s.getMsLevel() == 1) {
                    return s;
                }
            }
        }

        return null;
    }

    private double getMS1PrecursorWeightedSignal(Map<Integer, Scan> scans, int targetScan, double precMz, int charge) throws DataFormatException {

        Scan pivotScan = scans.get(targetScan);
        Scan currentMs1Scan = getMS1ScanFromMS2(scans, targetScan);

        float pivotRT = pivotScan.getRetentionTime();
        float currentRT = currentMs1Scan.getRetentionTime();

        Scan preMs1Scan = getPreMS1Scan(scans, currentMs1Scan);

        float dPivotPre = 0;
        if (preMs1Scan != null) {
            float preRT = preMs1Scan.getRetentionTime();
            dPivotPre = abs(pivotRT - preRT);
        }

        Scan postMs1Scan = getPostMS1Scan(scans, currentMs1Scan);

        float dPivotPost = 0;
        if (postMs1Scan != null) {
            float postRT = postMs1Scan.getRetentionTime();
            dPivotPost = abs(postRT - pivotRT);
        }

        float dPivotCurrent = abs(pivotRT - currentRT);

        float totalDistances = dPivotCurrent + dPivotPost + dPivotPre;

        double preSignal = preMs1Scan != null ? getMS1PrecursorSignalPerc(scans, targetScan, precMz, charge, preMs1Scan) : 0;
        double currentSignal = getMS1PrecursorSignalPerc(scans, targetScan, precMz, charge, currentMs1Scan);
        double postSignal = postMs1Scan != null ? getMS1PrecursorSignalPerc(scans, targetScan, precMz, charge, postMs1Scan) : 0;

        return (dPivotCurrent / totalDistances) * currentSignal + (dPivotPre / totalDistances) * preSignal + (dPivotPost / totalDistances) * postSignal;
    }

    /**
     * Intensity of the precursor percentage from the total intensity in a given
     * window (MS1)
     *
     * @param scans run scans
     * @param ms2ScanN ms2 scan
     * @param precMz precursor mz
     * @param charge precursor charge
     * @return percentage
     * @throws DataFormatException
     */
    private double getMS1PrecursorSignalPerc(Map<Integer, Scan> scans, int ms2ScanN, double precMz, int charge) throws DataFormatException {

        Scan ms1Scan = getMS1ScanFromMS2(scans, ms2ScanN);

        return getMS1PrecursorSignalPerc(scans, ms2ScanN, precMz, charge, ms1Scan);
    }

    /**
     * Intensity of the precursor percentage from the total intensity in a given
     * window (MS1)
     *
     * @param scans run scans
     * @param ms2ScanN ms2 scan
     * @param precMz precursor mz
     * @param charge precursor charge
     * @return percentage
     * @throws DataFormatException
     */
    private double getMS1PrecursorSignalPerc(Map<Integer, Scan> scans, int ms2ScanN, double precMz, int charge, Scan ms1Scan) throws DataFormatException {

        double signalPerc = 0;

        if (ms1Scan != null) {

            List<Peak> peaks = ms1Scan.getPeaks();

            double allIntensities = 0;

            double lowMz = precMz - ms1PrecWindowDaltons;
            double highMz = precMz + ms1PrecWindowDaltons;

            if (debug) {
                System.out.println("-- Precursor Signal Perc");
                System.out.println("precMz " + precMz + " window " + ms1PrecWindowDaltons + " low " + lowMz + " high " + highMz);
            }

            Pair<Double, Double> isoIntensities = getIsotopeDistributionIntensities(peaks, lowMz, highMz, charge, precMz);

            double isobIntensities = isoIntensities.getFirst();
            double nonIsobIntensities = isoIntensities.getSecond();

            // precursor is already added in isob intensities
            double precIntensity = isobIntensities;

            // all intensities is the sum of isob and non-isob intensities (contains all peaks within the specified window)
            allIntensities = isobIntensities + nonIsobIntensities;

            if (allIntensities > 0 && precIntensity > 0) {
                signalPerc = precIntensity / allIntensities;
                if (debug) {
                    System.out.println("all intensities " + allIntensities);
                    System.out.println("score " + signalPerc);
                }
            }

        }

        return signalPerc;

    }

    private List<Double> getMSNPrecursorIntensitiesInMS2(List<Precursor> precursors, Scan ms2Scan) throws DataFormatException {

        List<Double> precursorsInt = new ArrayList<>();
        List<Peak> mPeaks = new ArrayList<>(ms2Scan.getPeaks());
        int methodOption = Scan.PeakSelection.peakSelectionToOptionValue(searchMethod);
        Peak peak;

        // obtainig the most similar peak to the precursor (no repetitions)
        for (Precursor prec : precursors) {
            peak = IsoUtils.getNearestPeakBy(methodOption, IsoUtils.getLowMz(prec.getMZ(), MS2PPMTolerance), IsoUtils.getHighMz(prec.getMZ(), MS2PPMTolerance), prec.getMZ(), mPeaks);
            if (peak != null) {
                precursorsInt.add(peak.getIntensity());
                mPeaks.remove(peak);    // removing already added peak
            }
        }

        return precursorsInt;
    }

    private List<Double> getMSNPrecursorWindowIntensitiesInMS2(List<Precursor> precursors, Scan ms2Scan, int charge) throws DataFormatException {
        Set<Peak> matchingPeaks = new HashSet<>();
        List<Double> precursorsInt = new ArrayList<>();
        List<Peak> foundPeaks = new ArrayList<>();

        double tolerance = Math.abs(PeptideUtils.massToMZ(ms2PrecursorWindowTolerance, charge));

        for (Precursor p : precursors) {
            double lowMz = IsoUtils.getLowMz(p.getMZ() - tolerance, MS2PPMTolerance);
            double highMz = IsoUtils.getHighMz(p.getMZ() + tolerance, MS2PPMTolerance);
            foundPeaks = ms2Scan.getPeaks(lowMz, highMz);
            // the set will make sure that we are only adding the same peak once
            matchingPeaks.addAll(foundPeaks);
        }
        for (Peak p : matchingPeaks) {
            precursorsInt.add(p.getIntensity());
        }

        return precursorsInt;
    }

    private void removePrecursorsFromSPSList(List<Precursor> list, Scan scan) {
        if (scan != null) {
            List<Precursor> matchedPrecs = getMatchingMSPrecursors(list, scan);
            for (Precursor p : matchedPrecs) {
                list.remove(p);
            }
        }
    }

    /**
     * This function will remove all ms1 precursors from the list NOTE: it
     * should only be used for ms3 scans NOTE2: it will only work with mzMLs,
     * thus the need of other functions for retrocompatibility
     *
     * @param list
     */
    private void removeMS1Precursor(List<Precursor> list) {
        List<Precursor> matchedPrecs = new ArrayList<>();
        // obtaining all ms1 precursors
        for (Precursor p : list) {
            if (p.getPrecursorScanLevel() == 1) {
                matchedPrecs.add(p);
            }
        }
        // removing them
        for (Precursor p : matchedPrecs) {
            list.remove(p);
        }
    }

    private List<Precursor> getPrecursorsFromLevel(List<Precursor> list, int scanLevel) {
        List<Precursor> matchedPrecs = new ArrayList<>();
        for (Precursor p : list) {
            if (p.getPrecursorScanLevel() == scanLevel) {
                matchedPrecs.add(p);
            }
        }
        return matchedPrecs;
    }

    private List<Precursor> getMatchingMSPrecursors(List<Precursor> list, Scan scan) {
        List<Precursor> matchedPrecs = new ArrayList<>();
        if (scan != null) {
            List<Precursor> scanPrecs = scan.getPrecursors();
            for (Precursor pr : scanPrecs) {
                boolean found = false;
                for (int ii = 0; !found && ii < list.size(); ++ii) {
                    if (list.get(ii).getMZ() == pr.getMZ()) {
                        matchedPrecs.add(list.get(ii));
                        found = true;
                    }
                }
            }
        }
        return matchedPrecs;
    }

    private String getSPSMassesAsString(List<Precursor> list) {
        String masses = "";
        for (Precursor p : list) {
            masses += p.getMZ() + ";";
        }
        if (!masses.isEmpty()) {
            masses = masses.substring(0, masses.length() - 1);
        }
        return masses;
    }

    private MzFileFactory mzFileFactory = (MzFileFactory) FactoryProvider.getFactory("MzFile");
    private MzFile mzFile;

    private PeptideFileFactory peptideFileFactory = (PeptideFileFactory) FactoryProvider.getFactory("PeptideFile");
    private PeptideFile peptideFile;

    private List<Double> getReporterIonsMz() {
        List<Double> mzs = new ArrayList<>();
        for (LabelData label : labels) {
            mzs.add(label.getMz());
        }
        return mzs;
    }

    /**
     * For every search hit found, quantifies every label (Stores the peptide
     * and label information in two separate temporary files and uses them in
     * order to store the data into the DB)
     */
    public void quantify(DashboardController controller) throws ParserConfigurationException, SAXException, IOException, DataFormatException, MzMLUnmarshallerException {
        peptideFile = peptideFileFactory.create(peptideFilePath);
        List<Peptide> searchHits = peptideFile.getPeptides();

        if (searchHits != null) {

            try {
                if (controller != null) {
                    controller.reportAndLogException("Loading mz file");
                    controller.updateProgress(5);
                }

                mzFile = mzFileFactory.create(mzMLFilePath);
                mzFileType = mzFile.getFileType();

                Map<Integer, Scan> scans = mzFile.getScansMap();
                System.out.println("mz file loaded with " + scans.size() + " scans");

                // Creating temporary files to store the peptide and its labels data
                String token = (this.token != null) ? this.token : "";
                File filePep = new File(outputDirectory + token + "isopep.csv");

                if (filePep.getParentFile() != null) {
                    filePep.getParentFile().mkdirs();
                }

                File fileLab = new File(outputDirectory + token + "isolab.csv");
                File fileIso = new File(outputDirectory + token + "isopep_extra.csv");
                File fileFrag = new File(outputDirectory + token + "isofrag.csv");

                PrintWriter outPep = new PrintWriter(new FileWriter(filePep));
                PrintWriter outLab = new PrintWriter(new FileWriter(fileLab));
                PrintWriter outIso = new PrintWriter(new FileWriter(fileIso));
                PrintWriter outFrag = new PrintWriter(new FileWriter(fileFrag));
                String templatePep = "%d,%d,%d,%d,%f,%f";
                String templateLab = "%d,%d,%s,%f,%f,%d,%d,%d,%f,%f,%f,%d,%f";
                String templateIso = "%d,%d,%f,%f,%f,%f,%f,%f,%f,%d,%d,%f,%f,%f,%f,%f,%f,%f,%f,%d,%d,%d,%d,%f,%f,%f,%s";
                String templateFrag = "%d,%d,%d,%s,%d,%f,%f,%f,%s";

                if (controller != null) {
                    controller.reportAndLogException("Initializing isobaric labels");
                    controller.updateProgress(10);
                }

                initializeIsobaricLabels();

                System.out.println("Isobaric labels initialized");

                outPep.append("quantID,peptideID,xSearchID,ms2scan,noise,score,isotope\n");
                outLab.append("peptideID,quantID,labelName,mz,incMZ,firstScan,lastScan,numScans,score,area,maxIntensity,maxIntensityScan,maxIntensityRetT\n");
                outIso.append("quantID,peptideID,ms1PrecSIgnal,peptideIntensityScore,topXPeptidePeaksRatio,peptideTopXIntensityScore,topXIntensityFromTotalScore,peptideTopXIntensityFromTotalScore,topPeakIntensityScore,topPeakFromPeptide,topPeakFromNL,topPeakIntensityTopXScore,topPeakMass,msnTotalSignal,precTPIntRatio,precRepIntRatio,precTPNumRatio,precTotalSignal,totalSignalSPSWind,msnLevel,ms1ScanNum,ms2ScanNum,ms3ScanNum,ms1ScanRT,ms2ScanRT,ms3ScanRT,spsMz\n");
                outFrag.append("quantID,peptideID,charge,type,position,mz,mzDiff,intensity,matched\n");

                Scan scan = null;
                Scan ms3Scan = null;
                Scan ms2ScoreScan = null;
                Peptide peptide;
                LabelData label;
                String pepLine, labLine, isoLine, fragLine;
                double intensity, ppmVar, noise, score, mz, precSignal, ms2PeptideNoise;
                float retT;
                int peptideId, ms2scan;
                List<Double> isobIntensities;
                MS2ScoreCalc ms2ScoreCalc;
                List<Precursor> auxPrecList;
                List<Pair> excludedMzRanges;
                Pair auxMassRange;
                double mz1, mz2;

                if (recalcIntensities) {
                    if (controller != null) {
                        controller.reportAndLogException("Recalc intensities, preparing data sheet");
                        controller.updateProgress(10);
                    }
                    prepareDataSheet();
                }

                double[] labIntensities = new double[labels.size()];
                double[] labMz = new double[labels.size()];
                double[] labPPMVar = new double[labels.size()];

                String[] matchingIons;

                if (controller != null) {
                    controller.reportAndLogException("Quantifiying");
                    controller.updateProgress(50);
                }
                // For every scan

                System.out.println("searchHits size " + searchHits.size());
                int methodOption = Scan.PeakSelection.peakSelectionToOptionValue(searchMethod);

                for (int i = 0; i < searchHits.size(); ++i) {
                    peptide = searchHits.get(i);
                    peptideId = peptide.getPeptideID();
                    ms2scan = searchHits.get(i).getStartScan();

                    if (scanLevel == 2) {
                        scan = scans.get(ms2scan);
                    } else if (scanLevel == 3) {
                        scan = getSamePrecursorNextScan(scans, ms2scan, 3);
                        ms3Scan = scan;
                    }

                    if (scan != null) {
                        if (ms2scan == 18571 || ms2scan == 21631 || ms2scan == 18638 || ms2scan == 18662 || ms2scan == 24059
                                || ms2scan == 8786 || ms2scan == 14967 || ms2scan == 15249 || ms2scan == 26678 || ms2scan == 36915) {
                            System.out.println("");
                            System.out.println("");
                            System.out.println("----------------------------------- SCAN " + ms2scan + " debug info -----------------------------------");
                            MS2ScoreCalc.debug = true;
                            debug = true;
                        } else {
                            MS2ScoreCalc.debug = false;
                            debug = false;
                        }

                        Scan ms1Scan;

                        ms1Scan = getMS1ScanFromMS2(scans, ms2scan);

                        Scan ms2Scan = scans.get(ms2scan);

                        retT = scan.getRetentionTime();
                        isobIntensities = new ArrayList<>();
                        double totalIsobIntensities = 0.0;

                        // Calculating every label data in a different line in the temporary file
                        for (int j = 0; j < labels.size(); ++j) {

                            label = labels.get(j);

                            Peak peak = scan.getNearestPeakBy(searchMethod, label.getLowMz(), label.getHighMz(), label.getMz());
                            if (peak != null) {
                                labIntensities[j] = peak.getIntensity();
                                labMz[j] = peak.getMZ();
                                labPPMVar[j] = peak.getPPMVariation(peak.getMZ());
                                isobIntensities.add(peak.getIntensity());
                                totalIsobIntensities += peak.getIntensity();
                            } else {
                                labIntensities[j] = labPPMVar[j] = 0;
                                labMz[j] = label.getMz();
                            }

                        }

                        // Recalculating intensities using product data sheet
                        if (recalcIntensities) {
                            labIntensities = productDataSheet.calcIsotopicDistributionIntensities(labIntensities);
                        }

                        // Storing only the filtered labels
                        for (int j = 0; j < labels.size(); ++j) {

                            label = labels.get(j);

                            if (filteredLabels.contains(label.getId())) {
                                labLine = String.format(templateLab, peptideId, quantId, label.getId(), labMz[j], labPPMVar[j], scan.getScanNumber(), scan.getScanNumber(), 1, 0.0, 0.0,
                                        labIntensities[j], ms2scan, retT);
                                outLab.append(labLine + "\n");
                            }

                        }

                        noise = calculateNoise(scan, isobIntensities);
                        if (null == scoreType) {
                            score = 0;
                        } else {
                            switch (scoreType) {
                                case ISOTOPE_DISTRIBUTION:
                                    score = getMS1PrecursorSignalPerc(scans, ms2scan, peptide.getMZ(), peptide.getCharge());
                                    break;
                                case REPORTERS_INTENSITY:
                                    score = calculateReportersIntensityScore(scan, isobIntensities);
                                    break;
                                case REPORTERS_FOUND:
                                    score = calculateReportersFoundScore(scan);
                                    break;
                                default:
                                    score = 0;
                                    break;
                            }
                        }

                        pepLine = String.format(templatePep, quantId, peptideId, peptide.getSearchID(), scan.getScanNumber(), noise, score);
                        outPep.append(pepLine + "\n");

                        if (hitImpurity.equals("weighted_avg")) {
                            precSignal = getMS1PrecursorWeightedSignal(scans, ms2scan, peptide.getMZ(), peptide.getCharge());
                        } else {
                            precSignal = getMS1PrecursorSignalPerc(scans, ms2scan, peptide.getMZ(), peptide.getCharge());
                        }

                        List<Precursor> precs = scan.getPrecursors();

                        if (debug) {
                            System.out.println("------- Precursors");
                            for (Precursor p : precs) {
                                System.out.println("- " + p.getMZ() + " from " + p.getPrecursorScanNum() + " " + p.getPrecursorScanLevel());

                            }
                        }

                        // we will exlude TMT reporter ions and MS1 precursor for MS2 score calc
                        List<Double> excludedMzs = getReporterIonsMz();
                        auxPrecList = getPrecursorsFromLevel(precs, 1);
                        auxPrecList.addAll(getMatchingMSPrecursors(precs, ms1Scan));    // for retrocompatibility
                        for (Precursor p : auxPrecList) {
                            excludedMzs.add(p.getMZ());
                            if (debug) {
                                System.out.println("- Excluded " + p.getMZ() + " from " + p.getPrecursorScanNum() + " " + p.getPrecursorScanLevel());
                            }
                        }

                        // we will obtain the complement ions ranges to be excluded
                        excludedMzRanges = new ArrayList<>();

                        for (IsobaricLabels.ComplementIonRange cir : complementIonsRanges) {
                            auxMassRange = cir.getMassCutoffs(peptide.getMZ(), peptide.getCharge());
                            mz1 = (Double) auxMassRange.getFirst();
                            mz2 = (Double) auxMassRange.getSecond();
                            // we need them from lowest to highest
                            if (mz1 < mz2) {
                                excludedMzRanges.add(new Pair(mz1, mz2));
                            } else {
                                excludedMzRanges.add(new Pair(mz2, mz1));
                            }
                        }

                        // removing precursors that are not sps
                        if (scanLevel > 2) {
                            removeMS1Precursor(precs);  // preferred way to do so. Not available for all files
                            removePrecursorsFromSPSList(precs, ms2Scan);
                        }
                        removePrecursorsFromSPSList(precs, ms1Scan);

                        // calculate MS2 variables
                        ms2ScoreScan = scan;
                        if (scanLevel != 2) {
                            ms2ScoreScan = scans.get(ms2scan);
                        }
                        ms2ScoreCalc = new MS2ScoreCalc(ms2ScoreScan, peptide, this.topXNum, MS2PPMTolerance, searchMods, generateFragmentIonsFile, additionalFragmentIons, excludedMzs, excludedMzRanges, methodOption);

                        matchingIons = ms2ScoreCalc.getMatchingFragmentIonList();

                        List<Double> precursorIntensities = getMSNPrecursorIntensitiesInMS2(precs, ms2Scan);
                        List<Double> precWindowIntensities = getMSNPrecursorWindowIntensitiesInMS2(precs, ms2Scan, peptide.getCharge());

                        double msnTotalSignal = totalIsobIntensities;
                        double precTotalSignal = MathUtils.Sum(precursorIntensities).doubleValue();
                        double totalSignalSPSWind = MathUtils.Sum(precWindowIntensities).doubleValue();
                        int numPeaksSPSWind = precWindowIntensities.size();
                        double matchingBYInt = ms2ScoreCalc.getPrecursorMatchingBYIonsIntenisty(precs, MS2PPMTolerance);    // SPS
                        double precTPIntRatio = precTotalSignal / totalSignalSPSWind;
                        double precRepIntRatio = matchingBYInt / totalSignalSPSWind;
                        double precTPNumRatio = (double) precursorIntensities.size() / numPeaksSPSWind;

                        isoLine = String.format(templateIso,
                                quantId,
                                peptideId,
                                precSignal,
                                ms2ScoreCalc.getPeptideIntensityScore(),
                                ms2ScoreCalc.getTopXPeptidePeaksRatio(),
                                ms2ScoreCalc.getPeptideTopXIntensityScore(),
                                ms2ScoreCalc.getTopXIntensityFromTotalScore(),
                                ms2ScoreCalc.getPeptideTopXIntensityFromTotalScore(),
                                ms2ScoreCalc.getTopPeakIntensityScore(),
                                (ms2ScoreCalc.isTopPeakFromPeptide()) ? 1 : 0,
                                (ms2ScoreCalc.isTopPeakFromPeptideNeutralLoss()) ? 1 : 0,
                                ms2ScoreCalc.getTopPeakIntensityTopXScore(),
                                ms2ScoreCalc.getTopPeakMass(),
                                msnTotalSignal,
                                precTPIntRatio,
                                precRepIntRatio,
                                precTPNumRatio,
                                precTotalSignal,
                                totalSignalSPSWind,
                                scanLevel,
                                (ms1Scan == null) ? 0 : ms1Scan.getScanNumber(),
                                ms2Scan.getScanNumber(),
                                (ms3Scan != null) ? ms3Scan.getScanNumber() : 0,
                                (ms1Scan == null) ? 0 : ms1Scan.getRetentionTime(),
                                ms2Scan.getRetentionTime(),
                                (ms3Scan != null) ? ms3Scan.getRetentionTime() : 0,
                                getSPSMassesAsString(precs)
                        );

                        outIso.append(isoLine + "\n");

                        if (generateFragmentIonsFile) {
                            for (FragmentIon fIon : ms2ScoreCalc.getFragmentIonsList()) {
                                fragLine = String.format(templateFrag, quantId, peptideId, fIon.charge, fIon.type, fIon.position,
                                        fIon.mz, fIon.mzDiff, fIon.intensity, fIon.matched);
                                outFrag.append(fragLine + "\n");
                            }
                            outFrag.flush();
                        }

                        if (i % 1000 == 0) {
                            outLab.flush();
                            outPep.flush();
                            outIso.flush();
                        }

                    }

                }

                outLab.flush();
                outPep.flush();
                outIso.flush();

                outLab.close();
                outPep.close();
                outIso.close();
                outFrag.close();

                if (controller != null) {
                    controller.reportAndLogException("Quantification completed");
                    controller.updateProgress(100);
                    controller.updateResultPaths(filePep.getAbsolutePath(), fileIso.getAbsolutePath(), fileLab.getAbsolutePath(), fileFrag.getAbsolutePath());
                    controller.enableResultComponents();
                }

                System.out.println("Generated files:");
                System.out.println(filePep.getAbsolutePath());
                System.out.println(fileLab.getAbsolutePath());
                System.out.println(fileIso.getAbsolutePath());
                System.out.println(fileFrag.getAbsolutePath());

            } catch (IOException | DataFormatException ex) {
                Logger.getLogger(IsobaricQuant.class.getName()).log(Level.SEVERE, null, ex);
                throw ex;
            }

        } else {
            System.out.println("No search hits found");
        }
    }

    public void setQuantId(int quantId) {
        this.quantId = quantId;
    }

    public void setMzMLFilePath(String mzMLFilePath) {
        this.mzMLFilePath = mzMLFilePath;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void setPeptideFilePath(String peptideFilePath) {
        this.peptideFilePath = peptideFilePath;
    }

}
