package vn.ipz.phageipz.api; // Đổi tên package gốc

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.tags.Tag;

// --- Thư viện LangChain4j 0.35.0 (OpenAI + Ollama) ---
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.time.Duration;

// Đổi toàn bộ import sang package mới phageipz
import vn.ipz.phageipz.config.QCConfigManager;
import vn.ipz.phageipz.config.PhageIPZProperties; // Đã đổi từ PhageIPZProperties
import vn.ipz.phageipz.dto.response.AnalysisReportDTO;
import vn.ipz.phageipz.dto.response.chart.TileDataDTO;
import vn.ipz.phageipz.engine.pipeline.PipelineOrchestratorService;
import vn.ipz.phageipz.engine.qc.QCService;
import vn.ipz.phageipz.engine.qc.modules.PerTileQualityScores;
import vn.ipz.phageipz.engine.trimming.NativeTrimmingService;
import vn.ipz.phageipz.engine.trimming.TrimConfig;
import vn.ipz.phageipz.io.parser.RawDataViewer;
import vn.ipz.phageipz.io.parser.SequenceFactory;
import vn.ipz.phageipz.io.parser.SequenceFile;
import vn.ipz.phageipz.mapper.AnalysisReportMapper;
import vn.ipz.phageipz.service.PipelineOrchestrator;
import vn.ipz.phageipz.engine.pipeline.ResultAggregatorService;
import vn.ipz.phageipz.engine.assembly.NativeAssemblerService;
import vn.ipz.phageipz.engine.db.DatabaseManagerService;

@RestController
@RequestMapping("/api/v1/phageipz") // Cập nhật root endpoint
@Tag(name = "PhageIPZ Analysis", description = "Các API liên quan đến phân tích trình tự DNA Phage")
public class PhageIPZWebController { // Đổi tên class

    private static final Logger log = LoggerFactory.getLogger(PhageIPZWebController.class);

    @Value("${openai.api-key:demo}")
    private String openAiApiKey;

    @Value("${openai.model-name:gpt-4o-mini}")
    private String openAiModelName;

    private final QCService qcService;
    private final RawDataViewer rawDataViewer;
    private final NativeTrimmingService trimmingService;
    private final PhageIPZProperties phageipzProperties; // Đổi tên biến cấu hình
    private final NativeAssemblerService nativeAssemblerService; 
    private final QCConfigManager qcConfigManager;
    private final PipelineOrchestrator pipelineOrchestrator; 
    private final PipelineOrchestratorService pipeline; 
    private final ResultAggregatorService aggregator;
    private final DatabaseManagerService dbService; 

    // Cập nhật Constructor
    public PhageIPZWebController(QCService qcService, RawDataViewer rawDataViewer, 
                             NativeTrimmingService trimmingService, PhageIPZProperties phageipzProperties,
                             NativeAssemblerService nativeAssemblerService, 
                             QCConfigManager qcConfigManager, PipelineOrchestrator pipelineOrchestrator, 
                             PipelineOrchestratorService pipeline,
                             ResultAggregatorService aggregator,
                             DatabaseManagerService dbService) {
        this.qcService = qcService;
        this.rawDataViewer = rawDataViewer;
        this.trimmingService = trimmingService;
        this.phageipzProperties = phageipzProperties;
        this.nativeAssemblerService = nativeAssemblerService; 
        this.qcConfigManager = qcConfigManager;
        this.pipelineOrchestrator = pipelineOrchestrator;
        this.pipeline = pipeline;
        this.aggregator = aggregator;
        this.dbService = dbService; 
    }

