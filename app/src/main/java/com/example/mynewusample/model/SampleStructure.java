package com.example.mynewusample.model;

import android.text.TextUtils;

public class SampleStructure {
    private String sampleName, sampleLink, fileName, sampleCoverLink, note, sampleID;

    public SampleStructure() {
    }

    public SampleStructure(String sampleName, String sampleLink, String fileName, String sampleCoverLink, String note) {
        this.sampleName = sampleName;
        this.sampleLink = sampleLink;
        this.fileName = fileName;
        this.sampleCoverLink = sampleCoverLink;
        if(TextUtils.isEmpty(this.sampleCoverLink)){
            this.sampleCoverLink = "NONE";
        }
        this.note = note;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getSampleLink() {
        return sampleLink;
    }

    public void setSampleLink(String sampleLink) {
        this.sampleLink = sampleLink;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSampleCoverLink() {
        return sampleCoverLink;
    }

    public void setSampleCoverLink(String sampleCoverLink) {
        this.sampleCoverLink = sampleCoverLink;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
