package vn.ipz.phageipz.engine.qc.modules;

import vn.ipz.phageipz.dto.response.BasicStatsDTO;
import vn.ipz.phageipz.domain.sequence.PhredEncoding;
import vn.ipz.phageipz.domain.sequence.Sequence;
import vn.ipz.phageipz.engine.qc.core.QCModule;

public class BasicStats implements QCModule {

    private String name = null;
    private long actualCount = 0;
    private long filteredCount = 0;
    private int minLength = 0;
    private int maxLength = 0;
    private long totalBases = 0;
    private long gCount = 0;
    private long cCount = 0;
    private long aCount = 0;
    private long tCount = 0;
    private long nCount = 0;
    private char lowestChar = 126;
    private String fileType = null;

    @Override
    public String description() {
        return "Calculates some basic statistics about the file";
    }
    
    @Override
    public boolean ignoreFilteredSequences() {
        return false;
    }

    public void reset() {
        name = null;            //mơi thêm
        actualCount = 0;
        filteredCount = 0;
        minLength = 0;
        maxLength = 0;
        totalBases = 0; 
        gCount = 0;
        cCount = 0;
        aCount = 0;
        tCount = 0;
        nCount = 0;
        lowestChar = 126;
        fileType = null;
    }

    public String name() {
        return "Basic Statistics";
    }
    
    public void setFileName(String name) {
        if (name == null) return;
        this.name = name.replaceFirst("stdin:", "");
    }

    public BasicStatsDTO getFullStatsForWeb() {
        String encoding = PhredEncoding.getFastQEncodingOffset(lowestChar).toString();
        long totalSequences = actualCount;
        long sequencesFlagged = filteredCount;
        
        long totalATGC = aCount + tCount + gCount + cCount;
        double gcContent = (totalATGC > 0) ? ((double)(gCount + cCount) * 100) / totalATGC : 0.0;

        String seqLength = (minLength == maxLength) ? String.valueOf(minLength) : minLength + "-" + maxLength;

        return new BasicStatsDTO(
            this.name, 
            this.fileType, 
            encoding, 
            totalSequences, 
            sequencesFlagged, 
            seqLength, // <--- BỔ SUNG BIẾN NÀY VÀO DTO
            gcContent
        );
    }




    @Override
    public Object getResultsPanel() {
        return getFullStatsForWeb(); 
    }

    /*
    * Đọc một dải DNA trong nhiều dải DNA
    * Cộng dồn độ dài DNA vào tolalBases
    * Cập nhật minLength, maxLength
    * Đếm hạt ATGC
    * Đo chất lượng Phred, hệ thống sẽ đoán được file của bạn đang dùng chuẩn Phred+33 hay Phred+64
    */
    public void processSequence(Sequence sequence) {
        if (name == null && sequence.file() != null) {
            setFileName(sequence.file().getName());
        }
        
        if (sequence.isFiltered()) {
            filteredCount++;
            return;
        }
        
        actualCount++;
        int currentLength = sequence.getSequence().length();
        totalBases += currentLength;
        
        if (fileType == null) {
            if (sequence.getColorspace()) {
                fileType = "Colorspace converted to bases";
            } else {
                fileType = "Conventional base calls";
            }
        }
        
        if (actualCount == 1) {
            minLength = currentLength;
            maxLength = currentLength;
        } else {
            if (currentLength < minLength) minLength = currentLength;
            if (currentLength > maxLength) maxLength = currentLength;
        }

        char[] chars = sequence.getSequence().toCharArray();
        for (char aChar : chars) {
            switch (aChar) {
                case 'G': ++gCount; break;
                case 'A': ++aCount; break;
                case 'T': ++tCount; break;
                case 'C': ++cCount; break;
                case 'N': ++nCount; break;
            }
        }
        
        char[] quals = sequence.getQualityString().toCharArray();
        for (char q : quals) {
            if (q < lowestChar) {
                lowestChar = q;
            }
        }
    }
    
    public boolean raisesError() { return false; }
    public boolean raisesWarning() { return false; }
    public boolean ignoreInReport() { return false; }
    
    /*
    * Chuyển đổi đơn vị Sinh học
    */
    public static String formatLength(long originalLength) {
        double length = originalLength;
        String unit = " bp";

        if (length >= 1000000000) {
            length /= 1000000000;
            unit = " Gbp";
        } else if (length >= 1000000) {
            length /= 1000000;
            unit = " Mbp";
        } else if (length >= 1000) {
            length /= 1000;
            unit = " kbp";
        }

        String rawLength = String.format("%.1f", length);
        if (rawLength.endsWith(".0")) {
            rawLength = rawLength.substring(0, rawLength.length() - 2);
        }
        
        return rawLength + unit;
    }
}