package vn.ipz.phageipz.engine.qc.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import vn.ipz.phageipz.config.PhageIPZProperties;
import vn.ipz.phageipz.config.QCConfigManager; 
import vn.ipz.phageipz.engine.qc.modules.adapter.AdapterContent;
import vn.ipz.phageipz.engine.qc.modules.kmer.KmerContent;
import vn.ipz.phageipz.domain.sequence.ContaminentFinder;

import vn.ipz.phageipz.engine.qc.modules.BasicStats;
import vn.ipz.phageipz.engine.qc.modules.PerBaseQualityScores;
import vn.ipz.phageipz.engine.qc.modules.PerTileQualityScores;
import vn.ipz.phageipz.engine.qc.modules.PerSequenceQualityScores;
import vn.ipz.phageipz.engine.qc.modules.PerBaseSequenceContent;
import vn.ipz.phageipz.engine.qc.modules.PerSequenceGCContent;
import vn.ipz.phageipz.engine.qc.modules.NContent;
import vn.ipz.phageipz.engine.qc.modules.SequenceLengthDistribution;
import vn.ipz.phageipz.engine.qc.modules.OverRepresentedSeqs;
import vn.ipz.phageipz.engine.qc.modules.DuplicationLevel; // <-- THÊM IMPORT NÀY

/**
 * ModuleFactory đã được cải tiến thành Spring Component để 
 * tự động tiêm cấu hình (Dependency Injection) vào các Module.
 */
@Component
public class ModuleFactory {

    private final PhageIPZProperties properties;
    private final ModuleConfig moduleConfig;
    private final ContaminentFinder contaminentFinder;
    private final QCConfigManager qcConfigManager; 

    @Autowired
    public ModuleFactory(PhageIPZProperties properties, 
                         ModuleConfig moduleConfig, 
                         ContaminentFinder contaminentFinder,
                         QCConfigManager qcConfigManager) { 
        this.properties = properties;
        this.moduleConfig = moduleConfig;
        this.contaminentFinder = contaminentFinder;
        this.qcConfigManager = qcConfigManager;
    }

    /**
     * Khởi tạo danh sách các module QC tiêu chuẩn.
     */
    public QCModule[] getStandardModuleList() {

        // 1. Khởi tạo 2 module có tính phụ thuộc chéo
        OverRepresentedSeqs os = new OverRepresentedSeqs(moduleConfig, properties, contaminentFinder);
        DuplicationLevel dup = new DuplicationLevel(moduleConfig);

        // 2. LIÊN KẾT BẰNG TAY (Bypass Spring DI vì chúng ta dùng từ khóa 'new')
        os.setDuplicationModule(dup);
        dup.setOverrepresentedModule(os);
        
        // 3. Đưa vào danh sách chạy
        QCModule[] module_list = new QCModule[] {
            new BasicStats(), 
            new PerBaseQualityScores(moduleConfig, properties),
            new PerTileQualityScores(moduleConfig, properties),
            new PerSequenceQualityScores(moduleConfig),
            new PerBaseSequenceContent(moduleConfig, properties),
            new PerSequenceGCContent(moduleConfig),
            new NContent(moduleConfig, properties),
            new SequenceLengthDistribution(properties),
        
            dup, // Dùng biến dup đã được liên kết rõ ràng
            os,
            new AdapterContent(properties, moduleConfig, qcConfigManager), 
            new KmerContent(properties, moduleConfig)
        };
    
        return module_list;
    }
}