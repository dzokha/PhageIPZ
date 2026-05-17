package vn.ipz.phageipz.engine.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vn.ipz.phageipz.config.PhageIPZProperties;

import java.io.*;
import java.nio.file.*;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.text.DecimalFormat;

@Service
public class DatabaseManagerService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManagerService.class);
    private final PhageIPZProperties PhageIPZProperties;
    private final AtomicReference<String> dbStatus = new AtomicReference<>("CHECKING");
    private final String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

    public DatabaseManagerService(PhageIPZProperties PhageIPZProperties) {
        this.PhageIPZProperties = PhageIPZProperties;
    }

    public String getNormalizedDbPath() {
        String rawPath = PhageIPZProperties.getDbFileDir();
        return rawPath == null ? null : new File(rawPath).getAbsolutePath();
    }

    public String getDbPath() { return getNormalizedDbPath(); }

    public String checkDatabaseStatus() {
        String path = getNormalizedDbPath();
        if (path == null) return "ERROR";
        File folder = new File(path);

        boolean hasPhrogs = new File(folder, "PHROGs_seq_db").exists();
        boolean hasCard = new File(folder, "CARD_db").exists();
        boolean hasVfdb = new File(folder, "VFDB_db").exists();

        if (folder.exists() && folder.isDirectory() && hasPhrogs && hasCard && hasVfdb) {
            dbStatus.set("READY");
        } else {
            dbStatus.set("MISSING");
        }
        return dbStatus.get();
    }

    @Async
    public void buildDatabaseFromLocalFiles(String tempDirPath) {
        String dbPath = getNormalizedDbPath();
        dbStatus.set("BUILDING");
        log.info("🚀 [DB-BUILDER] Bắt đầu quá trình CẬP NHẬT CSDL tại: {}", dbPath);

        try {
            Path tempPath = Paths.get(tempDirPath);
            Path soapDbPath = Paths.get(dbPath);
            
            // XÓA BỎ DỮ LIỆU CŨ NẾU ĐÃ TỒN TẠI ĐỂ LÀM LẠI TỪ ĐẦU
            if (Files.exists(soapDbPath)) {
                log.info("🗑️ Đang xóa CSDL cũ để chuẩn bị cập nhật bản mới...");
                deleteDirectory(soapDbPath.toFile());
            }
            Files.createDirectories(soapDbPath);

            boolean isWin = os.contains("win");
            File scriptFile = new File(tempDirPath, isWin ? "build_db.bat" : "build_db.sh");

            // COPY FILE CHÚ GIẢI THỨ 5 (TSV) THẲNG VÀO THƯ MỤC CSDL MỚI
            log.info("  > Sao chép file chú giải phrog_annot_v4.tsv...");
            Path annotTsv = Files.walk(tempPath)
                    .filter(p -> p.getFileName().toString().toLowerCase().contains("phrog_annot") && p.toString().endsWith(".tsv"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy file phrog_annot_v4.tsv trong thư mục Upload!"));
            Files.copy(annotTsv, soapDbPath.resolve("phrog_annot_v4.tsv"), StandardCopyOption.REPLACE_EXISTING);

            // ==============================================================
            // BƯỚC 1: GIẢI NÉN VÀO THƯ MỤC CÁCH LY
            // ==============================================================
            log.info("  > Giải nén CSDL PHROGs FASTA...");
            Path phrogTar = Files.walk(tempPath)
                    .filter(p -> p.getFileName().toString().toLowerCase().contains("phrog") && p.toString().endsWith(".tar.gz"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy file CSDL PHROGs (.tar.gz) trong thư mục tải lên!"));
            
            Path phrogTempDir = tempPath.resolve("phrogs_extracted");
            Files.createDirectories(phrogTempDir);
            runCommand(tempPath.toString(), "tar", "-xzf", phrogTar.toAbsolutePath().toString(), "-C", phrogTempDir.toAbsolutePath().toString()); 

            log.info("  > Giải nén CARD data...");
            Path cardTar = Files.walk(tempPath)
                    .filter(p -> p.toString().endsWith(".tar.bz2"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Thiếu file CARD (.tar.bz2)!"));
            runCommand(tempPath.toString(), "tar", "-xf", cardTar.getFileName().toString(), "-C", ".");

            log.info("  > Giải nén VFDB bằng GZIP...");
            Path vfdbGz = Files.walk(tempPath)
                    .filter(p -> p.getFileName().toString().toUpperCase().contains("VFDB") && p.toString().endsWith(".gz"))
                    .findFirst().orElseThrow(() -> new RuntimeException("Thiếu file VFDB!"));
            Path vfdbFas = tempPath.resolve("VFDB_raw.fas");
            try (InputStream gis = new java.util.zip.GZIPInputStream(Files.newInputStream(vfdbGz));
                 OutputStream fos = Files.newOutputStream(vfdbFas)) {
                gis.transferTo(fos);
            }

            // ==============================================================
            // BƯỚC 2: GỘP FILE VÀ "BƠM" TÊN FILE VÀO MÃ GENE
            // ==============================================================
            log.info("  > Đang gộp các tệp PHROGs và gắn mã định danh PHROG vào Header...");
            Path mergedPhrogsPath = soapDbPath.resolve("phrogs_all.faa");
            AtomicInteger count = new AtomicInteger(0);
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(mergedPhrogsPath.toFile()))) {
                try (Stream<Path> paths = Files.walk(phrogTempDir)) {
                    paths.filter(p -> {
                        if (!Files.isRegularFile(p)) return false;
                        String name = p.getFileName().toString().toLowerCase();
                        if (name.startsWith("._")) return false;
                        return name.endsWith(".faa") || name.endsWith(".fasta") || name.endsWith(".fas") || name.endsWith(".pep") || name.startsWith("phrog_");
                    }).forEach(file -> {
                        try {
                            String fileName = file.getFileName().toString();
                            String phrogId = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;

                            try (BufferedReader reader = Files.newBufferedReader(file)) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (line.startsWith(">")) {
                                        writer.write(">" + phrogId + " ## " + line.substring(1) + "\n");
                                    } else {
                                        writer.write(line + "\n");
                                    }
                                }
                            }
                            count.incrementAndGet();
                        } catch (IOException e) {
                            log.error("Lỗi đọc file: {}", file);
                        }
                    });
                }
            }

            log.info("  > Đã gộp và bóc tách thành công {} tệp Fasta thành phrogs_all.faa.", count.get());
            if (count.get() == 0 || Files.size(mergedPhrogsPath) == 0) {
                throw new RuntimeException("LỖI: Tệp phrogs_all.faa trống rỗng!");
            }

            // ==============================================================
            // BƯỚC 3: TẠO SCRIPT CHỈ MỤC MMSEQS2
            // ==============================================================
            log.info("  > Tạo script lập chỉ mục...");
            try (FileWriter writer = new FileWriter(scriptFile)) {
                writer.write(isWin ? "@echo off\n" : "#!/bin/bash\nset -e\n");
                writer.write("cd \"" + dbPath + "\"\n");

                writer.write("echo 'Indexing PHROGs...'\n");
                writer.write("mmseqs createdb phrogs_all.faa PHROGs_seq_db\n");
                writer.write("mmseqs createindex PHROGs_seq_db tmp\n");

                writer.write("echo 'Indexing CARD...'\n");
                if (isWin) {
                    writer.write("for /r \"" + tempDirPath + "\" %%I in (protein_fasta_protein_homolog_model.fasta) do mmseqs createdb \"%%I\" CARD_db\n");
                } else {
                    writer.write("CARD_F=$(find \"" + tempDirPath + "\" -name 'protein_fasta_protein_homolog_model.fasta' | head -n 1)\n");
                    writer.write("mmseqs createdb \"$CARD_F\" CARD_db\n");
                }

                writer.write("echo 'Indexing VFDB...'\n");
                writer.write("mmseqs createdb \"" + vfdbFas.toAbsolutePath() + "\" VFDB_db\n");
            }

            // ==============================================================
            // BƯỚC 4: THỰC THI CHỈ MỤC
            // ==============================================================
            scriptFile.setExecutable(true);
            String runCmd = isWin ? "cmd.exe /c " + scriptFile.getName() : "./" + scriptFile.getName();
            runCommand(tempDirPath, runCmd.split(" "));

            // ==============================================================
            // BƯỚC 5: DỌN DẸP RÁC
            // ==============================================================
            log.info("🧹 Đang dọn dẹp thư mục tạm và file rác...");
            Files.deleteIfExists(mergedPhrogsPath); // Xóa file text gốc
            deleteDirectory(tempPath.toFile()); // Hàm này đã có sẵn ở dưới để dọn dẹp

            // BỔ SUNG: Dọn dẹp luôn cái thư mục "tmp" do MMseqs2 tạo ra
            File mmseqsTmp = new File(dbPath, "tmp");
            if (mmseqsTmp.exists()) {
                deleteDirectory(mmseqsTmp);
            }

            dbStatus.set("READY");
            log.info("✅ [DB-BUILDER] HOÀN TẤT! CSDL SOAP MỚI ĐÃ SẴN SÀNG.");

        } catch (Exception e) {
            dbStatus.set("ERROR");
            log.error("❌ [DB-BUILDER] Lỗi nghiêm trọng: ", e);
        }
    }

    private void runCommand(String workDir, String... args) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.environment().put("PATH", "/usr/local/bin:/usr/bin:/bin:/opt/homebrew/bin:" + System.getenv("PATH"));
        pb.directory(new File(workDir));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) { log.info("[EXEC] {}", line); }
        }
        if (p.waitFor() != 0) throw new RuntimeException("Lệnh thất bại: " + String.join(" ", args));
    }

    private void deleteDirectory(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) { deleteDirectory(file); }
        }
        directory.delete();
    }

    public String getDatabaseSizeFormatted() {
        String path = getNormalizedDbPath();
        if (path == null) return "0 B";
        try (Stream<Path> stream = Files.walk(Paths.get(path))) {
            long totalBytes = stream.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try { return Files.size(p); } 
                        catch (IOException e) { return 0L; }
                    }).sum();
            if (totalBytes <= 0) return "0 B";
            final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
            int digitGroups = (int) (Math.log10(totalBytes) / Math.log10(1024));
            return new DecimalFormat("#,##0.##").format(totalBytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
        } catch (Exception e) { return "Unknown"; }
    }
}