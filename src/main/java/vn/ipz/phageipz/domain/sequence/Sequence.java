package vn.ipz.phageipz.domain.sequence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.io.File;

public class Sequence implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private String sequence;
    private String quality;
    private final boolean isColorspace; 
    private boolean isFiltered = false;

    @JsonIgnore
    private File sourceFile;

    public Sequence(String id, String sequence, String quality, File sourceFile, boolean isColorspace) {
        this.id = id;
        this.sequence = (sequence != null) ? sequence.toUpperCase() : "";
        this.quality = (quality != null) ? quality : "";
        this.sourceFile = sourceFile;
        this.isColorspace = isColorspace;
    }

    public Sequence(String id, String sequence, String quality) {
        this(id, sequence, quality, null, false);
    }

    public String getSequence() { return sequence; }
    public String getQualityString() { return quality; }
    public String getID() { return id; }
    public File file() { return sourceFile; }
    public boolean getColorspace() { return isColorspace; }

    public String getId() { return id; }
    public String getQuality() { return quality; }
    public boolean isFiltered() { return isFiltered; }
    public void setFiltered(boolean filtered) { this.isFiltered = filtered; }
    public void setFile(File file) { this.sourceFile = file; }

    // THÊM 2 HÀM SETTER NÀY VÀO ĐỂ CẬP NHẬT DỮ LIỆU SAU KHI CẮT (TRIMMING)
    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public void setQualityString(String quality) {
        this.quality = quality;
    }
}