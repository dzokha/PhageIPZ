package vn.ipz.phageipz.engine.qc.modules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import vn.ipz.phageipz.config.PhageIPZProperties;
import vn.ipz.phageipz.domain.sequence.Sequence;
import vn.ipz.phageipz.domain.sequence.PhredEncoding;
import vn.ipz.phageipz.engine.qc.util.QualityCount;
import vn.ipz.phageipz.dto.response.chart.TileDataDTO; // Đảm bảo đã import DTO
import vn.ipz.phageipz.engine.qc.core.QCModule;
import vn.ipz.phageipz.engine.qc.core.ModuleConfig;
import vn.ipz.phageipz.engine.qc.util.BaseGroup;


/**
 * Phân tích chất lượng dữ liệu theo từng Tile (ô) trên Flowcell.
 */
@Component
@Scope("prototype")
public class PerTileQualityScores implements QCModule {

	private final PhageIPZProperties properties;
	private final ModuleConfig moduleConfig;

	@Autowired
	public PerTileQualityScores(ModuleConfig moduleConfig, PhageIPZProperties properties) {
	    this.moduleConfig = moduleConfig;
	    this.properties = properties;
	}

	public HashMap<Integer, QualityCount []> perTileQualityCounts = new HashMap<Integer, QualityCount[]>();
	private int currentLength = 0;
	private double [][] means = null;
	private String [] xLabels;
	private int [] tiles;
	private int high = 0;
	private PhredEncoding encodingScheme;
	private boolean calculated = false;
	private long totalCount = 0;
	private int splitPosition = -1;
	private double maxDeviation = 0;
	private boolean ignoreInReport = false;

	@Override
	public Object getResultsPanel() {
		return null;
	}

	/**
	 * Trả về đối tượng chứa toàn bộ dữ liệu cần thiết để vẽ Heatmap trên Frontend.
	 * Dữ liệu này sẽ được Controller gọi và trả về dưới dạng JSON.
	 */
	public TileDataDTO getChartData() {
	    // Đảm bảo dữ liệu đã được tính toán (calculate percentages)
	    if (!calculated) {
	        getPercentages();
	    }

	    // Nếu không có dữ liệu, trả về DTO trống để tránh lỗi Null
	    if (means == null || tiles == null) {
	        return new TileDataDTO(new String[0], new int[0], new double[0][0], 0);
	    }

	    // Đóng gói dữ liệu vào DTO
	    return new TileDataDTO(
	        this.xLabels,
	        this.tiles,
	        this.means,
	        this.maxDeviation
	    );
	}

	public boolean ignoreFilteredSequences() {
		return true;
	}

	public boolean ignoreInReport () {
		// SỬA: ModuleConfig.getParam -> moduleConfig.getParam
		if (ignoreInReport || moduleConfig.getParam("tile", "ignore") > 0 || currentLength == 0) {
			return true;
		}
		return false;
	}

	private synchronized void getPercentages () {

		if (perTileQualityCounts.isEmpty()) {
	        calculated = true;
	        return;
	    }

	    char [] range = calculateOffsets();
	    encodingScheme = PhredEncoding.getFastQEncodingOffset(range[0]);
	    high = range[1] - encodingScheme.offset();
	    if (high < 35) high = 35;

	    // SỬA TẠI ĐÂY: Thay 'length' bằng 'this.currentLength'
	    BaseGroup[] groups = BaseGroup.makeBaseGroups(this.currentLength, this.properties);
	    
	    Integer [] tileNumbers = perTileQualityCounts.keySet().toArray(new Integer[0]);
	    Arrays.sort(tileNumbers);
	    
	    tiles = new int[tileNumbers.length];
	    for (int i=0; i<tiles.length; i++) {
	        tiles[i] = tileNumbers[i];
	    }
		
		means = new double[tileNumbers.length][groups.length];
		xLabels = new String[groups.length];

		for (int t=0; t<tileNumbers.length; t++){
			for (int i=0; i<groups.length; i++) {
				if (t == 0) xLabels[i] = groups[i].toString();

				int minBase = groups[i].lowerCount();
				int maxBase = groups[i].upperCount();
				means[t][i] = getMean(tileNumbers[t], minBase, maxBase, encodingScheme.offset());
			}
		}
		
		// Chuẩn hóa dữ liệu (Normalisation)
		double currentMaxDev = 0;
		double [] averageQualitiesPerGroup = new double[groups.length];
		
		for (int t=0; t<tileNumbers.length; t++) {		
			for (int i=0; i<groups.length; i++) {
				averageQualitiesPerGroup[i] += means[t][i];
			}
		}
		
		for (int i=0; i<averageQualitiesPerGroup.length; i++) {
			averageQualitiesPerGroup[i] /= tileNumbers.length;
		}

		for (int i=0; i<groups.length; i++) {
			for (int t=0; t<tileNumbers.length; t++) {
				means[t][i] -= averageQualitiesPerGroup[i];
				if (Math.abs(means[t][i]) > currentMaxDev) {
					currentMaxDev = Math.abs(means[t][i]);
				}
			}
		}
		
		this.maxDeviation = currentMaxDev;
		calculated = true;
	}

