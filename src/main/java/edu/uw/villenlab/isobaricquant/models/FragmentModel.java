package edu.uw.villenlab.isobaricquant.models;

public class FragmentModel {
    
    private final int quantID;
    private final int peptideID;
    private final double charge;
    private final String type;
    private final int position;
    private final double mz;
    private final double mzDiff;
    private final double intensity;
    private final String matched;

    public FragmentModel(int quantID, int peptideID, double charge, String type, int position, double mz, double mzDiff, double intensity, String matched) {
        this.quantID = quantID;
        this.peptideID = peptideID;
        this.charge = charge;
        this.type = type;
        this.position = position;
        this.mz = mz;
        this.mzDiff = mzDiff;
        this.intensity = intensity;
        this.matched = matched;
    }  

    public int getQuantID() {
        return quantID;
    }

    public int getPeptideID() {
        return peptideID;
    }

    public double getCharge() {
        return charge;
    }

    public String getType() {
        return type;
    }

    public int getPosition() {
        return position;
    }

    public double getMz() {
        return mz;
    }

    public double getIntensity() {
        return intensity;
    }

    public String getMatched() {
        return matched;
    }

    public double getMzDiff() {
        return mzDiff;
    }
    
    
}
