package edu.uw.villenlab.isobaricquant;

import villeninputs.Peptide;
import edu.uw.VillenLab.Elements.Peak;
import edu.uw.VillenLab.MZParser.Elements.Scan;
import edu.uw.VillenLab.ProteomeUtils.PeptideUtils;
//import edu.uw.gs.villenlab.Zucchini.Core.Search.Peptide;
import edu.uw.VillenLab.Elements.FragmentIons;
import edu.uw.VillenLab.Elements.Modification;
import edu.uw.VillenLab.Elements.SearchModifications;
import edu.uw.VillenLab.MZParser.Elements.Precursor;
import edu.uw.VillenLab.ProteomeUtils.IsoUtils;
import static edu.uw.VillenLab.ProteomeUtils.IsoUtils.isMZInRange;
import edu.uw.villenlab.isobaricquant.models.PeakFi;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import org.apache.commons.math3.util.Pair;

/**
 *
 * @author Ariadna Llovet (allovet@uw.edu)
 */
public class MS2ScoreCalc {

    public double totalIntensity;
    public double totalFragmIonsIntensity;
    public int numFragmentIons;
    public double topXIntensity;
    public double topXFragmIonsIntensity;
    public int topXNum;
    public int topXFIons;	// number of fragment ions in top x
    public double topPeakIntensity;
    public double topPeakMass;
    public boolean topPeakFragmIon;
    public boolean topPeakNeutralLoss;

    public static boolean debug = false;

    private int ppm;
    private int nLIndex;
    private int charge;

    private Map<String, Double> peptideBYFIonsMap;
    String[] matchingFragmentIons;
    Map<Double, Double> matchingFragmentIonsMap;
    private SearchModifications searchMods;

    private boolean generateMathingFIonsList = false;
    private List<FragmentIon> fragmentIonsList;

    private Set<Character> additionalFragmentIons;
    private List<Double> excludedMzPeaks;   // reporter ions + MS1 precursor
    private List<Pair> complementIonsMzRanges;  // complement ions ranges (first lowest mz, second highest)
    private List<Peak> allMatchingFIonPeaks = new ArrayList<>();

    /* Search method option */
    private int searchOption;

