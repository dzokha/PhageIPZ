package vn.ipz.phageipz.engine.qc.modules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import javax.xml.stream.XMLStreamException;

import vn.ipz.phageipz.domain.sequence.Sequence;
import vn.ipz.phageipz.engine.qc.core.QCModule;
import vn.ipz.phageipz.engine.qc.core.ModuleConfig;

@Component
@Scope("prototype")
public class DuplicationLevel implements QCModule {

    private final ModuleConfig moduleConfig;

    @Autowired
    public DuplicationLevel(ModuleConfig moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    private OverRepresentedSeqs overrepresentedModule;
    private double [] totalPercentages = null;
    private double [] deduplicatedPercentages = null; 
    private double maxCount = 100;
    private double percentDifferentSeqs = 0;
    private String [] labels;
    private static final DecimalFormat df = new DecimalFormat("#.##");

    @Autowired
    public void setOverrepresentedModule(OverRepresentedSeqs overrepresentedModule) {
        this.overrepresentedModule = overrepresentedModule;
        if (this.overrepresentedModule != null) {
            this.overrepresentedModule.setDuplicationModule(this);
        }
    }

    public String description() {
        return "Plots the number of sequences which are duplicated to different levels";
    }

    public boolean ignoreFilteredSequences() {
        return moduleConfig.getParam("duplication", "ignore") > 0;
    }

    public boolean ignoreInReport() {
        return moduleConfig.getParam("duplication", "ignore") > 0;
    }
    
    protected synchronized void calculateLevels () {
        if (totalPercentages != null) return;
        
        totalPercentages = new double[16];
        deduplicatedPercentages = new double[16]; // BỔ SUNG KHỞI TẠO MẢNG DEDUP
        
        HashMap<Integer, Integer> collatedCounts = new HashMap<Integer, Integer>();
        Iterator<String> it = overrepresentedModule.sequences.keySet().iterator();
            
        while (it.hasNext()) {
            int thisCount = overrepresentedModule.sequences.get(it.next());
            collatedCounts.put(thisCount, collatedCounts.getOrDefault(thisCount, 0) + 1);
        }
        
        HashMap<Integer, Double> correctedCounts = new HashMap<Integer, Double>();
        Iterator<Integer> itr = collatedCounts.keySet().iterator();
        
        while (itr.hasNext()) {
            int dupLevel = itr.next();
            int count = collatedCounts.get(dupLevel);
            correctedCounts.put(dupLevel, getCorrectedCount(overrepresentedModule.countAtUniqueLimit, overrepresentedModule.count, dupLevel, count));
        }
        
        double dedupTotal = 0;
        double rawTotal = 0;
        Iterator<Integer> itc = correctedCounts.keySet().iterator();
        
        while (itc.hasNext()) {
            int dupLevel = itc.next();
            double count = correctedCounts.get(dupLevel);
            
            dedupTotal += count;
            rawTotal += count * dupLevel;

            int dupSlot = dupLevel - 1;
            
            if (dupSlot > 9999 || dupSlot < 0) dupSlot = 15;
            else if (dupSlot > 4999) dupSlot = 14;
            else if (dupSlot > 999) dupSlot = 13;
            else if (dupSlot > 499) dupSlot = 12;
            else if (dupSlot > 99) dupSlot = 11;
            else if (dupSlot > 49) dupSlot = 10;
            else if (dupSlot > 9) dupSlot = 9;

            totalPercentages[dupSlot] += count * dupLevel;
            deduplicatedPercentages[dupSlot] += count; // BỔ SUNG TÍNH TOÁN DEDUP
        }
        
        labels = new String [16];
        for (int i=0; i<totalPercentages.length; i++) {
            if (i<9) labels[i] = ""+(i+1);
            else if (i==9) labels[i]=">10";
            else if (i==10) labels[i]=">50";
            else if (i==11) labels[i]=">100";
            else if (i==12) labels[i]=">500";
            else if (i==13) labels[i]=">1k";
            else if (i==14) labels[i]=">5k";
            else if (i==15) labels[i]=">10k";
            
            totalPercentages[i] /= rawTotal;
            totalPercentages[i] *= 100;
            
            // BỔ SUNG TÍNH PHẦN TRĂM CHO DEDUP
            if (dedupTotal > 0) {
                deduplicatedPercentages[i] /= dedupTotal;
                deduplicatedPercentages[i] *= 100;
            }
        }
        
        percentDifferentSeqs = (dedupTotal/rawTotal)*100;
        if (rawTotal == 0) percentDifferentSeqs = 100;
    }
    
    private static double getCorrectedCount (long countAtLimit, long totalCount, int duplicationLevel, int numberOfObservations) {
        if (countAtLimit == totalCount) return numberOfObservations;
        if (totalCount - numberOfObservations < countAtLimit) return numberOfObservations;
        
        double pNotSeeingAtLimit = 1;
        double limitOfCaring = 1d - (numberOfObservations/(numberOfObservations+0.01d));
                
        for (int i=0; i<countAtLimit; i++) {
            pNotSeeingAtLimit *= ((totalCount-i)-duplicationLevel)/(double)(totalCount-i);
            if (pNotSeeingAtLimit < limitOfCaring) {
                pNotSeeingAtLimit = 0;
                break;
            }
        }
        
        double pSeeingAtLimit = 1 - pNotSeeingAtLimit;
        return numberOfObservations / pSeeingAtLimit;
    }

    @Override
    public Object getResultsPanel() { return null; }

    // DTO siêu nhỏ dùng nội bộ để mang theo 2 giá trị Y
    public record DupPoint(String label, double dedupPercent, double totalPercent) {}

    // Trả về danh sách chứa cả 2 đường line thay vì 1 đường
    public List<DupPoint> getChartData() {
        if (totalPercentages == null) calculateLevels();
        List<DupPoint> data = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            data.add(new DupPoint(labels[i], deduplicatedPercentages[i], totalPercentages[i]));
        }
        return data;
    }

    public String name() { return "Sequence Duplication Levels"; }
    public void processSequence(Sequence sequence) {}

    public boolean raisesError() {
        if (totalPercentages == null) calculateLevels();
        return percentDifferentSeqs < moduleConfig.getParam("duplication", "error");
    }

    public boolean raisesWarning() {
        if (totalPercentages == null) calculateLevels();
        return percentDifferentSeqs < moduleConfig.getParam("duplication", "warn");
    }

    public void reset() {
        totalPercentages = null;
        deduplicatedPercentages = null;
    }
}