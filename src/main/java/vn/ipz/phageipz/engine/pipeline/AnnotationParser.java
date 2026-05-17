package vn.ipz.phageipz.engine.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.ipz.phageipz.config.PhageIPZProperties;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class AnnotationParser {
    private static final Logger log = LoggerFactory.getLogger(AnnotationParser.class);
    private final PhageIPZProperties PhageIPZProperties;
    private Map<String, String[]> phrogDict = null;

    public AnnotationParser(PhageIPZProperties PhageIPZProperties) {
        this.PhageIPZProperties = PhageIPZProperties;
    }

    // Nạp từ điển phrog_annot_v4.tsv
    private void initDict() {
        if (phrogDict != null) return;
        phrogDict = new HashMap<>();
        try {
            Path dbDir = Paths.get(PhageIPZProperties.getDbFileDir());
            Path annotFile = dbDir.resolve("phrog_annot_v4.tsv");
            if (!Files.exists(annotFile)) annotFile = dbDir.resolve("phrog_annot.tsv");
            
            if (Files.exists(annotFile)) {
                log.info("📖 Đang nạp từ điển PHROGs...");
                Files.lines(annotFile).skip(1).forEach(line -> {
                    String[] parts = line.split("\t");
                    if (parts.length >= 4) {
                        String id = "phrog_" + parts[0].trim(); 
                        String product = parts[2].trim();
                        String function = parts[3].trim();
                        
                        // 🚀 CHUẨN HÓA DỮ LIỆU ĐẠT CHUẨN NCBI GENBANK
                        if (product.equalsIgnoreCase("NA") || product.equalsIgnoreCase("unknown") || product.isEmpty()) {
                            product = "hypothetical protein"; // Tên khoa học tiêu chuẩn
                        }
                        if (function.equalsIgnoreCase("NA") || function.equalsIgnoreCase("unknown") || function.isEmpty()) {
                            function = "unknown function";
                        }
                        
                        phrogDict.put(id, new String[]{function, product});
                    }
                });
            }
        } catch (Exception e) {
            log.error("❌ Lỗi nạp từ điển: {}", e.getMessage());
        }
    }

    public void parseTsvAndMerge(Path tsvPath, Map<String, GeneAnnotation> geneMap, String dbName) throws IOException {
        if (!Files.exists(tsvPath)) return;
        if (dbName.equals("PHROGs")) initDict(); 
        
        Files.lines(tsvPath).forEach(line -> {
            String[] parts = line.split("\t");
            if (parts.length < 3) return;

            String geneId = parts[0]; 
            String rawHitId = parts[1];
            
            try {
                double evalue = Double.parseDouble(parts[2]);

                if (geneMap.containsKey(geneId) && evalue <= 1e-5) {
                    GeneAnnotation g = geneMap.get(geneId);
                    if (evalue < g.evalue) {
                        g.evalue = evalue;
                        g.sourceDb = dbName;
                        g.topHit = rawHitId;

                        if (dbName.equals("PHROGs")) {
                            String cleanPhrogId = rawHitId;
                            if (rawHitId.contains("##")) {
                                cleanPhrogId = rawHitId.split("##")[0].trim();
                            }
                            g.phrog = cleanPhrogId; 
                            
                            if (phrogDict != null && phrogDict.containsKey(cleanPhrogId)) {
                                g.function = phrogDict.get(cleanPhrogId)[0];
                                g.product = phrogDict.get(cleanPhrogId)[1];
                            } else {
                                // Nếu ID bị thiếu trong từ điển (Dữ liệu rác)
                                g.product = "hypothetical protein";
                                g.function = "unknown function";
                            }
                        } else if (dbName.equals("CARD")) {
                            g.function = "Antibiotic Resistance";
                            g.product = rawHitId;
                        } else if (dbName.equals("VFDB")) {
                            g.function = "Virulence Factor";
                            g.product = rawHitId;
                        }
                    }
                }
            } catch (Exception e) {}
        });
    }
}