package vn.ipz.phageipz.engine.qc.modules.kmer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.math3.distribution.BinomialDistribution;

import java.util.List;
import java.util.ArrayList;

import vn.ipz.phageipz.config.PhageIPZProperties;
import vn.ipz.phageipz.domain.sequence.Sequence;
import vn.ipz.phageipz.engine.qc.core.QCModule;
import vn.ipz.phageipz.engine.qc.core.ModuleConfig;
import vn.ipz.phageipz.engine.qc.util.BaseGroup;

@Component
public class KmerContent implements QCModule {
    private final PhageIPZProperties properties;
    private final ModuleConfig moduleConfig; // Thêm moduleConfig

    private Hashtable<String, Kmer> kmers = new Hashtable<String, Kmer>();
    private int longestSequence = 0;
    private long [][] totalKmerCounts = new long [500][32]; // Khởi tạo kích thước an toàn
    private long skipCount = 0;
    private int minKmerSize = 7;
    private int maxKmerSize = 7;
    public boolean calculated = false;
    private Kmer [] enrichedKmers = null;
    private double [][] enrichments = null;
    private double minGraphValue = 0;
    private double maxGraphValue = 0;
    private String [] xCategories = new String[0];
    private String [] xLabels = new String[0];
    private BaseGroup [] groups;

    @Autowired
    public KmerContent (PhageIPZProperties properties, ModuleConfig moduleConfig) {
        this.properties = properties;
        this.moduleConfig = moduleConfig;

        // SỬA LỖI: Lấy kmerSize từ properties.getAnalysis() thay vì SOAPConfig
        int kmerSize = properties.getAnalysis().getKmerSize();
        if (kmerSize != 0) {
            this.minKmerSize = kmerSize;
            this.maxKmerSize = kmerSize;
        }
    }

    @Override
    public Object getResultsPanel() { return null; }

    @Override
    public boolean ignoreFilteredSequences() { return true; }

    @Override
    public boolean ignoreInReport () { return moduleConfig.getParam("kmer", "ignore") > 0; }

    @Override
    public void processSequence(Sequence sequence) {
        calculated = false;
        ++skipCount;
        if (skipCount % 50 != 0) return;

        String seq = sequence.getSequence();
        if (seq.length() > 500) seq = seq.substring(0, 500);
        if (seq.length() > longestSequence) longestSequence = seq.length();
                        
        for (int kmerSize=minKmerSize; kmerSize<=maxKmerSize; kmerSize++) {
            for (int i=0; i<=seq.length()-kmerSize; i++) {
                String kmer = seq.substring(i, i+kmerSize);
                
                // Cập nhật tổng đếm để tính toán xác suất sau này
                totalKmerCounts[i][kmerSize-1]++;

                if (kmer.indexOf("N") >= 0) continue;

                if (kmers.containsKey(kmer)) {
                    kmers.get(kmer).incrementCount(i);
                } else {
                    kmers.put(kmer, new Kmer(kmer, i, 500));
                }
            }
        }
    }

