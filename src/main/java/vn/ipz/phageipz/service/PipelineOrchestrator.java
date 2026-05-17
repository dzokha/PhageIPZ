package vn.ipz.phageipz.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

import vn.ipz.phageipz.engine.qc.QCService;
import vn.ipz.phageipz.engine.trimming.NativeTrimmingService;
import vn.ipz.phageipz.engine.trimming.TrimConfig;
import vn.ipz.phageipz.io.parser.SequenceFile;
import vn.ipz.phageipz.io.parser.SequenceFactory;
import vn.ipz.phageipz.config.PhageIPZProperties;
import vn.ipz.phageipz.domain.job.AnalysisResult;
import vn.ipz.phageipz.dto.response.AnalysisReportDTO;
import vn.ipz.phageipz.mapper.AnalysisReportMapper;
import vn.ipz.phageipz.engine.qc.core.QCModule;
import vn.ipz.phageipz.engine.qc.core.ModuleFactory;
import vn.ipz.phageipz.engine.qc.QCRunner;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PipelineOrchestrator {
    
    private static final Logger log = LoggerFactory.getLogger(PipelineOrchestrator.class);

    @Autowired 
    private QCService qcService;
    
    @Autowired 
    private NativeTrimmingService trimmingService;

    @Autowired
    private PhageIPZProperties PhageIPZProperties;

    @Autowired
    private SequenceFactory sequenceFactory;

    @Autowired
    private ModuleFactory moduleFactory;

    @Autowired
    private QCRunner qcRunner;

    public static class PipelineResult {
        public AnalysisReportDTO rawQcReport;
        public File trimmedFile;
        public AnalysisReportDTO cleanQcReport;
    }

    public PipelineResult runFullPipeline(MultipartFile file, TrimConfig trimConfig, boolean autoDetectAdapter) throws Exception {
        PipelineResult result = new PipelineResult();
        String originalName = file.getOriginalFilename();
        log.info("--- BẮT ĐẦU PIPELINE TỰ ĐỘNG CHO FILE: {} ---", originalName);

        // BƯỚC 1: RAW QC
        log.info("[Bước 1/3] Chạy Raw QC...");
        AnalysisResult rawQc = qcService.processUploadedFile(file).join(); 
        result.rawQcReport = AnalysisReportMapper.toDTO(rawQc);

        // BƯỚC 2: TRIMMING
        log.info("[Bước 2/3] Chạy Trimming...");
        File tempInputFile = File.createTempFile("pipe_raw_", "_" + originalName);
        file.transferTo(tempInputFile);

        File trimmedFile;
        try (SequenceFile seqFile = sequenceFactory.getSequenceFile(originalName, new FileInputStream(tempInputFile))) {
            trimmedFile = trimmingService.executeTrimming(seqFile, trimConfig, autoDetectAdapter);
            result.trimmedFile = trimmedFile;
        } finally {
            tempInputFile.delete();
        }

        // BƯỚC 3: CLEAN QC (Xử lý trực tiếp từ File vừa trim, không dùng MockMultipartFile)
        log.info("[Bước 3/3] Chạy Clean QC...");
        String cleanName = originalName.replace(".fastq", "_trimmed.fastq").replace(".fq", "_trimmed.fq");
        
        try (SequenceFile cleanSeqFile = sequenceFactory.getSequenceFile(cleanName, new FileInputStream(trimmedFile))) {
            QCModule[] modules = moduleFactory.getStandardModuleList();
            qcRunner.runAnalysisSync(cleanSeqFile, modules);
            
            AnalysisResult cleanResult = new AnalysisResult(cleanName, modules, "SUCCESS");
            result.cleanQcReport = AnalysisReportMapper.toDTO(cleanResult);
        }

        log.info("--- KẾT THÚC PIPELINE TỰ ĐỘNG ---");
        return result;
    }
}