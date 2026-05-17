package vn.ipz.phageipz.domain.job;

import vn.ipz.phageipz.engine.qc.core.QCModule;
import vn.ipz.phageipz.engine.qc.modules.BasicStats;
import java.util.Arrays;

public class AnalysisResult {
    private String fileName;
    private QCModule[] modules;
    private String status;

    public AnalysisResult(String fileName, QCModule[] modules, String status) {
        this.fileName = fileName;
        this.modules = modules;
        this.status = status;
    }

    public String getStatus() { return status; }
    public String getFileName() { return fileName; }
    public QCModule[] getModules() { return modules; }

    /**
     * Tìm kiếm và trả về module BasicStats từ mảng modules.
     * Mapper sẽ gọi hàm này để lấy dữ liệu map sang DTO.
     */
    public BasicStats getBasicStats() {
        if (modules == null) return null;
        
        return (BasicStats) Arrays.stream(modules)
                .filter(m -> m instanceof BasicStats)
                .findFirst()
                .orElse(null);
    }
}