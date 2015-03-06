package com.trifork.conference.leadscanner;

public class Scan {
    String scanned, title, rawText, barcodeType;

    public Scan() {
    }

    public String getScanned() {
        return scanned;
    }

    public void setScanned(String scanned) {
        this.scanned = scanned;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getBarcodeType() {
        return barcodeType;
    }

    public void setBarcodeType(String barcodeType) {
        this.barcodeType = barcodeType;
    }

    @Override
    public String toString() {
        return "Scan{" +
                "scanned='" + scanned + '\'' +
                ", title='" + title + '\'' +
                ", rawText='" + rawText + '\'' +
                ", barcodeType='" + barcodeType + '\'' +
                '}';
    }
}
