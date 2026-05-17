package vn.ipz.phageipz.engine.qc.modules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;

import vn.ipz.phageipz.engine.qc.modules.gc.GCModel;
import vn.ipz.phageipz.engine.qc.modules.gc.GCModelValue;
import vn.ipz.phageipz.domain.sequence.Sequence;
import vn.ipz.phageipz.core.utils.NormalDistribution;
import vn.ipz.phageipz.dto.response.chart.PointDTO; // Đảm bảo đã có DTO này
import vn.ipz.phageipz.engine.qc.core.QCModule;
import vn.ipz.phageipz.engine.qc.core.ModuleConfig;

/**
 * Phiên bản hoàn thiện cho Web/Server (Headless mode)
 */
@Component
@Scope("prototype")
public class PerSequenceGCContent implements QCModule {

    private final ModuleConfig moduleConfig;

    @Autowired
    public PerSequenceGCContent(ModuleConfig moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    private double [] gcDistribution = new double[101];
    private double [] theoreticalDistribution = new double[101];
    private int maxCount = 0; 
    private int [] xCategories = new int[0];
    private double max = 0;
    private double deviationPercent;
    private boolean calculated = false;
    
    private GCModel [] cachedModels = new GCModel [200];

    @Override
    public Object getResultsPanel() {
        return null;
    }

    /**
     * Cung cấp dữ liệu cho Frontend (D3.js) vẽ biểu đồ đường đôi (Thực tế vs Lý thuyết)
     */
    public List<double[]> getChartData() {
        if (!calculated) calculateDistribution();
        List<double[]> data = new ArrayList<>();
        data.add(gcDistribution);
        data.add(theoreticalDistribution);
        return data;
    }
    
    public boolean ignoreFilteredSequences() {
        return true;
    }
    
    public boolean ignoreInReport () {
        if (moduleConfig.getParam("gc_sequence", "ignore") > 0) {
            return true;
        }
        return false;
    }
    
    private synchronized void calculateDistribution () {
        max = 0;
        xCategories = new int[gcDistribution.length];
        double totalCount = 0;
        
        int firstMode = 0;
        double modeCount = 0;
        
        for (int i=0;i<gcDistribution.length;i++) {
            xCategories[i] = i;
            totalCount += gcDistribution[i];
            
            if (gcDistribution[i] > modeCount) {
                modeCount = gcDistribution[i];
                firstMode = i;
            }
            if (gcDistribution[i] > max) max = gcDistribution[i];
        }

        double mode = 0;
        int modeDuplicates = 0;
        boolean fellOffTop = true;

        for (int i=firstMode;i<gcDistribution.length;i++) {
            if (gcDistribution[i] > gcDistribution[firstMode] - (gcDistribution[firstMode]/10)) {
                mode += i;
                modeDuplicates++;
            }
            else {
                fellOffTop = false;
                break;
            }
        }

        boolean fellOffBottom = true;
        for (int i=firstMode-1;i>=0;i--) {
            if (gcDistribution[i] > gcDistribution[firstMode] - (gcDistribution[firstMode]/10)) {
                mode += i;
                modeDuplicates++;
            }
            else {
                fellOffBottom = false;
                break;
            }
        }

        if (fellOffBottom || fellOffTop) {
            mode = firstMode;
        }
        else {
            mode /= modeDuplicates;
        }
        
        double stdev = 0;
        for (int i=0;i<gcDistribution.length;i++) {
            stdev += Math.pow((i-mode),2) * gcDistribution[i];
        }
        
        if (totalCount > 1) {
            stdev /= totalCount-1;
            stdev = Math.sqrt(stdev);
        }
        else {
            stdev = 1;
        }
        
        NormalDistribution nd = new NormalDistribution(mode, stdev);
        deviationPercent = 0;
        
        double tempMax = max;
        for (int i=0; i<theoreticalDistribution.length; i++) {
            double probability = nd.getZScoreForValue(i);
            theoreticalDistribution[i] = probability * totalCount;
            
            if (theoreticalDistribution[i] > tempMax) {
                tempMax = theoreticalDistribution[i];
            }
            deviationPercent += Math.abs(theoreticalDistribution[i]-gcDistribution[i]);
        }
        
        // Cập nhật maxCount để LineGraph vẽ đúng trục Y
        this.maxCount = (int)Math.ceil(tempMax);
        this.max = tempMax;

        if (totalCount > 0) {
            deviationPercent /= totalCount;
            deviationPercent *= 100;
        }
        
        calculated = true;
    }

    public void processSequence(Sequence sequence) {
        char [] seq = truncateSequence(sequence);
        if (seq.length == 0) return; 
        
        int thisSeqGCCount = 0;
        for (int i=0;i<seq.length;i++) {
            if (seq[i] == 'G' || seq[i] == 'C') {
                ++thisSeqGCCount;
            }
        }

        if (seq.length >= cachedModels.length) {
            GCModel [] longerModels = new GCModel[seq.length+1];
            System.arraycopy(cachedModels, 0, longerModels, 0, cachedModels.length);
            cachedModels = longerModels;
        }
        
        if (cachedModels[seq.length] == null) {
            cachedModels[seq.length] = new GCModel(seq.length);
        }

        GCModelValue [] values = cachedModels[seq.length].getModelValues(thisSeqGCCount);
        for (int i=0;i<values.length;i++) {
            gcDistribution[values[i].percentage()] += values[i].increment();
        }
    }
    
    private char [] truncateSequence (Sequence sequence) {
        String seq = sequence.getSequence();
        if (seq.length() > 1000) {
            int length = (seq.length()/1000)*1000;
            return seq.substring(0, length).toCharArray();
        }
        if (seq.length() > 100) {
            int length = (seq.length()/100)*100;
            return seq.substring(0, length).toCharArray();
        }
        return seq.toCharArray();        
    }
    
    public void reset () {
        gcDistribution = new double[101];
        theoreticalDistribution = new double[101];
        maxCount = 0;
        max = 0;
        calculated = false;
    }

    public String description() {
        return "Shows the distribution of GC contents for whole sequences";
    }

    public String name() {
        return "Per sequence GC content";
    }

    public boolean raisesError() {
        if (!calculated) calculateDistribution();
        return deviationPercent > moduleConfig.getParam("gc_sequence", "error");
    }

    public boolean raisesWarning() {
        if (!calculated) calculateDistribution();
        return deviationPercent > moduleConfig.getParam("gc_sequence", "warn");
    }
}