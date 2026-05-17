package vn.ipz.phageipz.engine.pipeline;

import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Service
public class ResultAggregatorService {
    private final AnnotationParser annotationParser;

    public ResultAggregatorService(AnnotationParser annotationParser) {
        this.annotationParser = annotationParser;
    }

    public File createGenBank(Path jobDir, Path originalFasta, String genomeName) throws IOException {
        Map<String, GeneAnnotation> geneMap = new LinkedHashMap<>();

        // 1. Đọc GFF3 - Lấy ID chính xác 100% từ file của bạn
        Path gffPath = jobDir.resolve("predicted_genes.gff");
        if (Files.exists(gffPath)) {
            Files.lines(gffPath).filter(l -> !l.startsWith("#")).forEach(line -> {
                String[] parts = line.split("\t");
                if (parts.length >= 9) {
                    GeneAnnotation gene = new GeneAnnotation();
                    gene.start = Integer.parseInt(parts[3]);
                    gene.end = Integer.parseInt(parts[4]);
                    gene.strand = parts[6];
                    
                    // Lấy chính xác chuỗi "1_1" từ "ID=1_1;"
                    String attr = parts[8];
                    String exactId = attr.split(";")[0].replace("ID=", ""); 
                    
                    gene.id = exactId;
                    geneMap.put(exactId, gene);
                }
            });
        }

        // 2. Đọc FAA - Lấy chuỗi ">1_1"
        Path faaPath = jobDir.resolve("predicted_proteins.faa");
        if (Files.exists(faaPath)) {
            String currentId = null;
            StringBuilder seq = new StringBuilder();
            for (String line : Files.readAllLines(faaPath)) {
                if (line.startsWith(">")) {
                    if (currentId != null && geneMap.containsKey(currentId)) {
                        geneMap.get(currentId).translation = seq.toString();
                    }
                    // Bỏ dấu ">", lấy "1_1 # 1308..." -> "1_1"
                    currentId = line.split(" ")[0].substring(1);
                    seq = new StringBuilder();
                } else {
                    seq.append(line.trim());
                }
            }
            if (currentId != null && geneMap.containsKey(currentId)) {
                geneMap.get(currentId).translation = seq.toString();
            }
        }

        // 3. Chú giải từ TSV
        annotationParser.parseTsvAndMerge(jobDir.resolve("phrogs_result.tsv"), geneMap, "PHROGs");
        annotationParser.parseTsvAndMerge(jobDir.resolve("card_result.tsv"), geneMap, "CARD");
        annotationParser.parseTsvAndMerge(jobDir.resolve("vfdb_result.tsv"), geneMap, "VFDB");

        // 4. Xuất file GenBank
        String dnaSeq = readFastaSequence(originalFasta);
        File gbkFile = jobDir.resolve("annotated_genome.gbk").toFile();
        int count = 1;

        try (PrintWriter writer = new PrintWriter(new FileWriter(gbkFile))) {
            writer.printf("LOCUS       %-22s %d bp    DNA     linear   PHG 06-MAY-2026\n", "1", dnaSeq.length());
            writer.printf("DEFINITION  Bacteriophage annotated by SOAP.\n");
            writer.println("FEATURES             Location/Qualifiers");
            writer.printf("     source          1..%d\n", dnaSeq.length());
            writer.println("                     /organism=\"Bacteriophage\"");

            for (GeneAnnotation g : geneMap.values()) {
                String loc = g.strand.equals("+") ? g.start + ".." + g.end : "complement(" + g.start + ".." + g.end + ")";
                writer.println("     CDS             " + loc);
                writer.println("                     /locus_tag=\"SOAP_" + String.format("%04d", count++) + "\"");
                writer.println("                     /product=\"" + g.product + "\"");
                writer.println("                     /function=\"" + g.function + "\"");
                writer.println("                     /phrog=\"" + g.phrog + "\"");
                writer.println("                     /transl_table=11");
                
                if (g.translation != null && !g.translation.isEmpty()) {
                    writer.print("                     /translation=\"");
                    writer.print(formatTranslation(g.translation));
                    writer.println("\"");
                }
            }
            writer.println("ORIGIN");
            writer.print(formatSequence(dnaSeq));
            writer.println("//");
        }
        return gbkFile;
    }

    private String readFastaSequence(Path p) throws IOException {
        StringBuilder sb = new StringBuilder();
        Files.lines(p).filter(l -> !l.startsWith(">")).forEach(l -> sb.append(l.trim().toLowerCase()));
        return sb.toString();
    }

    private String formatTranslation(String s) {
        StringBuilder res = new StringBuilder();
        for (int i=0; i<s.length(); i+=45) {
            if (i>0) res.append("\n                     ");
            res.append(s.substring(i, Math.min(i+45, s.length())));
        }
        return res.toString();
    }

    private String formatSequence(String s) {
        StringBuilder res = new StringBuilder();
        for (int i=0; i<s.length(); i+=60) {
            res.append(String.format("%9d ", i+1));
            String chunk = s.substring(i, Math.min(i+60, s.length()));
            for (int j=0; j<chunk.length(); j+=10) res.append(chunk.substring(j, Math.min(j+10, chunk.length()))).append(" ");
            res.append("\n");
        }
        return res.toString();
    }
}