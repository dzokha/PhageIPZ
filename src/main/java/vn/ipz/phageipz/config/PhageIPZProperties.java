package vn.ipz.phageipz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.stream.Stream;

@Configuration
@ConfigurationProperties(prefix = "phageipz")
@Validated
public class PhageIPZProperties {

    @NotBlank(message = "Upload directory cannot be empty")
    private String uploadDir;
    
    @NotBlank(message = "Report directory cannot be empty")
    private String reportDir;

    @NotBlank(message = "Raw directory cannot be empty")
    private String rawDir;

    @NotBlank(message = "DB-File directory cannot be empty")
    private String dbFileDir;
    
    @Min(1)
    private int threadCount;
    
    private long maxFileSize;
    private boolean doUnzip;

    private final AnalysisDefaults analysis = new AnalysisDefaults();
    private final Logging logging = new Logging();
    private final QcThresholds qcThresholds = new QcThresholds();

    // --- Getters & Setters ---
    public String getUploadDir() { return uploadDir; }
    public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }

    public String getReportDir() { return reportDir; }
    public void setReportDir(String reportDir) { this.reportDir = reportDir; }

    public String getRawDir() { return rawDir; }
    public void setRawDir(String rawDir) { this.rawDir = rawDir; }

    public String getDbFileDir() { return dbFileDir; }
    public void setDbFileDir(String dbFileDir) { this.dbFileDir = dbFileDir; }

    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }

    public long getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }

    public boolean isDoUnzip() { return doUnzip; }
    public void setDoUnzip(boolean doUnzip) { this.doUnzip = doUnzip; }

    public AnalysisDefaults getAnalysis() { return analysis; }
    public Logging getLogging() { return logging; }
    public QcThresholds getQcThresholds() { return qcThresholds; }

    // --- Inner Classes ---

    public static class AnalysisDefaults {
        private int kmerSize;
        private int minLength;
        private int dupLength;
        private boolean casava;
        private boolean nofilter;
        private boolean nogroup;
        private boolean expgroup;
        private boolean svgOutput;
        private boolean ignoreSequenceLength;
        private String sequenceFormat;
        
        // Khớp với file yml: các file list nằm trong mục analysis
        private String adapterFile;
        private String contaminantFile;
        private String limitsFile;

        // --- Logic Validation cho Sequence Format ---
        public void setSequenceFormat(String sequenceFormat) {
            if (sequenceFormat == null || sequenceFormat.isBlank() || "null".equalsIgnoreCase(sequenceFormat)) {
                this.sequenceFormat = null;
                return;
            }
            String lower = sequenceFormat.toLowerCase().trim();
            if (lower.matches("fastq|sam|bam|sam_mapped|bam_mapped")) {
                this.sequenceFormat = lower;
            } else {
                throw new IllegalArgumentException("Định dạng '" + sequenceFormat + "' không được hỗ trợ.");
            }
        }

        // --- Getters & Setters ---
        public int getKmerSize() { return kmerSize; }
        public void setKmerSize(int kmerSize) { this.kmerSize = kmerSize; }
        public int getMinLength() { return minLength; }
        public void setMinLength(int minLength) { this.minLength = minLength; }
        public int getDupLength() { return dupLength; }
        public void setDupLength(int dupLength) { this.dupLength = dupLength; }
        public boolean isCasava() { return casava; }
        public void setCasava(boolean casava) { this.casava = casava; }
        public boolean isNofilter() { return nofilter; }
        public void setNofilter(boolean nofilter) { this.nofilter = nofilter; }
        public boolean isNogroup() { return nogroup; }
        public void setNogroup(boolean nogroup) { this.nogroup = nogroup; }
        public boolean isExpgroup() { return expgroup; }
        public void setExpgroup(boolean expgroup) { this.expgroup = expgroup; }
        public boolean isSvgOutput() { return svgOutput; }
        public void setSvgOutput(boolean svgOutput) { this.svgOutput = svgOutput; }
        public boolean isIgnoreSequenceLength() { return ignoreSequenceLength; }
        public void setIgnoreSequenceLength(boolean ignoreSequenceLength) { this.ignoreSequenceLength = ignoreSequenceLength; }
        public String getSequenceFormat() { return sequenceFormat; }
        public String getAdapterFile() { return adapterFile; }
        public void setAdapterFile(String adapterFile) { this.adapterFile = adapterFile; }
        public String getContaminantFile() { return contaminantFile; }
        public void setContaminantFile(String contaminantFile) { this.contaminantFile = contaminantFile; }
        public String getLimitsFile() { return limitsFile; }
        public void setLimitsFile(String limitsFile) { this.limitsFile = limitsFile; }
    }

    public static class QcThresholds {
        private double errorThreshold;
        private double warningThreshold;

        public double getErrorThreshold() { return errorThreshold; }
        public void setErrorThreshold(double errorThreshold) { this.errorThreshold = errorThreshold; }
        public double getWarningThreshold() { return warningThreshold; }
        public void setWarningThreshold(double warningThreshold) { this.warningThreshold = warningThreshold; }
    }

    public static class Logging {
        private String level;
        private String path;

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }
}