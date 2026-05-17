package vn.ipz.phageipz.engine.qc.modules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import vn.ipz.phageipz.config.PhageIPZProperties;
import vn.ipz.phageipz.domain.sequence.Sequence;
import vn.ipz.phageipz.domain.sequence.PhredEncoding;
import vn.ipz.phageipz.engine.qc.util.QualityCount;
import vn.ipz.phageipz.dto.response.chart.BoxPlotPointDTO; // ĐÃ IMPORT ĐÚNG CHUẨN
import vn.ipz.phageipz.engine.qc.core.QCModule;
import vn.ipz.phageipz.engine.qc.core.ModuleConfig;
import vn.ipz.phageipz.engine.qc.util.BaseGroup;


@Component
@Scope("prototype")
public class PerBaseQualityScores implements QCModule {

    private final ModuleConfig moduleConfig;
    private final PhageIPZProperties properties;

    @Autowired
    public PerBaseQualityScores(ModuleConfig moduleConfig, PhageIPZProperties properties) {
        this.moduleConfig = moduleConfig;
        this.properties = properties;
    }

    public QualityCount [] qualityCounts = new QualityCount[0];
    private double [] means = null;
    private double [] medians = null;
    private double [] lowerQuartile = null;
    private double [] upperQuartile = null;
    private double [] lowest = null;
    private double [] highest = null;
    private String [] xLabels;
    private PhredEncoding encodingScheme;
    private boolean calculated = false;

    private double [][] percentages;
    private String [] xCategories;
    
    @Override
    public Object getResultsPanel() {
        return null;
    }

    // HÀM NÀY NẰM Ở ĐÚNG CHỖ RỒI!
    public List<BoxPlotPointDTO> getBoxPlotData() {
        if (!calculated) getPercentages();
        List<BoxPlotPointDTO> data = new ArrayList<>();
        
        if (means == null || xLabels == null) return data;
        
        for (int i = 0; i < xLabels.length; i++) {
            BoxPlotPointDTO point = new BoxPlotPointDTO(
                xLabels[i],
                lowest[i],
                lowerQuartile[i],
                medians[i],
                upperQuartile[i],
                highest[i],
                means[i]
            );
            data.add(point);
        }
        return data;
    }
    
    public boolean ignoreFilteredSequences() {
        return true;
    }
    
    public boolean ignoreInReport () {
        if (moduleConfig.getParam("quality_base", "ignore") > 0 || qualityCounts.length == 0) {
            return true;
        }
        return false;
    }

    private synchronized void getPercentages () {
        if (qualityCounts.length == 0) {
            calculated = true;
            return;
        }
        
        char [] range = calculateOffsets();
        encodingScheme = PhredEncoding.getFastQEncodingOffset(range[0]);
        
        BaseGroup [] groups = BaseGroup.makeBaseGroups(qualityCounts.length, this.properties);

        int n = groups.length;
        means = new double[n];
        medians = new double[n];
        lowest = new double[n];
        highest = new double[n];
        lowerQuartile = new double[n];
        upperQuartile = new double[n];
        xLabels = new String[n];
        
        for (int i=0; i<n; i++) {
            xLabels[i] = groups[i].toString();
            int minBase = groups[i].lowerCount();
            int maxBase = groups[i].upperCount();
            
            lowest[i] = getPercentile(minBase, maxBase, encodingScheme.offset(), 10);
            highest[i] = getPercentile(minBase, maxBase, encodingScheme.offset(), 90);
            means[i] = getMean(minBase, maxBase, encodingScheme.offset());
            medians[i] = getPercentile(minBase, maxBase, encodingScheme.offset(), 50);
            lowerQuartile[i] = getPercentile(minBase, maxBase, encodingScheme.offset(), 25);
            upperQuartile[i] = getPercentile(minBase, maxBase, encodingScheme.offset(), 75);
        }
        calculated = true;
    }
    
    private char [] calculateOffsets () {
        char minChar = 0;
        char maxChar = 0;
        
        for (int q=0;q<qualityCounts.length;q++) {
            if (q == 0) {
                minChar = qualityCounts[q].getMinChar();
                maxChar = qualityCounts[q].getMaxChar();
            }
            else {
                if (qualityCounts[q].getMinChar() < minChar && qualityCounts[q].getMinChar() != 0) {
                    minChar = qualityCounts[q].getMinChar();
                }
                if (qualityCounts[q].getMaxChar() > maxChar) {
                    maxChar = qualityCounts[q].getMaxChar();
                }
            }
        }
        return new char[] {minChar,maxChar};
    }
    
    public void processSequence(Sequence sequence) {
        calculated = false;
        char [] qual = sequence.getQualityString().toCharArray();
        if (qualityCounts.length < qual.length) {
            QualityCount [] qualityCountsNew = new QualityCount[qual.length];
            System.arraycopy(qualityCounts, 0, qualityCountsNew, 0, qualityCounts.length);
            for (int i=qualityCounts.length; i<qualityCountsNew.length; i++) {
                qualityCountsNew[i] = new QualityCount();                
            }
            qualityCounts = qualityCountsNew;
        }
        
        for (int i=0;i<qual.length;i++) {
            qualityCounts[i].addValue(qual[i]);
        }
    }
    
    public void reset () {
        qualityCounts = new QualityCount[0];
        calculated = false;
    }

    public String description() {
        return "Shows the Quality scores of all bases at a given position in a sequencing run";
    }

    public String name() {
        return "Per base sequence quality";
    }

    public boolean raisesError() {
        if (!calculated) getPercentages();
        if (lowerQuartile == null) return false;

        for (int i=0;i<lowerQuartile.length;i++) {
            if (Double.isNaN(lowerQuartile[i])) continue;
            if (lowerQuartile[i] < moduleConfig.getParam("quality_base_lower", "error") || medians[i] < moduleConfig.getParam("quality_base_median", "error")) {
                return true;
            }
        }
        return false;
    }

    public boolean raisesWarning() {
        if (!calculated) getPercentages();
        if (lowerQuartile == null) return false;

        for (int i=0;i<lowerQuartile.length;i++) {
            if (Double.isNaN(lowerQuartile[i])) continue;
            if (lowerQuartile[i] < moduleConfig.getParam("quality_base_lower", "warn") || medians[i] < moduleConfig.getParam("quality_base_median", "warn")) {
                return true;
            }
        }
        return false;
    }
    
    
    private double getPercentile (int minbp, int maxbp, int offset, int percentile) {
        int count = 0;
        double total = 0;
        for (int i=minbp-1;i<maxbp;i++) {
            if (i < qualityCounts.length && qualityCounts[i].getTotalCount() > 100) {
                count++;
                total += qualityCounts[i].getPercentile(offset, percentile);
            }
        }
        return count > 0 ? total/count : Double.NaN;
    }

    private double getMean (int minbp, int maxbp, int offset) {
        int count = 0;
        double total = 0;
        for (int i=minbp-1;i<maxbp;i++) {
            if (i < qualityCounts.length && qualityCounts[i].getTotalCount() > 0) {
                count++;
                total += qualityCounts[i].getMean(offset);
            }
        }
        return count > 0 ? total/count : 0;
    }
}