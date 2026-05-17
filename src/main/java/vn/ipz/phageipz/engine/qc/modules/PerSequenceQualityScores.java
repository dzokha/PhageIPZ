package vn.ipz.phageipz.engine.qc.modules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import vn.ipz.phageipz.domain.sequence.Sequence;
import vn.ipz.phageipz.domain.sequence.PhredEncoding;
import vn.ipz.phageipz.dto.response.chart.PointDTO;
import vn.ipz.phageipz.engine.qc.core.QCModule;
import vn.ipz.phageipz.engine.qc.core.ModuleConfig;

/**
 * Phân tích phân phối điểm chất lượng trung bình trên mỗi sequence.
 */
@Component
@Scope("prototype")
public class PerSequenceQualityScores implements QCModule {

	private final ModuleConfig moduleConfig;

	@Autowired
	public PerSequenceQualityScores(ModuleConfig moduleConfig) {
		this.moduleConfig = moduleConfig;
	}

	private HashMap<Integer, Long> averageScoreCounts = new HashMap<Integer, Long>();
	private double [] qualityDistribution = null;
	private int [] xCategories = new int[0];
	private char lowestChar = 126;
	private int maxCount = 0;
	private int mostFrequentScore;
	private boolean calculated = false;
	
	@Override
	public Object getResultsPanel() {
		return null;
	}

	/**
     * API trả về dữ liệu thô cho Frontend vẽ biểu đồ (D3.js/Chart.js)
     */
    public List<PointDTO> getChartData() {
        if (!calculated) calculateDistribution();
        List<PointDTO> data = new ArrayList<>();
        if (qualityDistribution == null) return data;

        for (int i = 0; i < xCategories.length; i++) {
            data.add(new PointDTO(String.valueOf(xCategories[i]), qualityDistribution[i]));
        }
        return data;
    }
	
	public boolean ignoreInReport () {
		if (moduleConfig.getParam("quality_sequence", "ignore") > 0  || averageScoreCounts.size() == 0) {
			return true;
		}
		return false;
	}
	
	private synchronized void calculateDistribution () {
		
		if (averageScoreCounts.size() == 0) {
			qualityDistribution = new double[0];
			xCategories = new int[0];
			calculated = true;
			return;
		}

		PhredEncoding encoding = PhredEncoding.getFastQEncodingOffset(lowestChar);
		
		Integer [] rawScores = averageScoreCounts.keySet().toArray(new Integer [0]);
		Arrays.sort(rawScores);
		
		// Khởi tạo mảng dựa trên dải điểm chất lượng xuất hiện
		qualityDistribution = new double [1+(rawScores[rawScores.length-1]-rawScores[0])] ;
		xCategories = new int[qualityDistribution.length];
		
		maxCount = 0;
		for (int i=0; i<qualityDistribution.length; i++) {
			int currentRawScore = rawScores[0] + i;
			xCategories[i] = currentRawScore - encoding.offset();
			
			if (averageScoreCounts.containsKey(currentRawScore)) {
				qualityDistribution[i] = averageScoreCounts.get(currentRawScore);
			}
			
			if (qualityDistribution[i] > maxCount) {
				maxCount = (int)qualityDistribution[i];
				mostFrequentScore = xCategories[i];
			}
		}
				
		calculated = true;
	}

	public void processSequence(Sequence sequence) {
		String qualityString = sequence.getQualityString();
		if (qualityString == null || qualityString.length() == 0) return;

		char [] seq = qualityString.toCharArray();
		long totalQuality = 0;
		
		for (int i=0; i<seq.length; i++) {
			if (seq[i] < lowestChar) {
				lowestChar = seq[i];
			}
			totalQuality += seq[i];
		}

		int averageQuality = (int)(totalQuality / seq.length);
				
		if (averageScoreCounts.containsKey(averageQuality)) {
			averageScoreCounts.put(averageQuality, averageScoreCounts.get(averageQuality) + 1);
		}
		else {
			averageScoreCounts.put(averageQuality, 1L);
		}
	}
	
	public void reset () {
		averageScoreCounts.clear();
		lowestChar = 126;
		maxCount = 0;
		calculated = false;
		qualityDistribution = null;
		xCategories = new int[0];
	}

	public String description() {
		return "Shows the distribution of average quality scores for whole sequences";
	}

	public String name() {
		return "Per sequence quality scores";
	}

	public boolean raisesError() {
		if (!calculated) calculateDistribution();
		if (xCategories.length == 0) return false;
		// SỬA: ModuleConfig.getParam -> moduleConfig.getParam
		return mostFrequentScore <= moduleConfig.getParam("quality_sequence", "error");
	}

	public boolean raisesWarning() {
		if (!calculated) calculateDistribution();
		if (xCategories.length == 0) return false;
		// SỬA: ModuleConfig.getParam -> moduleConfig.getParam
		return mostFrequentScore <= moduleConfig.getParam("quality_sequence", "warn");
	}

	public boolean ignoreFilteredSequences() {
		return true;
	}
}