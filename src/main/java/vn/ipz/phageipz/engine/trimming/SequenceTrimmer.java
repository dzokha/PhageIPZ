package vn.ipz.phageipz.engine.trimming;

import vn.ipz.phageipz.domain.sequence.Sequence;
import org.springframework.stereotype.Component;

/**
 * LÕI THUẬT TOÁN CẮT TỈA (NATIVE TRIMMER)
 * Thực hiện in-memory để tối ưu tốc độ và giám sát thông số như Cutadapt.
 */
@Component
public class SequenceTrimmer {

    /**
     * 1. Cắt base vô điều kiện (Cutadapt -u)
     * Số dương: cắt ở đầu 5'. Số âm: cắt ở đuôi 3'.
     */
    public int cutBases(Sequence seq, int cut) {
        if (cut == 0) return 0;
        String s = seq.getSequence();
        String q = seq.getQualityString();
        int basesTrimmed = 0;
        
        if (cut > 0 && cut < s.length()) { 
            basesTrimmed = cut;
            seq.setSequence(s.substring(cut));
            seq.setQualityString(q.substring(cut));
        } else if (cut < 0 && Math.abs(cut) < s.length()) { 
            int keepLen = s.length() + cut; 
            basesTrimmed = Math.abs(cut);
            seq.setSequence(s.substring(0, keepLen));
            seq.setQualityString(q.substring(0, keepLen));
        } else if (Math.abs(cut) >= s.length()) { 
            basesTrimmed = s.length();
            seq.setSequence("");
            seq.setQualityString("");
        }
        return basesTrimmed;
    }

    /**
     * 2. Thuật toán cắt theo Chất lượng (Quality Trimming - BWA)
     * Trả về số lượng base đã bị cắt bỏ.
     */
    public int trimByQuality(Sequence seq, int qualityCutoff) {
        String qualString = seq.getQualityString();
        if (qualString == null || qualString.isEmpty()) return 0;

        int minSum = 0;
        int currentSum = 0;
        int bestCutPosition = qualString.length();

        for (int i = qualString.length() - 1; i >= 0; i--) {
            int phredScore = qualString.charAt(i) - 33; 
            currentSum += (qualityCutoff - phredScore);

            if (currentSum > minSum) {
                minSum = currentSum;
                bestCutPosition = i;
            }
        }

        int basesTrimmed = 0;
        if (bestCutPosition < qualString.length()) {
            basesTrimmed = qualString.length() - bestCutPosition;
            seq.setSequence(seq.getSequence().substring(0, bestCutPosition));
            seq.setQualityString(qualString.substring(0, bestCutPosition));
        }
        return basesTrimmed;
    }

    /**
     * 3. Thuật toán cắt Adapter
     * Trả về số lượng base đã bị cắt bỏ.
     */
    public int trimAdapter(Sequence seq, String adapterSeq, int minOverlap, double allowedErrorRate) {
        if (adapterSeq == null || adapterSeq.isEmpty() || seq.getSequence().length() < minOverlap) return 0;

        String dna = seq.getSequence();
        int seqLen = dna.length();
        int adapterLen = adapterSeq.length();
        int bestCutIndex = -1;

        for (int i = 0; i <= seqLen - minOverlap; i++) {
            int overlapLength = Math.min(seqLen - i, adapterLen);
            String dnaPart = dna.substring(i, i + overlapLength);
            String adapterPart = adapterSeq.substring(0, overlapLength);

            int errors = calculateMismatches(dnaPart, adapterPart);
            
            if ((double) errors / overlapLength <= allowedErrorRate) {
                bestCutIndex = i;
                break;
            }
        }

        int basesTrimmed = 0;
        if (bestCutIndex != -1) {
            basesTrimmed = seqLen - bestCutIndex;
            seq.setSequence(dna.substring(0, bestCutIndex));
            seq.setQualityString(seq.getQualityString().substring(0, bestCutIndex));
        }
        return basesTrimmed;
    }

    /**
     * 4. Thuật toán cắt đuôi Poly-A
     */
    public int trimPolyA(Sequence seq) {
        String s = seq.getSequence();
        if (s.isEmpty()) return 0;

        int n = s.length();
        int bestIndex = n;
        int bestScore = 0;
        int score = 0;
        int errors = 0;

        for (int i = n - 1; i >= 0; i--) {
            char nuc = s.charAt(i);
            if (nuc == 'A' || nuc == 'a') {
                score += 1;
            } else {
                score -= 2;
                errors += 1;
            }

            if (score > bestScore && errors <= 0.2 * (n - i)) {
                bestIndex = i;
                bestScore = score;
            }
        }

        int basesTrimmed = 0;
        if (bestIndex < n) {
            basesTrimmed = n - bestIndex;
            seq.setSequence(s.substring(0, bestIndex));
            seq.setQualityString(seq.getQualityString().substring(0, bestIndex));
        }
        return basesTrimmed;
    }

    /**
     * 5. Thuật toán cắt N 2 đầu
     */
    public int trimN(Sequence seq) {
        String s = seq.getSequence();
        if (s.isEmpty()) return 0;

        int start = 0;
        int end = s.length();

        while (start < end && (s.charAt(start) == 'N' || s.charAt(start) == 'n')) {
            start++;
        }
        while (end > start && (s.charAt(end - 1) == 'N' || s.charAt(end - 1) == 'n')) {
            end--;
        }

        int basesTrimmed = 0;
        if (start > 0 || end < s.length()) {
            basesTrimmed = s.length() - (end - start);
            seq.setSequence(s.substring(start, end));
            seq.setQualityString(seq.getQualityString().substring(start, end));
        }
        return basesTrimmed;
    }

    private int calculateMismatches(String s1, String s2) {
        int mismatches = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (s1.charAt(i) != 'N' && s2.charAt(i) != 'N' && s1.charAt(i) != s2.charAt(i)) {
                mismatches++;
            }
        }
        return mismatches;
    }
}