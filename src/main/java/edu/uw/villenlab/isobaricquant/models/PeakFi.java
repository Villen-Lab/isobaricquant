package edu.uw.villenlab.isobaricquant.models;

import edu.uw.villenlab.isobaricquant.FragmentIon;
import java.util.Comparator;

/**
 *
 * @author villenlab
 */
public class PeakFi {

    /**
     * Peak mz
     */
    public double mz;

    /**
     * Peak intensity
     */
    public double intensity;

    /**
     * If set, it's the fragment ion info. Null meaning it's not a fragment ion
     */
    public FragmentIon fragmentIon = null;

    /**
     * Indicates if this peak is a neutral loss
     */
    public boolean isNL = false;

    /**
     * Indicates if this peak is a fragment ion
     */
    public boolean isBYFragmentIon = false;

    /**
     * Comparator to sort by DESC intensity
     */
    public static Comparator<PeakFi> comparatorByDescIntensity = new Comparator<PeakFi>() {
        @Override
        public int compare(PeakFi p1, PeakFi p2) {
            return -Double.compare(p1.intensity, p2.intensity);
        }
    };

    public PeakFi(double mz, double intensity) {
        this.mz = mz;
        this.intensity = intensity;
    }

    public PeakFi(double mz, double intensity, FragmentIon fragmentIon) {
        this.mz = mz;
        this.intensity = intensity;
        this.fragmentIon = fragmentIon;
    }

    public PeakFi(double mz, double intensity, FragmentIon fragmentIon, boolean isNL) {
        this.mz = mz;
        this.intensity = intensity;
        this.fragmentIon = fragmentIon;
        this.isNL = isNL;
    }

    public PeakFi(double mz, double intensity, boolean isFragmentIon, boolean isNL) {
        this.mz = mz;
        this.intensity = intensity;
        this.isBYFragmentIon = isFragmentIon;
        this.isNL = isNL;
    }

    public PeakFi(double mz, double intensity, boolean isNL) {
        this.mz = mz;
        this.intensity = intensity;
        this.isNL = isNL;
    }

    public boolean isBorYFragmentIon() {
        return (fragmentIon != null && fragmentIon.isBorY()) || isBYFragmentIon;
    }

    public String getFragmentIonInfo() {
        if (fragmentIon == null) {
            return "";
        }
        return fragmentIon.getId() + " (" + fragmentIon.getMz() + ")";
    }

}
