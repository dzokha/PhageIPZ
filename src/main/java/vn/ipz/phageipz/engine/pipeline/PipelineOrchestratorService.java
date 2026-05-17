package vn.ipz.phageipz.engine.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.ipz.phageipz.engine.db.DatabaseManagerService;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

@Service
public class PipelineOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(PipelineOrchestratorService.class);
    private final DatabaseManagerService dbService;

    public PipelineOrchestratorService(DatabaseManagerService dbService) {
        this.dbService = dbService;
    }

    public void runAnalysisPipeline(String jobId, String inputFastaPath, String outputDirPath) {
        log.info("🎯 [Job: {}] BẮT ĐẦU LUỒNG PHÂN TÍCH...", jobId);
        long startTime = System.currentTimeMillis();

        try {
            Path outputDir = Paths.get(outputDirPath, jobId);
            if (!Files.exists(outputDir)) Files.createDirectories(outputDir);

            // PHASE 1: GENE CALLING
            log.info("  [1/3] Đang tìm gen bằng Prodigal...");
            Path proteinFasta = outputDir.resolve("predicted_proteins.faa");
            Path genesGff = outputDir.resolve("predicted_genes.gff");
            
            runCommand(outputDir.toFile(), "prodigal", 
                "-i", inputFastaPath, 
                "-a", proteinFasta.toString(), 
                "-o", genesGff.toString(), 
                "-f", "gff", 
                "-p", "meta");
                    
            if (!Files.exists(proteinFasta) || Files.size(proteinFasta) == 0) {
                throw new RuntimeException("Cắt gen thất bại!");
            }

            // PHASE 2: ANNOTATION
            log.info("  [2/3] Kích hoạt chế độ chú giải siêu luồng...");
            String dbPath = dbService.getDbPath();

            CompletableFuture<Void> phrogsTask = CompletableFuture.runAsync(() -> {
                String phrogsDb = Paths.get(dbPath, "PHROGs_seq_db").toString(); 
                executePhrogSearch(outputDir, proteinFasta, Paths.get(phrogsDb), "phrogs_result.tsv", "tmp_phrogs"); 
            });

            CompletableFuture<Void> cardTask = CompletableFuture.runAsync(() -> {
                String cardDb = Paths.get(dbPath, "CARD_db").toString();
                executeStandardSearch(outputDir, proteinFasta, Paths.get(cardDb), "card_result.tsv", "tmp_card");
            });

            CompletableFuture<Void> vfdbTask = CompletableFuture.runAsync(() -> {
                String vfdbDb = Paths.get(dbPath, "VFDB_db").toString();
                executeStandardSearch(outputDir, proteinFasta, Paths.get(vfdbDb), "vfdb_result.tsv", "tmp_vfdb"); 
            });

            CompletableFuture.allOf(phrogsTask, cardTask, vfdbTask).join();
            log.info("🎉 [Job: {}] HOÀN TẤT SAU {} GIÂY!", jobId, (System.currentTimeMillis() - startTime) / 1000);

        } catch (Exception e) {
            log.error("❌ Lỗi nghiêm trọng: {}", e.getMessage());
        }
    }

    private void executePhrogSearch(Path outDir, Path queryFasta, Path phrogDb, String outName, String tmpName) {
        File workDir = outDir.toFile();
        String outTsv = outDir.resolve(outName).toString();
        String tmpDir = outDir.resolve(tmpName).toString();

        String m4Cores = String.valueOf(Runtime.getRuntime().availableProcessors());
        log.info("      🚀 Đang dóng hàng Sequence-vs-Sequence trên M4 Pro ({} cores)...", m4Cores);

        runCommand(workDir, "mmseqs", "easy-search", 
                queryFasta.toString(), 
                phrogDb.toString(), 
                outTsv, 
                tmpDir,
                "-s", "7.5", 
                "--threads", m4Cores,
                "--format-output", "query,target,evalue,pident,alnlen,qcov,tcov"); 
                
        log.info("      ✅ Xong PHROGs Annotation: {}", outName);
    }

    private void executeStandardSearch(Path outDir, Path query, Path db, String outName, String tmpName) {
        String m4Cores = String.valueOf(Runtime.getRuntime().availableProcessors());
        runCommand(outDir.toFile(), "mmseqs", "easy-search", 
                query.toString(), db.toString(), outDir.resolve(outName).toString(), outDir.resolve(tmpName).toString(),
                "--threads", m4Cores,
                "--format-output", "query,target,evalue,pident,alnlen,qcov,tcov");
        log.info("      ✅ Xong Standard Annotation: {}", outName);
    }

    private void runCommand(File workDir, String... args) {
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.environment().put("PATH", "/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin:" + System.getenv("PATH"));
            pb.directory(workDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.toLowerCase().contains("error")) log.error("  [{} LOG] {}", args[0], line);
                }
            }
            p.waitFor();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}