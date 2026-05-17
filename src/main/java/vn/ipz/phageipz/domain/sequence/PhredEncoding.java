package vn.ipz.phageipz.domain.sequence;

public class PhredEncoding {
	
    private String name;
    private int offset;

    private static final int SANGER_ENCODING_OFFSET = 33;
    private static final int ILLUMINA_1_3_ENCODING_OFFSET = 64;

    private PhredEncoding(String name, int offset) {
        this.name = name;
        this.offset = offset;
    }

    public static PhredEncoding getFastQEncodingOffset(char lowestChar) {
        if (lowestChar < 33) {
            throw new IllegalArgumentException("Mã ASCII không hợp lệ (<33): " + (int) lowestChar);
        } else if (lowestChar < 64) {
            return new PhredEncoding("Sanger / Illumina 1.9", SANGER_ENCODING_OFFSET);
        } else if (lowestChar == 65) {
            return new PhredEncoding("Illumina 1.3", ILLUMINA_1_3_ENCODING_OFFSET);
        } else if (lowestChar <= 126) {
            return new PhredEncoding("Illumina 1.5", ILLUMINA_1_3_ENCODING_OFFSET);
        }
        throw new IllegalArgumentException("Mã ASCII không hợp lệ (>126): " + (int) lowestChar);
    }
    
    public static double convertSangerPhredToProbability(int phred) {
        return Math.pow(10, phred / -10.0);
    }

    public static double convertOldIlluminaPhredToProbability(int phred) {
        // Công thức ngược của Logit: p = 1 / (10^(Q/10) + 1)
        return 1.0 / (Math.pow(10, phred / 10.0) + 1.0);
    }

    public static int convertProbabilityToSangerPhred(double p) {
        if (p <= 0) return 93; 
        if (p >= 1) return 0;
        return (int) Math.round(-10.0 * Math.log10(p));
    }

    public static int convertProbabilityToOldIlluminaPhred(double p) {
        if (p <= 0) return 93;
        if (p >= 1) return 0;
        return (int) Math.round(-10.0 * Math.log10(p / (1.0 - p)));
    }

    public String name() { return name; }
    public int offset() { return offset; }
    @Override
    public String toString() { return name; }
}