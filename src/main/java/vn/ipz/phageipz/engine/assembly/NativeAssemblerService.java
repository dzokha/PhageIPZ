package vn.ipz.phageipz.engine.assembly;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPInputStream;

@Service
public class NativeAssemblerService {

    private static final Logger log = LoggerFactory.getLogger(NativeAssemblerService.class);

    // Thông số lọc nhiễu: K-mer phải xuất hiện ít nhất 3 lần mới được coi là dữ liệu thật
    private static final int MIN_KMER_COVERAGE = 3;

    static class Node {
        String kmer;
        List<Edge> outgoingEdges = new ArrayList<>();
        int inDegree = 0;
        Node(String kmer) { this.kmer = kmer; }
    }

    static class Edge {
        Node to;
        char transitionChar;
        Edge(Node to, char transitionChar) {
            this.to = to;
            this.transitionChar = transitionChar;
        }
    }

    public File executeNativeAssembly(MultipartFile[] files, int k) throws Exception {
        log.info("🚀 Khởi động Lõi SOAP Native Assembler (Có khử nhiễu & RC)...");
        List<String> reads = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            InputStream is = file.getInputStream();
            if (fileName != null && fileName.toLowerCase().endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                int lineCount = 0;
                while ((line = br.readLine()) != null) {
                    lineCount++;
                    if (lineCount % 4 == 2) {
                        reads.add(line.trim().toUpperCase());
                    }
                }
            }
        }
        log.info("✅ Đã tải {} reads lên RAM.", reads.size());

        List<String> contigs = assembleNative(reads, k);

        Path tempDir = Files.createTempDirectory("soap_native_assembly_");
        File fastaFile = new File(tempDir.toFile(), "soap_native_assembled.fasta");
        
        try (PrintWriter pw = new PrintWriter(new FileWriter(fastaFile))) {
            for (int i = 0; i < contigs.size(); i++) {
                String contig = contigs.get(i);
                pw.println(">NODE_" + (i + 1) + "_length_" + contig.length() + "_SOAP_Native_Engine");
                for (int j = 0; j < contig.length(); j += 80) {
                    pw.println(contig.substring(j, Math.min(j + 80, contig.length())));
                }
            }
        }

        if (!fastaFile.exists() || fastaFile.length() == 0) {
            throw new RuntimeException("Thuật toán Native không tạo ra được Contig nào.");
        }
        return fastaFile;
    }

    private List<String> assembleNative(List<String> reads, int k) {
        // BƯỚC 1: Đếm tần suất K-mer (Bao gồm cả Reverse Complement)
        log.info("🔍 Bước 1: Quét K-mer và lọc nhiễu (Coverage >= {})...", MIN_KMER_COVERAGE);
        Map<String, Integer> kmerCounts = new HashMap<>();
        
        for (String read : reads) {
            if (read.length() < k) continue;
            String rcRead = getReverseComplement(read);

            extractAndCountKmers(read, k, kmerCounts);
            extractAndCountKmers(rcRead, k, kmerCounts); // Nạp cả chuỗi ngược
        }

        // Lọc bỏ K-mer rác (Lỗi giải trình tự)
        Set<String> solidKmers = new HashSet<>();
        for (Map.Entry<String, Integer> entry : kmerCounts.entrySet()) {
            if (entry.getValue() >= MIN_KMER_COVERAGE) {
                solidKmers.add(entry.getKey());
            }
        }
        log.info("🧹 Đã lọc bỏ K-mer rác. Giữ lại {} Solid K-mers.", solidKmers.size());

        // BƯỚC 2: Xây dựng đồ thị De Bruijn chỉ từ Solid K-mers
        log.info("🧠 Bước 2: Xây dựng Đồ thị De Bruijn...");
        Map<String, Node> graph = new HashMap<>();
        buildDeBruijnGraph(solidKmers, k, graph);

        // BƯỚC 3: Trích xuất Contigs
        List<String> contigs = extractUnambiguousContigs(graph);
        
        // Sắp xếp Contig từ dài xuống ngắn
        contigs.sort((a, b) -> Integer.compare(b.length(), a.length()));

        log.info("🎉 Lắp ráp hoàn tất! Đã tạo ra {} contigs. Contig dài nhất: {} bp.", 
                 contigs.size(), contigs.isEmpty() ? 0 : contigs.get(0).length());
        return contigs;
    }

    private void extractAndCountKmers(String seq, int k, Map<String, Integer> counts) {
        for (int i = 0; i <= seq.length() - k; i++) {
            String kmer = seq.substring(i, i + k);
            counts.put(kmer, counts.getOrDefault(kmer, 0) + 1);
        }
    }

    private void buildDeBruijnGraph(Set<String> kmers, int k, Map<String, Node> graph) {
        long edgeCount = 0;
        for (String kmer : kmers) {
            String prefix = kmer.substring(0, k - 1);
            String suffix = kmer.substring(1, k);
            char transitionChar = kmer.charAt(k - 1);

            Node prefixNode = graph.computeIfAbsent(prefix, Node::new);
            Node suffixNode = graph.computeIfAbsent(suffix, Node::new);

            prefixNode.outgoingEdges.add(new Edge(suffixNode, transitionChar));
            suffixNode.inDegree++;
            edgeCount++;
        }
        log.info("📊 Thống kê Đồ thị: {} Nodes, {} Edges.", graph.size(), edgeCount);
    }

    private List<String> extractUnambiguousContigs(Map<String, Node> graph) {
        log.info("🔗 Bước 3: Đang dò đường và trích xuất Contigs...");
        List<String> contigs = new ArrayList<>();
        Set<Node> visited = new HashSet<>();

        for (Node node : graph.values()) {
            // Tìm điểm bắt đầu (In-degree = 0, hoặc là node bị phân nhánh đến)
            if (!visited.contains(node) && node.inDegree != 1 && node.outgoingEdges.size() == 1) {
                StringBuilder contigBuilder = new StringBuilder(node.kmer);
                Node curr = node;
                visited.add(curr);

                while (curr.outgoingEdges.size() == 1) {
                    Edge edge = curr.outgoingEdges.get(0);
                    Node next = edge.to;

                    if (visited.contains(next) || next.inDegree > 1) {
                        break; 
                    }

                    contigBuilder.append(edge.transitionChar);
                    visited.add(next);
                    curr = next;
                }
                
                if (contigBuilder.length() > 200) { // Tăng độ dài tối thiểu lên 200bp
                    contigs.add(contigBuilder.toString());
                }
            }
        }
        return contigs;
    }

    // Thuật toán Sinh học cốt lõi: Sợi bổ sung ngược (Reverse Complement)
    private String getReverseComplement(String seq) {
        StringBuilder sb = new StringBuilder(seq.length());
        for (int i = seq.length() - 1; i >= 0; i--) {
            char c = seq.charAt(i);
            switch (c) {
                case 'A': sb.append('T'); break;
                case 'T': sb.append('A'); break;
                case 'G': sb.append('C'); break;
                case 'C': sb.append('G'); break;
                default: sb.append('N'); break;
            }
        }
        return sb.toString();
    }
}