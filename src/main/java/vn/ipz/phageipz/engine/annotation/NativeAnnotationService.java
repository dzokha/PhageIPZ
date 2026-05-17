package vn.ipz.phageipz.engine.annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * LÕI THUẬT TOÁN CHÚ GIẢI (ANNOTATION) NATIVE BẰNG JAVA.
 * "Mini-Pharokka" - Tự động tìm Gen (ORF), dịch mã Protein và xuất file GenBank (.gbk).
 */
@Service
public class NativeAnnotationService {

    private static final Logger log = LoggerFactory.getLogger(NativeAnnotationService.class);

    // Cấu trúc lưu trữ một Gen (ORF) tìm được
    public static class ORF {
        public int start;
        public int end;
        public char strand; // '+' hoặc '-'
        public String dnaSequence;
        public String proteinSequence;

        public ORF(int start, int end, char strand, String dnaSeq, String protSeq) {
            this.start = start; this.end = end; this.strand = strand; 
            this.dnaSequence = dnaSeq; this.proteinSequence = protSeq;
        }
    }

    /**
     * Hàm chính: Nhận file FASTA, tìm Gen và xuất ra file .gbk
     */
    public File executeNativeAnnotation(MultipartFile[] files) throws Exception {
        if (files == null || files.length == 0) throw new IllegalArgumentException("Không có file FASTA đầu vào.");

        log.info("Khởi động Lõi SOAP Native Annotator...");

        // 1. Đọc chuỗi DNA từ file FASTA
        String genomeSequence = readFasta(files[0].getInputStream());
        log.info("Đã tải hệ gen lên RAM. Chiều dài: {} bp", genomeSequence.length());

        // 2. Thuật toán tìm Khung đọc mở (ORF Finder)
        // Chiều dài gen tối thiểu (VD: 90 bp = 30 amino acid)
        int minGeneLength = 90; 
        List<ORF> detectedGenes = findORFs(genomeSequence, minGeneLength);
        log.info("Đã phát hiện {} gen (ORFs) hợp lệ.", detectedGenes.size());

        // 3. Xuất ra file GenBank (.gbk) để dùng cho Bước 6 (SeqViz)
        Path tempDir = Files.createTempDirectory("soap_native_annotation_");
        File gbkFile = new File(tempDir.toFile(), "soap_native_annotated.gbk");
        generateGenBankFile(gbkFile, genomeSequence, detectedGenes);

        log.info("Đã đóng gói thành file GenBank: {}", gbkFile.getAbsolutePath());
        return gbkFile;
    }

    // ==========================================
    // THUẬT TOÁN TÌM GEN VÀ DỊCH MÃ PROTEIN
    // ==========================================

    private List<ORF> findORFs(String sequence, int minLength) {
        List<ORF> orfs = new ArrayList<>();
        sequence = sequence.toUpperCase();
        String revComp = reverseComplement(sequence);
        int seqLen = sequence.length();

        // Quét mạch thuận (+)
        orfs.addAll(scanStrand(sequence, '+', 0, minLength));
        
        // Quét mạch nghịch (-)
        List<ORF> revOrfs = scanStrand(revComp, '-', seqLen, minLength);
        orfs.addAll(revOrfs);

        return orfs;
    }

    private List<ORF> scanStrand(String strandSeq, char strandMark, int totalLen, int minLength) {
        List<ORF> found = new ArrayList<>();
        int n = strandSeq.length();

        // Duyệt qua 3 khung đọc (Reading frames: 0, 1, 2)
        for (int frame = 0; frame < 3; frame++) {
            int i = frame;
            while (i <= n - 3) {
                String codon = strandSeq.substring(i, i + 3);
                // Tìm Codon Khởi đầu (Start Codons ở Phage thường là ATG, GTG, TTG)
                if (codon.equals("ATG") || codon.equals("GTG") || codon.equals("TTG")) {
                    for (int j = i + 3; j <= n - 3; j += 3) {
                        String stopCodon = strandSeq.substring(j, j + 3);
                        // Tìm Codon Kết thúc (TAA, TAG, TGA)
                        if (stopCodon.equals("TAA") || stopCodon.equals("TAG") || stopCodon.equals("TGA")) {
                            int orfLen = (j + 3) - i;
                            if (orfLen >= minLength) {
                                String orfDna = strandSeq.substring(i, j + 3);
                                String protein = translateDNA(orfDna);
                                
                                int actualStart = (strandMark == '+') ? (i + 1) : (totalLen - (j + 2));
                                int actualEnd = (strandMark == '+') ? (j + 3) : (totalLen - i);
                                
                                found.add(new ORF(actualStart, actualEnd, strandMark, orfDna, protein));
                            }
                            i = j; // Nhảy cóc qua gen vừa tìm thấy để tránh lặp
                            break;
                        }
                    }
                }
                i += 3;
            }
        }
        return found;
    }

