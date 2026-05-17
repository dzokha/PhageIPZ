package vn.ipz.phageipz.engine.qc.modules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import vn.ipz.phageipz.config.PhageIPZProperties;
import vn.ipz.phageipz.domain.sequence.Sequence;
import vn.ipz.phageipz.domain.sequence.ContaminantHit;
import vn.ipz.phageipz.domain.sequence.ContaminentFinder;
import vn.ipz.phageipz.engine.qc.core.QCModule;
import vn.ipz.phageipz.engine.qc.core.ModuleConfig;

@Component
public class OverRepresentedSeqs implements QCModule {

    private final ModuleConfig moduleConfig;
    private final PhageIPZProperties properties;
    private final ContaminentFinder contaminentFinder;
    
    @Autowired
    public OverRepresentedSeqs(ModuleConfig moduleConfig, 
                               PhageIPZProperties properties, 
                               ContaminentFinder contaminentFinder) {
        this.moduleConfig = moduleConfig;
        this.properties = properties;
        this.contaminentFinder = contaminentFinder;
        this.duplicationModule = new DuplicationLevel(moduleConfig);
    }

    protected HashMap<String, Integer> sequences = new HashMap<String, Integer>();
    protected long count = 0;
    private OverrepresentedSeq[] overrepresntedSeqs = null;
    private boolean calculated = false;
    private boolean frozen = false;
    private DuplicationLevel duplicationModule;
    
    private final int OBSERVATION_CUTOFF = 100000;
    private int uniqueSequenceCount = 0;
    protected long countAtUniqueLimit = 0;

    public void setDuplicationModule(DuplicationLevel duplicationModule) {
        this.duplicationModule = duplicationModule;
    }
    
    public boolean ignoreInReport () {
        return moduleConfig.getParam("overrepresented", "ignore") > 0;
    }
    
    public String description() {
        return "Identifies sequences which are overrepresented in the set";
    }
    
    public boolean ignoreFilteredSequences() {
        return true;
    }
    
    public DuplicationLevel duplicationLevelModule () {
        return duplicationModule;
    }

    @Override
    public Object getResultsPanel() {
        return null; // Tắt UI Swing cũ
    }

    // =======================================================
    // BỔ SUNG: DTO nội bộ và hàm xuất dữ liệu cho Web API
    // =======================================================
    public record OverrepresentedData(String sequence, long count, double percentage, String source) {}

    public List<OverrepresentedData> getChartData() {
        if (!calculated) getOverrepresentedSeqs();
        List<OverrepresentedData> list = new ArrayList<>();
        if (overrepresntedSeqs != null) {
            for (OverrepresentedSeq seq : overrepresntedSeqs) {
                list.add(new OverrepresentedData(
                    seq.seq(), 
                    seq.count(), 
                    seq.percentage(), 
                    seq.contaminantHit()
                ));
            }
        }
        return list;
    }

    public DuplicationLevel getDuplicationLevelModule () {
        return duplicationModule;
    }

    private synchronized void getOverrepresentedSeqs () {
        if (calculated) return;

        if (duplicationModule != null) {
            duplicationModule.calculateLevels();
        }
        
        Iterator<String> s = sequences.keySet().iterator();
        List<OverrepresentedSeq> keepers = new ArrayList<OverrepresentedSeq>();
        
        while (s.hasNext()) {
            String seq = s.next();
            double percentage = ((double)sequences.get(seq)/count)*100;
            
            // Nếu % vượt ngưỡng Warn (mặc định là 0.1%) thì mới ghi nhận
            if (percentage > moduleConfig.getParam("overrepresented", "warn")) {
                OverrepresentedSeq os = new OverrepresentedSeq(seq, sequences.get(seq), percentage);
                keepers.add(os);
            }
        }
        
        overrepresntedSeqs = keepers.toArray(new OverrepresentedSeq[0]);
        Arrays.sort(overrepresntedSeqs);
        calculated = true;
        sequences.clear();
    }
    
    public void reset () {
        count = 0;
        sequences.clear();
        calculated = false;
        overrepresntedSeqs = null;
    }

    public String name() {
        return "Overrepresented sequences";
    }

    public void processSequence(Sequence sequence) {
        calculated = false;
        ++count;
        
        String seq = sequence.getSequence();

        int dupLength = properties.getAnalysis().getDupLength();
        
        if (dupLength != 0) {
            int end = Math.min(seq.length(), dupLength);
            seq = seq.substring(0, end);            
        } else if (seq.length() > 50) {
            seq = seq.substring(0, 50);
        }
                
        if (sequences.containsKey(seq)) {
            sequences.put(seq, sequences.get(seq)+1);
            if (!frozen) {
                countAtUniqueLimit = count;
            }
        } else {
            if (!frozen) {
                sequences.put(seq, 1);
                ++uniqueSequenceCount;
                countAtUniqueLimit = count;
                if (uniqueSequenceCount == OBSERVATION_CUTOFF) {
                    frozen = true;
                }
            }
        }       
    }

    private class OverrepresentedSeq implements Comparable<OverrepresentedSeq>{
        private String seq;
        private int count;
        private double percentage;
        private ContaminantHit contaminantHit;
        
        public OverrepresentedSeq (String seq, int count, double percentage) {
            this.seq = seq;
            this.count = count;
            this.percentage = percentage;
            this.contaminantHit = contaminentFinder.findContaminantHit(seq);
        }
        
        public String seq () { return seq; }
        public int count () { return count; }
        public double percentage () { return percentage; }

        public String contaminantHit () {
            return (contaminantHit == null) ? "No Hit" : contaminantHit.toString();
        }

        public int compareTo(OverrepresentedSeq o) {
            return o.count - this.count;
        }
    }

    public boolean raisesError() {
        if (!calculated) getOverrepresentedSeqs();
        if (overrepresntedSeqs != null && overrepresntedSeqs.length>0) {
            if (overrepresntedSeqs[0].percentage > moduleConfig.getParam("overrepresented", "error")) {
                return true;
            }
        }
        return false;
    }

    public boolean raisesWarning() {
        if (!calculated) getOverrepresentedSeqs();
        return overrepresntedSeqs != null && overrepresntedSeqs.length > 0;
    }
}