    public MS2ScoreCalc(Scan scan, Peptide peptide, int topXNum, int ppm, SearchModifications searchMods, boolean generateFIonsList,
            Set<Character> additinalFragmentIons, List<Double> excludedMzPeaks, List<Pair> complementIonsMzRanges, int searchOption) {
        this.ppm = ppm;
        this.topXNum = topXNum;
        this.topXFIons = 0;
        this.totalIntensity = 0;
        this.totalFragmIonsIntensity = 0;
        this.numFragmentIons = 0;
        this.topXIntensity = 0;
        this.topXFragmIonsIntensity = 0;
        this.topPeakIntensity = 0;
        this.topPeakMass = 0;
        this.topPeakFragmIon = false;
        this.charge = peptide.getCharge();
        this.searchMods = searchMods;
        this.matchingFragmentIonsMap = new HashMap<>();
        this.generateMathingFIonsList = generateFIonsList;
        this.additionalFragmentIons = additinalFragmentIons;
        this.excludedMzPeaks = excludedMzPeaks;
        this.complementIonsMzRanges = complementIonsMzRanges;
        this.searchOption = searchOption;
        try {
            calcMS2IntensityVariables(scan, peptide, topXNum);
        } catch (DataFormatException ex) {
            Logger.getLogger(MS2ScoreCalc.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private List<Double> getFragmentIonsAndNLMZs(Peptide peptide) {

        String sequence = peptide.getSequence();
        List<Modification> peptideVarMods = IsoUtils.getPeptideVarModifications(sequence, searchMods.variableModifications);
        String cleanSeq = PeptideUtils.cleanSequence(sequence);
        double nMod = IsoUtils.calcNTermMass(sequence, searchMods);
        double cMod = IsoUtils.calcCTermMass(sequence, searchMods);
        int charge = peptide.getCharge();

        FragmentIons.debug = debug;
        FragmentIons fIons = new FragmentIons(cleanSeq, charge, cMod, nMod, peptideVarMods, searchMods.constantModifications, FragmentIons.MassType.MONO, additionalFragmentIons);

        if (debug) {
            System.out.println("");
            System.out.println("*****Fragment ions for peptide " + peptide.getSequence() + ": ");
            fIons.printFragmentIonsSorted();
        }

        peptideBYFIonsMap = fIons.getBAndYFragmentIons();
        fragmentIonsList = fragmentIonsMapToList(fIons.getFragmentIons());

        List<Double> fIonMzList = new ArrayList<>(peptideBYFIonsMap.values());
        nLIndex = fIonMzList.size();

        for (Double mass : searchMods.neutralLossesForFragmIons) {
            fIons = new FragmentIons(cleanSeq, charge, cMod, nMod, peptideVarMods, searchMods.constantModifications, FragmentIons.MassType.MONO, -mass, additionalFragmentIons);
            if (debug) {
                System.out.println("");
                System.out.println("*****Fragment ions for NL " + mass + ": ");
                fIons.printFragmentIonsSorted();
            }
            fIonMzList.addAll(fIons.getBAndYFragmentIons().values());
        }

        double nlMass;
        double peptideMass;
        if (debug) {
            peptideMass = IsoUtils.getPeptideMassDebug(sequence, searchMods.variableModifications, searchMods.constantModifications, cMod, nMod);
        } else {
            peptideMass = IsoUtils.getPeptideMass(sequence, searchMods.variableModifications, searchMods.constantModifications, cMod, nMod);
        }

        for (Double mass : searchMods.neutralLosses) {
            nlMass = peptideMass - mass;
            fIonMzList.add(PeptideUtils.massToMZ(nlMass, charge));
            if (debug) {
                System.out.println("");
                System.out.println("*****NL " + mass + "= " + PeptideUtils.massToMZ(nlMass, charge));
            }
        }

        if (debug) {
            System.out.println("");
            System.out.println("*****Peptide mass = " + peptideMass);
        }

        return fIonMzList;

    }

    private List<FragmentIon> fragmentIonsMapToList(Map<String, Double> map) {
        List<FragmentIon> fIonsList = new ArrayList<>();
        int charge, position;
        char type;
        FragmentIon fion;
        for (String key : map.keySet()) {
            charge = key.charAt(0) - '0';
            type = key.charAt(1);
            position = Integer.parseInt(key.substring(2));
            fion = new FragmentIon(type, charge, position, map.get(key));
            fIonsList.add(fion);
        }
        Collections.sort(fIonsList);
        return fIonsList;
    }

    private boolean isAnyNL(List<Double> fIonsMzs, double peakMz) {

        double mz;
        for (int i = nLIndex; i < fIonsMzs.size(); ++i) {
            mz = fIonsMzs.get(i);
            if (IsoUtils.isMZInRange(peakMz, mz, ppm)) {
                return true;
            }
        }

        return false;

    }

    /**
     * Returns if the given index it's from the NL
     *
     * @param index
     * @return
     */
    private boolean isAnyNL(int index) {
        return index >= nLIndex;
    }

    /**
     * Returns the position in the list of the matching fragment ion. If not
     * found -1
     *
     * @param fIons
     * @param peakMz
     * @param ppm
     * @return
     */
    public static int isAnyFragmentIon(List<FragmentIon> fIons, double peakMz, int ppm) {

        double mz;
        for (int i = 0; i < fIons.size(); ++i) {
            mz = fIons.get(i).mz;
            if (isMZInRange(peakMz, mz, ppm)) {
                return i;
            }
        }

        return -1;

    }

    private boolean isExcludedPeak(double mz) {
        for (Double excludedMz : excludedMzPeaks) {
            if (IsoUtils.isMZInRange(mz, excludedMz, ppm)) {
                return true;
            }
        }
        return false;
    }

    private void printPeaks(List<Peak> peaks) {
        System.out.println("");
        System.out.println("------- Peaks sorted by Mz");
        String output = "";
        for (Peak p : peaks) {
            output += p.getMZ() + " (" + p.getIntensity() + "), ";
        }
        System.out.println(output.substring(0, output.length() - 2));
    }

    private void printPeaksFI(List<PeakFi> peaks) {
        System.out.println("");
        System.out.println("");
        System.out.println("------- Peaks sorted by Intensity with Fragment ion matching info ");
        for (PeakFi p : peaks) {
            System.out.println(p.mz + " (" + p.intensity + ") " + p.getFragmentIonInfo());
        }
    }

    private void calcMS2IntensityVariables(Scan scan, Peptide peptide, int topX) throws DataFormatException {

        if (scan != null) {

            // generates fragment ions and nl
            List<Double> fIonMzList = getFragmentIonsAndNLMZs(peptide);

            String intMatch = "";
            String mzMatch = "";

            List<Peak> peaks = new ArrayList<>(scan.getPeaks());
            Peak peak;

            // We will create a new list with all the information needed to calculate scores
            List<PeakFi> peaksFI = new ArrayList<>();

            // We are going to find all BY fragment ions
            double mz;
            PeakFi pfi;

            if (debug) {
                printPeaks(peaks);
                System.out.println("");
                System.out.println("-------- BY Fragment Ions Matching");
            }
            String id;
            for (FragmentIon fi : fragmentIonsList) {
                if (fi.isBorY()) {
                    mz = fi.mz;
                    peak = IsoUtils.getNearestPeakBy(searchOption, IsoUtils.getLowMz(mz, ppm), IsoUtils.getHighMz(mz, ppm), mz, peaks);
                    if (peak != null) {
                        peaks.remove(peak);
                        // adding the matching info to the fragment ion
                        fi.setMatchedIntensity(peak.getIntensity());
                        fi.setMzDifference(peak.getMZ());
                        // we are adding the peak as a fragment ion
                        pfi = new PeakFi(peak.getMZ(), peak.getIntensity(), fi, isAnyNL(fIonMzList, peak.getMZ()));
                        peaksFI.add(pfi);
                        id = fi.getId();
                        if (debug) {
                            System.out.println(id + " (" + mz + ")" + " has been matched to: " + peak.getMZ() + " (" + peak.getIntensity() + ")");
                            if (pfi.isNL) {
                                System.out.println("* is NL");
                            }
                        }
                        // adding info for scores
                        totalIntensity += pfi.intensity;
                        totalFragmIonsIntensity += pfi.intensity;
                        numFragmentIons++;
                        mzMatch += id + "(" + mz + "),";
                        intMatch += id + "(" + pfi.intensity + "),";
                        matchingFragmentIonsMap.put(pfi.mz, pfi.intensity);
                        allMatchingFIonPeaks.add(peak);
                    }
                }
            }

            // We remove all excluded peaks that weren't fragment ions
            if (debug) {
                System.out.println("-------- Exclusions");
            }
            for (Double excludedMz : excludedMzPeaks) {
                peak = IsoUtils.getNearestPeakBy(searchOption, IsoUtils.getLowMz(excludedMz, ppm), IsoUtils.getHighMz(excludedMz, ppm), excludedMz, peaks);
                if (peak != null) {
                    peaks.remove(peak);
                    if (debug) {
                        System.out.println("Peak " + peak.getMZ() + " (" + peak.getIntensity() + ") has been excluded for: " + excludedMz);
                    }
                }
            }
            // removing all peaks within ranges
            List<Peak> peaksInWindow;
            for (Pair exclRange : complementIonsMzRanges) {
                peaksInWindow = IsoUtils.getPeaksInRange((Double) exclRange.getFirst(), (Double) exclRange.getSecond(), peaks);
                for (Peak p : peaksInWindow) {
                    peaks.remove(p);
                }
                if (debug) {
                    System.out.println("Removed " + peaksInWindow.size() + " peaks for complement ions located from " + ((Double) exclRange.getFirst()) + " to " + ((Double) exclRange.getSecond()));
                }
            }

            // checking all neutral losses B and Y fragment ions
            for (int i = nLIndex; i < fIonMzList.size(); ++i) {
                mz = fIonMzList.get(i);
                peak = IsoUtils.getNearestPeakBy(searchOption, IsoUtils.getLowMz(mz, ppm), IsoUtils.getHighMz(mz, ppm), mz, peaks);
                if (peak != null) {
                    peaks.remove(peak);
                    // we are adding the peak as a fragment ion with neutral loss
                    pfi = new PeakFi(peak.getMZ(), peak.getIntensity(), true, true);
                    peaksFI.add(pfi);
                    if (debug) {
                        System.out.println("NL Fr.Ion (" + mz + ")" + " has been matched to: " + peak.getMZ() + " (" + peak.getIntensity() + ")");
                    }
                    // adding info for scores
                    totalIntensity += pfi.intensity;
                    totalFragmIonsIntensity += pfi.intensity;
                    numFragmentIons++;
                    matchingFragmentIonsMap.put(pfi.mz, pfi.intensity);
                    allMatchingFIonPeaks.add(peak);
                }
            }

            // We are checking all the other requested fragment ions
            for (FragmentIon fi : fragmentIonsList) {
                if (!fi.isBorY()) { // b and y they have already been calculated
                    peak = IsoUtils.getNearestPeakBy(searchOption, IsoUtils.getLowMz(fi.mz, ppm), IsoUtils.getHighMz(fi.mz, ppm), fi.mz, peaks);
                    if (peak != null) {
                        peaks.remove(peak);
                        // adding the matching info to the fragment ion
                        fi.setMatchedIntensity(peak.getIntensity());
                        fi.setMzDifference(peak.getMZ());
                        // we are adding the peak as a fragment ion
                        pfi = new PeakFi(peak.getMZ(), peak.getIntensity(), fi, isAnyNL(fIonMzList, peak.getMZ()));
                        peaksFI.add(pfi);
                        if (debug) {
                            id = fi.getId();
                            System.out.println(id + " (" + fi.mz + ")" + " has been matched to: " + peak.getMZ() + " (" + peak.getIntensity() + ")");
                            if (pfi.isNL) {
                                System.out.println("* is NL");
                            }
                        }
                        totalIntensity += pfi.intensity;
                    }
                }
            }

            // We are adding all remaining peaks to the list with info regarding NL
            for (Peak p : peaks) {
                pfi = new PeakFi(p.getMZ(), p.getIntensity(), isAnyNL(fIonMzList, p.getMZ()));
                peaksFI.add(pfi);
                if (debug) {
                    if (pfi.isNL) {
                        System.out.println("* " + p.getMZ() + " (" + p.getIntensity() + ") is NL");
                    }
                }
                // adding info for scores
                totalIntensity += pfi.intensity;
            }

            // We are sorting the list by DESC intensity (highest to lowest) to be able to calculate scores
            Collections.sort(peaksFI, PeakFi.comparatorByDescIntensity);

            // at this point we only need to calculate the scores related to the topX
            for (int i = 0; i < topX && i < peaksFI.size(); ++i) {
                pfi = peaksFI.get(i);
                topXIntensity += pfi.intensity;
                if (pfi.isBorYFragmentIon()) {
                    topXFragmIonsIntensity += pfi.intensity;
                    ++topXFIons;
                }
                // scores for highest intensity
                if (i == 0) {
                    topPeakIntensity = pfi.intensity;
                    topPeakMass = PeptideUtils.mzToMass(pfi.mz, this.charge);
                    if (pfi.isBorYFragmentIon()) {
                        this.topPeakFragmIon = true;
                    }
                    if (pfi.isNL) {
                        this.topPeakNeutralLoss = true;
                        if (debug) {
                            System.out.println("++++ Top peak is neutral loss");
                        }
                    }
                }
            }

            if (!mzMatch.isEmpty()) { // removing last comma
                mzMatch = mzMatch.substring(0, mzMatch.length() - 1);
                intMatch = intMatch.substring(0, intMatch.length() - 1);
            }
            matchingFragmentIons = new String[]{mzMatch, intMatch};

            if (debug) {
                printPeaksFI(peaksFI);
                printScoreVars();
            }

        }

    }

    private void printScoreVars() {
        System.out.println("");
        System.out.println("------ SCORES");
        System.out.println("totalIntensity: " + totalIntensity);
        System.out.println("totalFragmIonsIntensity: " + totalFragmIonsIntensity);
        System.out.println("numFragmentIons: " + numFragmentIons);
        System.out.println("topXIntensity: " + topXIntensity);
        System.out.println("topXFragmIonsIntensity: " + topXFragmIonsIntensity);
        System.out.println("topXFIons: " + topXFIons);
        System.out.println("topPeakIntensity: " + topPeakIntensity);
        System.out.println("topPeakMass: " + topPeakMass);
        System.out.println("topPeakFragmIon: " + topPeakFragmIon);
        System.out.println("topPeakNeutralLoss: " + topPeakNeutralLoss);
        System.out.println("");
    }

    public String[] getMatchingFragmentIonList() {
        return matchingFragmentIons;
    }

    public Map<Double, Double> getMatchingFragmentIons() {
        return matchingFragmentIonsMap;
    }

    public double getPrecursorMatchingBYIonsIntenisty(List<Precursor> precursors, int ppm) {
        double sumIntensities = 0;
        List<Peak> mPeaks = new ArrayList<>(allMatchingFIonPeaks);
        Collections.sort(mPeaks);
        Peak peak;

        // obtainig the most similar peak to the precursor
        for (Precursor prec : precursors) {
            if (debug) {
                System.out.println("Searching matching peak for precursor " + prec.getMZ() + " from scan " + prec.getPrecursorScanNum() + " " + prec.getPrecursorScanLevel());
            }
            peak = IsoUtils.getNearestPeakBy(searchOption, IsoUtils.getLowMz(prec.getMZ(), ppm), IsoUtils.getHighMz(prec.getMZ(), ppm), prec.getMZ(), mPeaks);
            if (peak != null) {
                sumIntensities += peak.getIntensity();
                mPeaks.remove(peak);    // removing already summed peak
                if (debug) {
                    System.out.println("***Precursor (" + prec.getMZ() + ")" + " has been matched to: " + peak.getMZ() + " (" + peak.getIntensity() + ")");
                }

            }
        }

        if (debug) {
            System.out.println("");
            System.out.println("All peaks matching fragment ions are:");
            printData(allMatchingFIonPeaks);
        }

        return sumIntensities;
    }

    public void printData(List<Peak> list) {
        for (int i = 0; i < list.size(); ++i) {
            System.out.println("" + list.get(i).getMZ() + " (" + list.get(i).getIntensity() + ")");
        }
    }

    public List<FragmentIon> getFragmentIonsList() {
        return fragmentIonsList;
    }

    /**
     * **************************** SCORES *****************************
     */
    public double getPeptideIntensityScore() {
        double score = 0;
        if (totalFragmIonsIntensity > 0) {
            score = totalFragmIonsIntensity / totalIntensity;
        }
        return score;
    }

    public double getTopXPeptidePeaksRatio() {
        double score = 0;
        if (topXNum > 0) {
            score = ((double) topXFIons) / ((double) topXNum);
        }
        return score;
    }

    public double getPeptideTopXIntensityScore() {
        double score = 0;
        if (topXIntensity > 0) {
            score = topXFragmIonsIntensity / topXIntensity;
        }
        return score;
    }

    public double getTopXIntensityFromTotalScore() {
        double score = 0;
        if (totalIntensity > 0) {
            score = topXIntensity / totalIntensity;
        }
        return score;
    }

    public double getPeptideTopXIntensityFromTotalScore() {
        double score = 0;
        if (totalIntensity > 0) {
            score = topXFragmIonsIntensity / totalIntensity;
        }
        return score;
    }

    public double getTopPeakIntensityScore() {
        double score = 0;
        if (totalIntensity > 0) {
            score = topPeakIntensity / totalIntensity;
        }
        return score;
    }

    public double getTopPeakIntensityTopXScore() {
        double score = 0;
        if (totalIntensity > 0) {
            score = topPeakIntensity / topXIntensity;
        }
        return score;
    }

    public boolean isTopPeakFromPeptide() {
        return topPeakFragmIon;
    }

    public boolean isTopPeakFromPeptideNeutralLoss() {
        return topPeakNeutralLoss;
    }

    public double getTopPeakMass() {
        return topPeakMass;
    }

    public double getTotalFragmIonsIntensity() {
        return totalFragmIonsIntensity;
    }

    public int getNumFragmentIons() {
        return numFragmentIons;
    }

}
