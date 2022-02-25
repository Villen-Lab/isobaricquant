package edu.uw.villenlab.isobaricquant;

public class FragmentIon implements Comparable<FragmentIon> {

    char type;
    int charge;
    int position;
    double mz;
    double intensity;
    boolean matched;
    double mzDiff;

    public FragmentIon(char type, int charge, int position, double mz) {
        this.type = type;
        this.charge = charge;
        this.position = position;
        this.mz = mz;
        this.intensity = 0;
        this.matched = false;
    }

    public void setMatchedIntensity(double intensity) {
        this.intensity = intensity;
        this.matched = true;
    }

    public void setMzDifference(double foundMz) {
        this.mzDiff = Math.abs(mz - foundMz);
    }

    public boolean isFragmentIon(String id) {
        return id.equals(charge + type + position);
    }

    public String getId() {
        return "" + charge + type + position;
    }

    public boolean isBorY() {
        return type == 'b' || type == 'y';
    }

    public double getMz() {
        return mz;
    }

    @Override
    public int compareTo(FragmentIon o) {
        if (type == o.type) {
            if (charge == o.charge) {
                if (type == 'x' || type == 'y' || type == 'z') {
                    return Integer.compare(o.position, position);
                }
                return Integer.compare(position, o.position);
            }
            return Integer.compare(charge, o.charge);
        }
        return Character.compare(type, o.type);
    }

}
