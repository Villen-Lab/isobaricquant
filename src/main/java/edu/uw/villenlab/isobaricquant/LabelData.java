/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uw.villenlab.isobaricquant;

import edu.uw.VillenLab.ProteomeUtils.IsoUtils;

/**
 * Isobaric label data
 */
class LabelData implements Comparable<LabelData> {

    /**
     * Label identifier
     */
    private final String id;

    /**
     * MZ
     */
    private final double mz;

    /**
     * Low MZ having into account the ppm tolerance
     */
    private final double lowMz;

    /**
     * High MZ having into account the ppm tolerance
     */
    private final double highMz;

    /**
     * Creates LabelData calculating the Low and High MZ
     *
     * @param mz Label MZ
     * @param id
     */
    public LabelData(double mz, String id, int PPMTolerance) {
        this.id = id;
        this.mz = mz;
        this.lowMz = IsoUtils.getLowMz(mz, PPMTolerance);
        this.highMz = IsoUtils.getHighMz(mz, PPMTolerance);

    }

    /**
     * Gets the label MZ
     *
     * @return MZ
     */
    public double getMz() {
        return mz;
    }

    /**
     * Gets the low MZ (having into account the ppm tolerance)
     *
     * @return Low MZ
     */
    public double getLowMz() {
        return lowMz;
    }

    /**
     * Gets the high MZ (having into account the ppm tolerance)
     *
     * @return High MZ
     */
    public double getHighMz() {
        return highMz;
    }

    @Override
    public int compareTo(LabelData o) {
        double d = mz - o.mz;
        if (d < 0) {
            return -1;
        } else if (d > 0) {
            return 1;
        }
        return 0;
    }

    public String getId() {
        return id;
    }

}
