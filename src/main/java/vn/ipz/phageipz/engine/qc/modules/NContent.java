package vn.ipz.phageipz.engine.qc.modules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;

import java.util.List;
import java.util.ArrayList;

import vn.ipz.phageipz.config.PhageIPZProperties;
import vn.ipz.phageipz.domain.sequence.Sequence;
import vn.ipz.phageipz.dto.response.chart.PointDTO;
import vn.ipz.phageipz.engine.qc.core.QCModule;
import vn.ipz.phageipz.engine.qc.core.ModuleConfig;
import vn.ipz.phageipz.engine.qc.util.BaseGroup;

@Component
@Scope("prototype")
public class NContent implements QCModule {

    private final ModuleConfig moduleConfig;
    private final PhageIPZProperties properties;

    @Autowired
    public NContent(ModuleConfig moduleConfig, PhageIPZProperties properties) {
        this.moduleConfig = moduleConfig;
        this.properties = properties;
    }

    public long [] nCounts = new long [0];
    public long [] notNCounts = new long [0];
    public boolean calculated = false;
    public double [] percentages = null;
    public String [] xCategories = new String[0];

    @Override
    public Object getResultsPanel() {
        return null;
    }

    public List<PointDTO> getChartData() {
        if (!calculated) getPercentages();
        List<PointDTO> data = new ArrayList<>();
        if (percentages == null) return data;
        
        for (int i = 0; i < xCategories.length; i++) {
            data.add(new PointDTO(xCategories[i], percentages[i]));
        }
        return data;
    }

    public boolean ignoreFilteredSequences() {
        return true;
    }
    
    public boolean ignoreInReport () {
        return moduleConfig.getParam("n_content", "ignore") > 0;
    }
    
    private synchronized void getPercentages () {
        if (nCounts.length == 0) {
            calculated = true;
            return;
        }
        
        BaseGroup [] groups = BaseGroup.makeBaseGroups(nCounts.length, this.properties);
        
        xCategories = new String[groups.length];
        percentages = new double [groups.length];

        for (int i=0; i<groups.length; i++) {
            xCategories[i] = groups[i].toString();

            long nCount = 0;
            long total = 0;
            
            for (int bp = groups[i].lowerCount() - 1; bp < groups[i].upperCount(); bp++) {
                // VÁ LỖI 1: Ngăn lỗi Out of Bounds (tràn mảng) khi sequence có độ dài không đều
                if (bp >= nCounts.length) break;
                
                nCount += nCounts[bp];
                total += nCounts[bp];
                total += notNCounts[bp];
            }
            
            // VÁ LỖI 2: Ngăn lỗi chia cho 0 tạo ra giá trị NaN làm chết biểu đồ D3.js
            if (total > 0) {
                percentages[i] = 100 * (nCount / (double) total);
            } else {
                percentages[i] = 0.0; 
            }
        }
                
        calculated = true;
    }
        
    public void processSequence(Sequence sequence) {
        calculated = false;
        char [] seq = sequence.getSequence().toCharArray();
        if (nCounts.length < seq.length) {
            long [] nCountsNew = new long [seq.length];
            long [] notNCountsNew = new long [seq.length];

            // Tối ưu hóa: Dùng System.arraycopy cho code chạy nhanh hơn vòng lặp for
            System.arraycopy(nCounts, 0, nCountsNew, 0, nCounts.length);
            System.arraycopy(notNCounts, 0, notNCountsNew, 0, notNCounts.length);
            
            nCounts = nCountsNew;
            notNCounts = notNCountsNew;
        }
        
        for (int i=0; i<seq.length; i++) {
            if (seq[i] == 'N') {
                ++nCounts[i];
            } else {
                ++notNCounts[i];
            }
        }
    }
    
    public String name() { return "Per base N content"; }
    public String description() { return "Shows the percentage of bases at each position which are not being called"; }
    
    // Đã bổ sung logic Đánh giá Cảnh báo (Warn / Error) chuẩn như FastQC
    public boolean raisesError() { 
        if (!calculated) getPercentages();
        if (percentages == null) return false;
        for (double p : percentages) {
            if (p > moduleConfig.getParam("n_content", "error")) return true;
        }
        return false; 
    }
    
    public boolean raisesWarning() { 
        if (!calculated) getPercentages();
        if (percentages == null) return false;
        for (double p : percentages) {
            if (p > moduleConfig.getParam("n_content", "warn")) return true;
        }
        return false; 
    }

    public void reset() { 
        nCounts = new long[0]; 
        notNCounts = new long[0]; 
        calculated = false; 
    }
}