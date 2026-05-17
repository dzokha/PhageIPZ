package vn.ipz.phageipz.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BasicStatsDTO(
    @JsonProperty("filename") String filename,
    @JsonProperty("fileType") String fileType,
    @JsonProperty("encoding") String encoding,
    @JsonProperty("totalSequences") long totalSequences,
    @JsonProperty("sequencesFlagged") long sequencesFlagged,
    @JsonProperty("sequenceLength") String sequenceLength, // <--- THÊM DÒNG NÀY
    @JsonProperty("gcContent") double gcContent
) {
    public BasicStatsDTO(String filename, String fileType) {
        this(filename, fileType, "Unknown", 0L, 0L, "0", 0.0); // Cập nhật lại constructor
    }
}