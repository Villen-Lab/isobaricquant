package edu.uw.villenlab.isobaricquant.models;

public class PeptideModel {

    // From isoPep
    private final int quantID;
    private final int peptideID;
    private final int searchID;
    private final int scanNumber;
    private final double noise;
    private final double score;
    // From isoPepExtra
    private final double precSignal;
    private final double intensityScore;
    private final double topXPeptidePeaksRatio;
    private final double topXPeptideIntensityScore;
    private final double topXIntensityFromTotalScore;
    private final double topXPeptideIntensityFromTotalScore;
    private final double topPeakIntensityScore;
    private final boolean isTopPeakFromPeptide;
    private final boolean isTopPeakFromPeptideNeutralLoss;
    private final double topPeakIntensityTopXScore;
    private final double topPeakMass;
    private final double msnTotalSignal;
    private final String precTPIntRatio;
    private final String precRepIntRatio;
    private final String precTPNumRatio;
    private final double precTotalSignal;
    private final double totalSignalSPSWind;
    private final int scanLevel;
    private final int ms1ScanNumber;
    private final int ms2ScanNumber;
    private final int ms3ScanNumber;
    private final double ms1RetentionTime;
    private final double ms2RetentionTime;
    private final double ms3RetentionTime;
    private final String SPSMasses;
    // From hits
    private final String reference;
    private final String sequence;
    private final double mz;
    private final int charge;

    public PeptideModel(int quantID, int peptideID, int searchID, int scanNumber, double noise, double score, double precSignal, double intensityScore, double topXPeptidePeaksRatio, double topXPeptideIntensityScore, double topXIntensityFromTotalScore, double topXPeptideIntensityFromTotalScore, double topPeakIntensityScore, boolean isTopPeakFromPeptide, boolean isTopPeakFromPeptideNeutralLoss, double topPeakIntensityTopXScore, double topPeakMass, double msnTotalSignal, String precTPIntRatio, String precRepIntRatio, String precTPNumRatio, double precTotalSignal, double totalSignalSPSWind, int scanLevel, int ms1ScanNumber, int ms2ScanNumber, int ms3ScanNumber, double ms1RetentionTime, double ms2RetentionTime, double ms3RetentionTime, String SPSMasses, String reference, String sequence, double mz, int charge) {
        this.quantID = quantID;
        this.peptideID = peptideID;
        this.searchID = searchID;
        this.scanNumber = scanNumber;
        this.noise = noise;
        this.score = score;
        this.precSignal = precSignal;
        this.intensityScore = intensityScore;
        this.topXPeptidePeaksRatio = topXPeptidePeaksRatio;
        this.topXPeptideIntensityScore = topXPeptideIntensityScore;
        this.topXIntensityFromTotalScore = topXIntensityFromTotalScore;
        this.topXPeptideIntensityFromTotalScore = topXPeptideIntensityFromTotalScore;
        this.topPeakIntensityScore = topPeakIntensityScore;
        this.isTopPeakFromPeptide = isTopPeakFromPeptide;
        this.isTopPeakFromPeptideNeutralLoss = isTopPeakFromPeptideNeutralLoss;
        this.topPeakIntensityTopXScore = topPeakIntensityTopXScore;
        this.topPeakMass = topPeakMass;
        this.msnTotalSignal = msnTotalSignal;
        this.precTPIntRatio = precTPIntRatio;
        this.precRepIntRatio = precRepIntRatio;
        this.precTPNumRatio = precTPNumRatio;
        this.precTotalSignal = precTotalSignal;
        this.totalSignalSPSWind = totalSignalSPSWind;
        this.scanLevel = scanLevel;
        this.ms1ScanNumber = ms1ScanNumber;
        this.ms2ScanNumber = ms2ScanNumber;
        this.ms3ScanNumber = ms3ScanNumber;
        this.ms1RetentionTime = ms1RetentionTime;
        this.ms2RetentionTime = ms2RetentionTime;
        this.ms3RetentionTime = ms3RetentionTime;
        this.SPSMasses = SPSMasses;
        this.reference = reference;
        this.sequence = sequence;
        this.mz = mz;
        this.charge = charge;
    }

    public int getQuantID() {
        return quantID;
    }

    public int getPeptideID() {
        return peptideID;
    }

    public int getSearchID() {
        return searchID;
    }

    public int getScanNumber() {
        return scanNumber;
    }

    public double getNoise() {
        return noise;
    }

    public double getScore() {
        return score;
    }

    public double getPrecSignal() {
        return precSignal;
    }

    public double getIntensityScore() {
        return intensityScore;
    }

    public double getTopXPeptidePeaksRatio() {
        return topXPeptidePeaksRatio;
    }

    public double getTopXPeptideIntensityScore() {
        return topXPeptideIntensityScore;
    }

    public double getTopXIntensityFromTotalScore() {
        return topXIntensityFromTotalScore;
    }

    public double getTopXPeptideIntensityFromTotalScore() {
        return topXPeptideIntensityFromTotalScore;
    }

    public double getTopPeakIntensityScore() {
        return topPeakIntensityScore;
    }

    public boolean isIsTopPeakFromPeptide() {
        return isTopPeakFromPeptide;
    }

    public boolean isIsTopPeakFromPeptideNeutralLoss() {
        return isTopPeakFromPeptideNeutralLoss;
    }

    public double getTopPeakIntensityTopXScore() {
        return topPeakIntensityTopXScore;
    }

    public double getTopPeakMass() {
        return topPeakMass;
    }

    public double getMsnTotalSignal() {
        return msnTotalSignal;
    }

    public String getPrecTPIntRatio() {
        return precTPIntRatio;
    }

    public String getPrecRepIntRatio() {
        return precRepIntRatio;
    }

    public String getPrecTPNumRatio() {
        return precTPNumRatio;
    }

    public double getPrecTotalSignal() {
        return precTotalSignal;
    }

    public double getTotalSignalSPSWind() {
        return totalSignalSPSWind;
    }

    public int getScanLevel() {
        return scanLevel;
    }

    public int getMs1ScanNumber() {
        return ms1ScanNumber;
    }

    public int getMs2ScanNumber() {
        return ms2ScanNumber;
    }

    public int getMs3ScanNumber() {
        return ms3ScanNumber;
    }

    public double getMs1RetentionTime() {
        return ms1RetentionTime;
    }

    public double getMs2RetentionTime() {
        return ms2RetentionTime;
    }

    public double getMs3RetentionTime() {
        return ms3RetentionTime;
    }

    public String getSPSMasses() {
        return SPSMasses;
    }

    public String getReference() {
        return reference;
    }

    public String getSequence() {
        return sequence;
    }

    public double getMz() {
        return mz;
    }

    public int getCharge() {
        return charge;
    }
    
    
}