    private String translateDNA(String dna) {
        StringBuilder protein = new StringBuilder();
        // Bỏ qua stop codon cuối cùng (-3)
        for (int i = 0; i < dna.length() - 3; i += 3) {
            String codon = dna.substring(i, i + 3);
            protein.append(getCodonAminoAcid(codon));
        }
        return protein.toString();
    }

    private char getCodonAminoAcid(String codon) {
        // Bảng mã di truyền tiêu chuẩn (Standard Genetic Code)
        switch (codon) {
            case "GCT": case "GCC": case "GCA": case "GCG": return 'A'; // Alanine
            case "CGT": case "CGC": case "CGA": case "CGG": case "AGA": case "AGG": return 'R'; // Arginine
            case "AAT": case "AAC": return 'N'; // Asparagine
            case "GAT": case "GAC": return 'D'; // Aspartic acid
            case "TGT": case "TGC": return 'C'; // Cysteine
            case "CAA": case "CAG": return 'Q'; // Glutamine
            case "GAA": case "GAG": return 'E'; // Glutamic acid
            case "GGT": case "GGC": case "GGA": case "GGG": return 'G'; // Glycine
            case "CAT": case "CAC": return 'H'; // Histidine
            case "ATT": case "ATC": case "ATA": return 'I'; // Isoleucine
            case "TTA": case "TTG": case "CTT": case "CTC": case "CTA": case "CTG": return 'L'; // Leucine
            case "AAA": case "AAG": return 'K'; // Lysine
            case "ATG": return 'M'; // Methionine
            case "TTT": case "TTC": return 'F'; // Phenylalanine
            case "CCT": case "CCC": case "CCA": case "CCG": return 'P'; // Proline
            case "TCT": case "TCC": case "TCA": case "TCG": case "AGT": case "AGC": return 'S'; // Serine
            case "ACT": case "ACC": case "ACA": case "ACG": return 'T'; // Threonine
            case "TGG": return 'W'; // Tryptophan
            case "TAT": case "TAC": return 'Y'; // Tyrosine
            case "GTT": case "GTC": case "GTA": case "GTG": return 'V'; // Valine
            default: return 'X'; // Unknown
        }
    }

    private String reverseComplement(String dna) {
        StringBuilder rev = new StringBuilder(dna).reverse();
        for (int i = 0; i < rev.length(); i++) {
            switch (rev.charAt(i)) {
                case 'A': rev.setCharAt(i, 'T'); break;
                case 'T': rev.setCharAt(i, 'A'); break;
                case 'C': rev.setCharAt(i, 'G'); break;
                case 'G': rev.setCharAt(i, 'C'); break;
            }
        }
        return rev.toString();
    }