    private synchronized void calculateEnrichment () {
        if (longestSequence == 0) return;
        groups = BaseGroup.makeBaseGroups((longestSequence - minKmerSize) + 1, this.properties);
        Vector<Kmer> unevenKmers = new Vector<Kmer>();
        Iterator<Kmer> rawKmers = kmers.values().iterator();
        
        while (rawKmers.hasNext()) {
            Kmer k = rawKmers.next();
            long totalKmerOfThisLength = 0;

            for (int i=0; i<totalKmerCounts.length; i++) {
                totalKmerOfThisLength += totalKmerCounts[i][k.sequence().length()-1];
            }

            if (totalKmerOfThisLength == 0) continue;
            float expectedProportion = k.count / (float)totalKmerOfThisLength;
            
            float [] obsExpPositions = new float[groups.length];
            float [] binomialPValues = new float[groups.length];
            long [] positionCounts = k.getPositions();
                
            for (int g=0; g<groups.length; g++) {
                long totalGroupCount = 0;
                long totalGroupHits = 0;
                for (int p=groups[g].lowerCount()-1; p<groups[g].upperCount() && p < positionCounts.length; p++) {
                    totalGroupCount += totalKmerCounts[p][k.sequence().length()-1];
                    totalGroupHits += positionCounts[p];
                }
                            
                if (totalGroupCount == 0 || expectedProportion == 0) continue;
                float predicted = expectedProportion * totalGroupCount;
                obsExpPositions[g] = (float)(totalGroupHits/predicted);
                
                BinomialDistribution bd = new BinomialDistribution((int)totalGroupCount, expectedProportion);
                if (totalGroupHits > predicted) {
                    binomialPValues[g] = (float)((1 - bd.cumulativeProbability((int)totalGroupHits)) * Math.pow(4, k.sequence().length()));
                } else {
                    binomialPValues[g] = 1;
                }
            }
            
            k.setObsExpPositions(obsExpPositions);
            float lowestP = 1;
            for (float p : binomialPValues) if (p < lowestP) lowestP = p;
            
            if (lowestP < 0.01) {
                k.setLowestPValue(lowestP);
                unevenKmers.add(k);
            }
        }
        
        enrichedKmers = unevenKmers.toArray(new Kmer[0]);
        Arrays.sort(enrichedKmers);
        if (enrichedKmers.length > 20) enrichedKmers = Arrays.copyOf(enrichedKmers, 20);
        
        enrichments = new double [Math.min(6, enrichedKmers.length)][groups.length];
        xLabels = new String[enrichments.length];
        xCategories = new String [groups.length];
        for (int i=0; i<groups.length; i++) xCategories[i] = groups[i].toString();
        
        for (int k=0; k<enrichments.length; k++) {
            float [] obsExpPos = enrichedKmers[k].getObsExpPositions();
            for (int g=0; g<groups.length; g++) {                
                enrichments[k][g] = obsExpPos[g];
                if (obsExpPos[g] > maxGraphValue) maxGraphValue = obsExpPos[g];
            }
            xLabels[k] = enrichedKmers[k].sequence();
        }
        kmers.clear();
        calculated = true;
    }

    @Override public String name() { return "Kmer Content"; }
    @Override public String description() { return "Identifies short sequences with uneven representation"; }
    @Override public boolean raisesError() { return false; }
    @Override public boolean raisesWarning() { return false; }
    @Override public void reset() { kmers.clear(); calculated=false; }

    private class Kmer implements Comparable<Kmer> {
        private String sequence;
        private float lowestPValue = 1;
        private float [] obsExpPositions;
        private long [] positions;
        private long count = 0;

        public Kmer(String s, int p, int maxLen) { 
            this.sequence = s; 
            this.positions = new long[maxLen];
            incrementCount(p);
        }
        
        public void incrementCount(int p) {
            if (p < positions.length) {
                positions[p]++;
                count++;
            }
        }

        public long[] getPositions() { return positions; }
        public String sequence() { return sequence; }
        public float[] getObsExpPositions() { return obsExpPositions; }
        public void setObsExpPositions(float[] f) { this.obsExpPositions = f; }
        public void setLowestPValue(float f) { this.lowestPValue = f; }
        
        @Override 
        public int compareTo(Kmer o) { 
            return Float.compare(this.lowestPValue, o.lowestPValue); 
        }
    }

    // =======================================================
    // BỔ SUNG: DTO nội bộ và hàm xuất dữ liệu cho Web API
    // =======================================================
    public record KmerPoint(String position, double value) {}
    public record KmerSeries(String kmer, List<KmerPoint> points) {}

    public List<KmerSeries> getChartData() {
        if (!calculated) calculateEnrichment();
        List<KmerSeries> data = new ArrayList<>();
        if (enrichments == null) return data;

        for (int k = 0; k < xLabels.length; k++) {
            List<KmerPoint> points = new ArrayList<>();
            for (int g = 0; g < xCategories.length; g++) {
                points.add(new KmerPoint(xCategories[g], enrichments[k][g]));
            }
            data.add(new KmerSeries(xLabels[k], points));
        }
        return data;
    }
}