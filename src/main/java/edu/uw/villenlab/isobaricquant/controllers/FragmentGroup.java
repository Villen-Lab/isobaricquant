package edu.uw.villenlab.isobaricquant.controllers;

/**
 *
 * @author Julian
 */
public class FragmentGroup {

    private final String ionType;
    private int count = 0;
    private String data = "";

    public FragmentGroup(String ionType) {
        this.ionType = ionType;
    }

    public void addData(String data) {
        this.data += data;
        this.count++;
    }

    public int getCount() {
        return count;
    }

    public String getData() {
        return data;
    }

    public String getIonType() {
        return this.ionType;
    }
}
