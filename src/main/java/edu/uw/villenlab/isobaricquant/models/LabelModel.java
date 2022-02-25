package edu.uw.villenlab.isobaricquant.models;

//public class LabelModel implements Comparable<LabelModel> {
public class LabelModel {

    private final int quantID;
    private final int peptideID;
    private final String labelName;
    private final double mz;
    private final double incMZ;
    private final int firstScan;
    private final int lastScan;
    private final int numScans;
    private final double score;
    private final double area;
    private final double maxIntensity;
    private final int maxIntensityScan;
    private final double maxIntensityRetT;

    public LabelModel(int quantID, int peptideID, String labelName, double mz, double incMZ, int firstScan, int lastScan, int numScans,
            double score, double area, double maxIntensity, int maxIntensityScan, double maxIntensityRetT) {

        this.quantID = quantID;
        this.peptideID = peptideID;
        this.labelName = labelName;
        this.mz = mz;
        this.incMZ = incMZ;
        this.firstScan = firstScan;
        this.lastScan = lastScan;
        this.numScans = numScans;
        this.score = score;
        this.area = area;
        this.maxIntensity = maxIntensity;
        this.maxIntensityScan = maxIntensityScan;
        this.maxIntensityRetT = maxIntensityRetT;
    }

    /**
     *
     * @return
     */
    public int getQuantID() {
        return quantID;
    }

    /**
     *
     * @return
     */
    public int getPeptideID() {
        return peptideID;
    }

    /**
     *
     * @return
     */
    public String getLabelName() {
        return labelName;
    }

    /**
     *
     * @return
     */
    public double getMz() {
        return mz;
    }

    /**
     *
     * @return
     */
    public double getIncMZ() {
        return incMZ;
    }

    /**
     *
     * @return
     */
    public int getFirstScan() {
        return firstScan;
    }

    /**
     *
     * @return
     */
    public int getLastScan() {
        return lastScan;
    }

    /**
     *
     * @return
     */
    public int getNumScans() {
        return numScans;
    }

    /**
     *
     * @return
     */
    public double getScore() {
        return score;
    }

    /**
     *
     * @return
     */
    public double getArea() {
        return area;
    }

    /**
     *
     * @return
     */
    public double getMaxIntensity() {
        return maxIntensity;
    }

    /**
     *
     * @return
     */
    public int getMaxIntensityScan() {
        return maxIntensityScan;
    }

    /**
     *
     * @return
     */
    public double getMaxIntensityRetT() {
        return maxIntensityRetT;
    }

    /*@Override
    public int compareTo(LabelModel o) {

        if (mz < o.getMZ()) {
            return -1;
        } else if (mz > o.getMZ()) {
            return 1;
        } else {
            return 0;
        }
    }*/

}
