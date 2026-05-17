package vn.ipz.phageipz.engine.trimming;

import org.springframework.stereotype.Service;
import vn.ipz.phageipz.domain.sequence.Sequence;
import vn.ipz.phageipz.io.parser.SequenceFile;
import vn.ipz.phageipz.io.parser.SequenceFormatException; 
import vn.ipz.phageipz.io.writer.FastQWriter; 
import vn.ipz.phageipz.config.QCConfigManager;

import java.io.File;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NativeTrimmingService {

    private static final Logger log = LoggerFactory.getLogger(NativeTrimmingService.class);
    private final SequenceTrimmer trimmerAlgo;
    private final QCConfigManager qcConfigManager; 

    public NativeTrimmingService(SequenceTrimmer trimmerAlgo, QCConfigManager qcConfigManager) {
        this.trimmerAlgo = trimmerAlgo;
        this.qcConfigManager = qcConfigManager;
    }

    public File executeTrimming(SequenceFile inFile, TrimConfig config, boolean autoDetectAdapter) throws Exception {
        TrimStats stats = new TrimStats();
        log.info("Bắt đầu phân tích và cắt tỉa dữ liệu (Mô phỏng Cutadapt)...");

        File outputFile = File.createTempFile("trimmed_output_", ".fastq.gz");

        try (FastQWriter writer = new FastQWriter(outputFile, true)) {
            while (inFile.hasNext()) {
                Sequence seq = inFile.next();
                
                stats.totalReads++;
                stats.totalBases += seq.getSequence().length();

                // 1. Cắt base vô điều kiện (-u)
                if (config.getCutBases() != 0) {
                    trimmerAlgo.cutBases(seq, config.getCutBases());
                }

                // 2. Cắt theo chất lượng
                if (config.getQualityCutoff() > 0) {
                    int removedQual = trimmerAlgo.trimByQuality(seq, config.getQualityCutoff());
                    stats.qualityTrimmedBases += removedQual;
                }

                if (seq.getSequence().isEmpty()) {
                    stats.readsTooShort++;
                    continue;
                }

                // 3. Cắt Adapter
                if (autoDetectAdapter && qcConfigManager != null) {
                    boolean adapterFoundForThisRead = false;
                    for (Map.Entry<String, String> entry : qcConfigManager.getAllAdapters().entrySet()) {
                        String adapterSeq = entry.getValue();
                        
                        int removedAdapt = trimmerAlgo.trimAdapter(seq, adapterSeq, config.getMinOverlap(), config.getErrorRate());
                        
                        if (removedAdapt > 0) {
                            stats.adapterTrimmedBases += removedAdapt;
                            if (!adapterFoundForThisRead) {
                                stats.readsWithAdapter++;
                                adapterFoundForThisRead = true;
                            }
                            break; 
                        }
                    }
                }

                // 4. Cắt đuôi Poly-A
                if (config.isTrimPolyA()) {
                    trimmerAlgo.trimPolyA(seq);
                }

                // 5. Cắt N hai đầu
                if (config.isTrimN()) {
                    trimmerAlgo.trimN(seq);
                }

                // 6. Lọc chiều dài tối thiểu
                if (seq.getSequence().length() < config.getMinLength()) {
                    stats.readsTooShort++;
                    continue; 
                }

                // Ghi ra file
                writer.write(seq);
                stats.outputReads++;
                stats.outputBases += seq.getSequence().length();
            }
        } catch (SequenceFormatException e) {
            log.error("Lỗi định dạng chuỗi DNA: {}", e.getMessage());
        }

        // In Báo cáo Cutadapt
        stats.printCutadaptReport(log);
        
        return outputFile;
    }
}