	private char [] calculateOffsets () {
		char minChar = 0;
		char maxChar = 0;

		for (QualityCount[] qualityCounts : perTileQualityCounts.values()) {
			for (QualityCount qc : qualityCounts) {
				if (minChar == 0) {
					minChar = qc.getMinChar();
					maxChar = qc.getMaxChar();
				} else {
					if (qc.getMinChar() < minChar && qc.getMinChar() != 0) minChar = qc.getMinChar();
					if (qc.getMaxChar() > maxChar) maxChar = qc.getMaxChar();
				}
			}
		}
		return new char[] {minChar, maxChar};
	}

	public void processSequence(Sequence sequence) {

		if (totalCount == 0 && moduleConfig.getParam("tile", "ignore") > 0) {
			ignoreInReport = true;
		}

		if (ignoreInReport || sequence.getQualityString().length() == 0) return;
				
		calculated = false;
		totalCount++;
		
		// Sampling: Lấy 10k seq đầu tiên, sau đó lấy 10%
		if (totalCount > 10000 && totalCount % 10 != 0) return;
		
		int tile = -1;
		String [] splitID = sequence.getID().split(":");

		try {
			if (splitPosition >= 0) {
				if (splitID.length > splitPosition) {
					tile = Integer.parseInt(splitID[splitPosition]);
				}
			} else if (splitID.length >= 7) {
				splitPosition = 4;
				tile = Integer.parseInt(splitID[4]);
			} else if (splitID.length >= 5) {
				splitPosition = 2;
				tile = Integer.parseInt(splitID[2]);
			} else {
				ignoreInReport = true;
				return;
			}
		} catch (NumberFormatException nfe) {
			ignoreInReport = true;
			return;
		}

		if (tile == -1) return;

		char [] qual = sequence.getQualityString().toCharArray();
		if (currentLength < qual.length) {
			expandQualityCounts(qual.length);
			currentLength = qual.length;
		}

		if (!perTileQualityCounts.containsKey(tile)) {
			if (perTileQualityCounts.size() > 2500) {
				ignoreInReport = true;
				perTileQualityCounts.clear();
				return;
			}
			QualityCount [] qualityCounts = new QualityCount[currentLength];
			for (int i=0; i<currentLength; i++) qualityCounts[i] = new QualityCount();
			perTileQualityCounts.put(tile, qualityCounts);
		}

		QualityCount [] qualityCounts = perTileQualityCounts.get(tile);
		for (int i=0; i<qual.length; i++) {
			qualityCounts[i].addValue(qual[i]);
		}
	}

	private void expandQualityCounts(int newLength) {
		for (Integer thisTile : perTileQualityCounts.keySet()) {
			QualityCount [] oldCounts = perTileQualityCounts.get(thisTile);
			QualityCount [] newCounts = new QualityCount[newLength];
			System.arraycopy(oldCounts, 0, newCounts, 0, oldCounts.length);
			for (int i=oldCounts.length; i<newLength; i++) {
				newCounts[i] = new QualityCount();				
			}
			perTileQualityCounts.put(thisTile, newCounts);
		}
	}

	public void reset () {
		totalCount = 0;
		perTileQualityCounts.clear();
		calculated = false;
		currentLength = 0;
		splitPosition = -1;
	}

	public String description() {
		return "Shows the per tile quality scores of all bases at a given position in a sequencing run";
	}

	public String name() {
		return "Per tile sequence quality";
	}

	public boolean raisesError() {
		if (!calculated) getPercentages();
		// SỬA: ModuleConfig.getParam -> moduleConfig.getParam
		return maxDeviation > moduleConfig.getParam("tile", "error");
	}

	public boolean raisesWarning() {
		if (!calculated) getPercentages();
		// SỬA: ModuleConfig.getParam -> moduleConfig.getParam
		return maxDeviation > moduleConfig.getParam("tile", "warn");
	}


	private double getMean (int tile, int minbp, int maxbp, int offset) {
		int count = 0;
		double total = 0;
		QualityCount [] qualityCounts = perTileQualityCounts.get(tile);
		if (qualityCounts == null) return 0;

		for (int i=minbp-1; i<maxbp; i++) {
			if (i < qualityCounts.length && qualityCounts[i].getTotalCount() > 0) {
				count++;
				total += qualityCounts[i].getMean(offset);
			}
		}
		return count > 0 ? total/count : 0;
	}
}