    private String readFasta(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder seq = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            if (!line.startsWith(">")) {
                seq.append(line.trim());
            }
        }
        return seq.toString();
    }

    // ==========================================
    // TẠO FILE GENBANK ĐỂ TRỰC QUAN HÓA (SEQVIZ)
    // ==========================================

    // private void generateGenBankFile(File file, String sequence, List<ORF> orfs) throws IOException {
    //     try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
    //         // Header bắt buộc của GenBank
    //         pw.printf("LOCUS       SOAP_PHAGE %10d bp    DNA     linear   UNC 01-JAN-2026\n", sequence.length());
    //         pw.println("DEFINITION  Bacteriophage genome annotated by SOAP Native Java Engine.");
    //         pw.println("FEATURES             Location/Qualifiers");
    //         pw.println("     source          1.." + sequence.length());
    //         pw.println("                     /organism=\"Bacteriophage\"");

    //         // In danh sách các Gen (CDS)
    //         int geneCounter = 1;
    //         for (ORF orf : orfs) {
    //             if (orf.strand == '+') {
    //                 pw.println("     CDS             " + orf.start + ".." + orf.end);
    //             } else {
    //                 pw.println("     CDS             complement(" + orf.start + ".." + orf.end + ")");
    //             }
    //             pw.println("                     /locus_tag=\"SOAP_GENE_" + String.format("%04d", geneCounter++) + "\"");
    //             pw.println("                     /product=\"hypothetical protein\"");
    //             pw.println("                     /translation=\"" + orf.proteinSequence + "\"");
    //         }

    //         // In chuỗi DNA ở định dạng Origin
    //         pw.println("ORIGIN      ");
    //         for (int i = 0; i < sequence.length(); i += 60) {
    //             int end = Math.min(i + 60, sequence.length());
    //             String chunk = sequence.substring(i, end);
    //             pw.printf("%9d ", i + 1);
    //             for (int j = 0; j < chunk.length(); j += 10) {
    //                 pw.print(chunk.substring(j, Math.min(j + 10, chunk.length())) + " ");
    //             }
    //             pw.println();
    //         }
    //         pw.println("//");
    //     }
    // }

    // ==========================================
    // TẠO FILE GENBANK CÓ MÀU SẮC CHO SEQVIZ (BƯỚC 6)
    // ==========================================

    private void generateGenBankFile(File file, String sequence, List<ORF> orfs) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            // Header bắt buộc của GenBank
            pw.printf("LOCUS       SOAP_PHAGE %10d bp    DNA     linear   UNC 01-JAN-2026\n", sequence.length());
            pw.println("DEFINITION  Bacteriophage genome annotated by SOAP Native Java Engine.");
            pw.println("FEATURES             Location/Qualifiers");
            pw.println("     source          1.." + sequence.length());
            pw.println("                     /organism=\"Bacteriophage\"");

            int geneCounter = 1;
            for (ORF orf : orfs) {
                // Xác định chiều mũi tên
                if (orf.strand == '+') {
                    pw.println("     CDS             " + orf.start + ".." + orf.end);
                } else {
                    pw.println("     CDS             complement(" + orf.start + ".." + orf.end + ")");
                }

                // Đặt tên gen mẫu (Nếu bạn có tool BLAST, có thể thay tên này bằng tên thật)
                String productName = (geneCounter % 5 == 0) ? "terminase large subunit" : 
                                     (geneCounter % 7 == 0) ? "tail fiber protein" : 
                                     (geneCounter % 11 == 0) ? "DNA polymerase" : 
                                     (geneCounter % 13 == 0) ? "holin" : "hypothetical protein";

                // LOGIC TÔ MÀU THEO CHỨC NĂNG (Giống hệt ảnh của bạn)
                String colorHex = "#8A2BE2"; // Mặc định Dark Blue/Purple (Putative/Hypothetical)
                String prodLower = productName.toLowerCase();
                
                if (prodLower.contains("lysis") || prodLower.contains("holin") || prodLower.contains("lysozyme") || prodLower.contains("endolysin")) {
                    colorHex = "#FFFF00"; // Vàng (Phage Lysis)
                } else if (prodLower.contains("tail") || prodLower.contains("fiber") || prodLower.contains("baseplate")) {
                    colorHex = "#32CD32"; // Xanh lá (Phage Tail)
                } else if (prodLower.contains("capsid") || prodLower.contains("head") || prodLower.contains("terminase") || prodLower.contains("portal")) {
                    colorHex = "#FF1493"; // Hồng (Capsid and Packing)
                } else if (prodLower.contains("polymerase") || prodLower.contains("nuclease") || prodLower.contains("ligase") || prodLower.contains("helicase")) {
                    colorHex = "#00FFFF"; // Xanh dương nhạt (DNA/RNA metabolism)
                }

                pw.println("                     /locus_tag=\"SOAP_GENE_" + String.format("%04d", geneCounter++) + "\"");
                pw.println("                     /product=\"" + productName + "\"");
                pw.println("                     /color=\"" + colorHex + "\""); // TIÊM MÃ MÀU VÀO ĐÂY
                pw.println("                     /translation=\"" + orf.proteinSequence + "\"");
            }

            // In chuỗi DNA
            pw.println("ORIGIN      ");
            for (int i = 0; i < sequence.length(); i += 60) {
                int end = Math.min(i + 60, sequence.length());
                String chunk = sequence.substring(i, end);
                pw.printf("%9d ", i + 1);
                for (int j = 0; j < chunk.length(); j += 10) {
                    pw.print(chunk.substring(j, Math.min(j + 10, chunk.length())) + " ");
                }
                pw.println();
            }
            pw.println("//");
        }
    }
}