package vn.ipz.phageipz.engine.trimming;

public class TrimConfig {
    private int cutBases = 0; // -u (Cắt vô điều kiện: dương cắt 5', âm cắt 3')
    private int qualityCutoff = 20; // -q 20
    private String adapterSequence; // -a
    private int minLength = 20;     // -m 20
    private int minOverlap = 3;     // -O 3
    private double errorRate = 0.1; // -e 0.1
    private boolean trimPolyA = false; // --poly-a
    private boolean trimN = false;     // --trim-n

    public TrimConfig() {}

    public int getCutBases() { return cutBases; }
    public void setCutBases(int cutBases) { this.cutBases = cutBases; }

    public int getQualityCutoff() { return qualityCutoff; }
    public void setQualityCutoff(int qualityCutoff) { this.qualityCutoff = qualityCutoff; }

    public String getAdapterSequence() { return adapterSequence; }
    public void setAdapterSequence(String adapterSequence) { this.adapterSequence = adapterSequence; }

    public int getMinLength() { return minLength; }
    public void setMinLength(int minLength) { this.minLength = minLength; }

    public int getMinOverlap() { return minOverlap; }
    public void setMinOverlap(int minOverlap) { this.minOverlap = minOverlap; }

    public double getErrorRate() { return errorRate; }
    public void setErrorRate(double errorRate) { this.errorRate = errorRate; }

    public boolean isTrimPolyA() { return trimPolyA; }
    public void setTrimPolyA(boolean trimPolyA) { this.trimPolyA = trimPolyA; }

    public boolean isTrimN() { return trimN; }
    public void setTrimN(boolean trimN) { this.trimN = trimN; }
}