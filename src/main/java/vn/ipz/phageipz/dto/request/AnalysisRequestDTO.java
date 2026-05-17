package vn.ipz.phageipz.dto.request;

import java.io.Serializable;

// Data Transfer Object
public class AnalysisRequestDTO implements Serializable {

    private String filename;      // File cần phân tích
    private Integer kmerSize;     // Có thể null nếu user để mặc định
    private Integer minLength;
    private Boolean casava;
    private Boolean svgOutput;

    // Constructor mặc định (Cần thiết để Jackson mapping JSON)
    public AnalysisRequestDTO() {}

    // --- GETTERS & SETTERS ---

    public String getFilename() {return filename;}
    public void setFilename(String filename) { this.filename = filename; }
    public Integer getKmerSize() { return kmerSize;}
    public void setKmerSize(Integer kmerSize) { this.kmerSize = kmerSize; }
    public Integer getMinLength() { return minLength; }
    public void setMinLength(Integer minLength) { this.minLength = minLength; }
    public Boolean getCasava() { return casava; }
    public void setCasava(Boolean casava) { this.casava = casava;  }
    public Boolean getSvgOutput() { return svgOutput; }
    public void setSvgOutput(Boolean svgOutput) { this.svgOutput = svgOutput; }

    @Override
    public String toString() {
        return "AnalysisRequestDTO{" +
                "filename='" + filename + '\'' +
                ", kmerSize=" + kmerSize +
                ", minLength=" + minLength +
                ", casava=" + casava +
                ", svgOutput=" + svgOutput +
                '}';
    }
}