    @GetMapping("/system/database/status")
    public ResponseEntity<Map<String, String>> getDbStatus() {
        Map<String, String> res = new HashMap<>();
        res.put("status", dbService.checkDatabaseStatus());
        res.put("path", dbService.getNormalizedDbPath());
        res.put("size", dbService.getDatabaseSizeFormatted());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/system/database/upload-build")
    public ResponseEntity<Map<String, String>> uploadAndBuildDb(
            @RequestParam("phrogHmm") MultipartFile phrogHmm,
            @RequestParam("phrogFaa") MultipartFile phrogFaa,
            @RequestParam("cardData") MultipartFile cardData,
            @RequestParam("vfdbData") MultipartFile vfdbData,
            @RequestParam("phrogAnnot") MultipartFile phrogAnnot) {
        try {
            // Đổi prefix thư mục tạm
            Path tempDir = Files.createTempDirectory("phageipz_db_upload_");
            phrogHmm.transferTo(tempDir.resolve(phrogHmm.getOriginalFilename()).toFile());
            phrogFaa.transferTo(tempDir.resolve(phrogFaa.getOriginalFilename()).toFile());
            cardData.transferTo(tempDir.resolve(cardData.getOriginalFilename()).toFile());
            vfdbData.transferTo(tempDir.resolve(vfdbData.getOriginalFilename()).toFile());
            phrogAnnot.transferTo(tempDir.resolve(phrogAnnot.getOriginalFilename()).toFile()); 

            dbService.buildDatabaseFromLocalFiles(tempDir.toAbsolutePath().toString());
            return ResponseEntity.ok(Map.of("message", "Đã nhận 5 file. Hệ thống đang tiến hành Build CSDL ngầm..."));
        } catch (Exception e) {
            log.error("Lỗi upload CSDL: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<List<AnalysisReportDTO>>> analyze(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "limits_adapterWarn", required = false) Double adapterWarn,
            @RequestParam(value = "limits_adapterError", required = false) Double adapterError,
            @RequestParam(value = "limits_nWarn", required = false) Double nWarn,
            @RequestParam(value = "limits_nError", required = false) Double nError,
            @RequestParam(value = "limits_dupWarn", required = false) Double dupWarn,
            @RequestParam(value = "limits_dupError", required = false) Double dupError) {

        if (qcConfigManager != null) {
            if (adapterWarn != null) qcConfigManager.updateLimit("adapter", "warn", adapterWarn);
            if (adapterError != null) qcConfigManager.updateLimit("adapter", "error", adapterError);
            if (nWarn != null) qcConfigManager.updateLimit("n_content", "warn", nWarn);
            if (nError != null) qcConfigManager.updateLimit("n_content", "error", nError);
            if (dupWarn != null) qcConfigManager.updateLimit("duplication", "warn", dupWarn);
            if (dupError != null) qcConfigManager.updateLimit("duplication", "error", dupError);
        }
        
        return qcService.processMultipleFiles(files).thenApply(results -> {
            List<AnalysisReportDTO> dtos = results.stream().map(AnalysisReportMapper::toDTO).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        });
    }

    @PostMapping(value = "/trim", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> trimData(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "qualityCutoff", defaultValue = "20") int qualityCutoff,
            @RequestParam(value = "minLength", defaultValue = "35") int minLength,
            @RequestParam(value = "autoDetectAdapter", defaultValue = "true") boolean autoDetectAdapter) {
        try {
            TrimConfig config = new TrimConfig();
            config.setQualityCutoff(qualityCutoff);
            config.setMinLength(minLength);
            if (autoDetectAdapter) config.setAdapterSequence("AGATCGGAAGAGC"); 

            MultipartFile uploadedFile = files[0];
            File tempInputFile = File.createTempFile("upload_raw_", "_" + uploadedFile.getOriginalFilename());
            uploadedFile.transferTo(tempInputFile);

            // Cập nhật truyền properies
            SequenceFactory factory = new SequenceFactory(phageipzProperties);
            SequenceFile seqFile = factory.getSequenceFile(uploadedFile.getOriginalFilename(), new FileInputStream(tempInputFile));

            File trimmedFile = trimmingService.executeTrimming(seqFile, config, autoDetectAdapter);
            tempInputFile.delete();

            InputStreamResource resource = new InputStreamResource(new FileInputStream(trimmedFile));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"trimmed_" + uploadedFile.getOriginalFilename() + ".gz\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); }
    }

    @PostMapping(value = "/assemble", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> assembleData(@RequestParam("files") MultipartFile[] files) {
        try {
            File assembledFastaFile = nativeAssemblerService.executeNativeAssembly(files, 31);
            InputStreamResource resource = new InputStreamResource(new FileInputStream(assembledFastaFile));
            return ResponseEntity.ok()
                    // Đổi tên file trả về
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"phageipz_assembled.fasta\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); }
    }

    @PostMapping("/annotate")
    public ResponseEntity<Resource> annotateData(@RequestParam("files") MultipartFile[] files) {
        try {
            String jobId = "JOB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            // Đổi prefix thư mục tạm
            Path workDir = Files.createTempDirectory("phageipz_annotation_");
            Path inputFasta = workDir.resolve(files[0].getOriginalFilename());
            files[0].transferTo(inputFasta.toFile());
            pipeline.runAnalysisPipeline(jobId, inputFasta.toString(), workDir.toString());
            // Đổi tham số tag
            File finalGbkFile = aggregator.createGenBank(workDir.resolve(jobId), inputFasta, "PhageIPZ_Annotated");
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"annotated.gbk\"").body(new FileSystemResource(finalGbkFile));
        } catch (Exception e) { return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); }
    }

    @PostMapping(value = "/visualize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> visualizeGenome(@RequestParam("file") MultipartFile file) {
        try { return ResponseEntity.ok(new String(file.getBytes(), StandardCharsets.UTF_8)); }
        catch (Exception e) { return ResponseEntity.status(500).build(); }
    }

    @GetMapping("/per-tile-quality")
    public ResponseEntity<TileDataDTO> getPerTileQuality() {
        try {
            PerTileQualityScores module = qcService.getModule(PerTileQualityScores.class);
            return (module != null) ? ResponseEntity.ok(module.getChartData()) : ResponseEntity.noContent().build();
        } catch (Exception e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); }
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("PhageIPZ Analysis Service is active.");
    }

    @PostMapping(value = "/view-raw", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> viewRawData(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        try {
            List<String> rawLines = rawDataViewer.getRawFastqFromStream(file.getInputStream(), limit);
            String result = String.join("\n", rawLines);
            return rawLines.isEmpty() ? ResponseEntity.ok("File không hợp lệ.") : ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Lỗi view raw data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi đọc file: " + e.getMessage());
        }
    }

    @PostMapping(value = "/run-pipeline", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> runAutoPipeline(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "qualityCutoff", defaultValue = "20") int qualityCutoff,
            @RequestParam(value = "minLength", defaultValue = "35") int minLength,
            @RequestParam(value = "autoDetectAdapter", defaultValue = "true") boolean autoDetectAdapter) {
        try {
            TrimConfig config = new TrimConfig();
            config.setQualityCutoff(qualityCutoff);
            config.setMinLength(minLength);
            if (autoDetectAdapter) config.setAdapterSequence("AGATCGGAAGAGC"); 

            PipelineOrchestrator.PipelineResult result = pipelineOrchestrator.runFullPipeline(file, config, autoDetectAdapter);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("status", "SUCCESS");
            responseData.put("rawQcData", result.rawQcReport);
            responseData.put("cleanQcData", result.cleanQcReport);
            responseData.put("message", "Đã phân tích, cắt tỉa và đánh giá lại thành công!");
            
            return ResponseEntity.ok(responseData);
        } catch (Exception e) { 
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage()); 
        }
    }

    // ===============================================
    // TÍCH HỢP ĐA MÔ HÌNH AI: CHATGPT & OLLAMA
    // ===============================================
    @PostMapping(value = "/explain-qc", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> explainQCWithAI(@RequestBody Map<String, Object> requestData) {
        try {
            String modelChoice = (String) requestData.getOrDefault("modelChoice", "ollama");
            log.info("🌐 Frontend yêu cầu kết nối AI Engine: {}", modelChoice);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> qcData = (Map<String, Object>) requestData.get("qcData");

            ChatLanguageModel model;

            if (modelChoice != null && (modelChoice.equalsIgnoreCase("chatgpt") || 
                                        modelChoice.equalsIgnoreCase("openai") || 
                                        modelChoice.equalsIgnoreCase("gemini"))) {
                log.info("🤖 Đang sử dụng OpenAI ChatGPT để phân tích...");
                
                if (openAiApiKey == null || openAiApiKey.equals("demo") || openAiApiKey.isEmpty()) {
                    throw new RuntimeException("Bạn chưa cấu hình openai.api-key trong file application.yml!");
                }

                model = OpenAiChatModel.builder()
                        .apiKey(openAiApiKey)
                        .modelName(openAiModelName)
                        .temperature(0.3)
                        .timeout(Duration.ofSeconds(60))
                        .build();
            } else {
                log.info("🏠 Đang sử dụng Ollama Llama 3 để phân tích...");
                model = OllamaChatModel.builder()
                        .baseUrl("http://localhost:11434")
                        .modelName("llama3")
                        .temperature(0.3)
                        .timeout(Duration.ofMinutes(2))
                        .build();
            }

            String prompt = String.format(
                "Bạn là một chuyên gia Tin sinh học.\n" +
                "Hãy phân tích các chỉ số kiểm soát chất lượng (QC) mẫu FastQ:\n" +
                "- Tên file: %s\n- Reads: %s\n- %%GC: %s %%\\n- Length: %s\n\n" +
                "Yêu cầu:\n" +
                "1. Nhận xét ngắn gọn về độ tin cậy.\n" +
                "2. Đưa ra lời khuyên cho bước Trimming hoặc Assembly.\n\n" +
                "Trả lời bằng Tiếng Việt. Định dạng HTML (<b>, <br>, <ul>, <li>). Tối đa 250 từ.",
                qcData.get("filename"), qcData.get("totalSequences"), qcData.get("gcContent"), qcData.get("sequenceLength")
            );

            return ResponseEntity.ok(Map.of("explanation", model.generate(prompt)));
        } catch (Exception e) {
            log.error("Lỗi AI: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Lỗi AI: " + e.getMessage()));
        }
    }
}