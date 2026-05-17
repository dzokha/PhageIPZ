package vn.ipz.phageipz.engine.qc.modules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList; // Thêm import
import java.util.List;      // Thêm import

import java.io.IOException;
import javax.xml.stream.XMLStreamException;

import vn.ipz.phageipz.config.PhageIPZProperties; // THÊM IMPORT
import vn.ipz.phageipz.domain.sequence.Sequence;
import vn.ipz.phageipz.dto.response.chart.PointDTO; // Giả định bạn đã có DTO này
import vn.ipz.phageipz.engine.qc.core.QCModule;
import vn.ipz.phageipz.engine.qc.core.ModuleConfig;
import vn.ipz.phageipz.engine.qc.util.BaseGroup;


@Component
@Scope("prototype")
public class PerBaseSequenceContent implements QCModule {

	private final ModuleConfig moduleConfig;
    private final PhageIPZProperties properties; // THÊM BIẾN PROPERTIES

    @Autowired
    public PerBaseSequenceContent(ModuleConfig moduleConfig, PhageIPZProperties properties) {
        this.moduleConfig = moduleConfig;
        this.properties = properties; // INJECT VÀO CONSTRUCTOR
    }

	public long [] gCounts = new long [0];
	public long [] aCounts = new long [0];
	public long [] cCounts = new long [0];
	public long [] tCounts = new long [0];
	private double [][] percentages = null;
	public String [] xCategories = new String[0];
	private boolean calculated = false;

	@Override
	public Object getResultsPanel() { 
		return null;
	}

	/**
     * Cung cấp dữ liệu cho Frontend vẽ bằng D3.js hoặc Chart.js
     */
    public List<double[]> getRawData() {
        if (!calculated) getPercentages();
        List<double[]> data = new ArrayList<>();
        if (percentages != null) {
            for (double[] p : percentages) {
                data.add(p);
            }
        }
        return data;
    }
	
	public boolean ignoreFilteredSequences() {
		return true;
	}
	
	public boolean ignoreInReport () {
		if (moduleConfig.getParam("sequence", "ignore") > 0) {
			return true;
		}
		return false;
	}

	private synchronized void getPercentages () {
		if (gCounts.length == 0) return;

		BaseGroup [] groups = BaseGroup.makeBaseGroups(gCounts.length, this.properties);
		xCategories = new String[groups.length];

		double [] gPercent = new double[groups.length];
		double [] aPercent = new double[groups.length];
		double [] tPercent = new double[groups.length];
		double [] cPercent = new double[groups.length];

		for (int i=0; i<groups.length; i++) {
			xCategories[i] = groups[i].toString();

			long gCount = 0;
			long aCount = 0;
			long tCount = 0;
			long cCount = 0;
			long total = 0;
			
			for (int bp=groups[i].lowerCount()-1; bp<groups[i].upperCount(); bp++) {
				if (bp >= gCounts.length) break;

				total += gCounts[bp] + cCounts[bp] + aCounts[bp] + tCounts[bp];
				aCount += aCounts[bp];
				tCount += tCounts[bp];
				cCount += cCounts[bp];
				gCount += gCounts[bp];				
			}
			
			if (total > 0) {
				gPercent[i] = (gCount/(double)total)*100;
				aPercent[i] = (aCount/(double)total)*100;
				tPercent[i] = (tCount/(double)total)*100;
				cPercent[i] = (cCount/(double)total)*100;
			} else {
				gPercent[i] = aPercent[i] = tPercent[i] = cPercent[i] = 0;
			}
		}
		
		percentages = new double [][] {tPercent, cPercent, aPercent, gPercent};
		calculated = true;
	}
	
	public void processSequence(Sequence sequence) {
		calculated = false;
		char [] seq = sequence.getSequence().toCharArray();
		
		if (gCounts.length < seq.length) {
			int newLen = seq.length;
			long [] gCountsNew = new long [newLen];
			long [] aCountsNew = new long [newLen];
			long [] cCountsNew = new long [newLen];
			long [] tCountsNew = new long [newLen];

			System.arraycopy(gCounts, 0, gCountsNew, 0, gCounts.length);
			System.arraycopy(aCounts, 0, aCountsNew, 0, aCounts.length);
			System.arraycopy(tCounts, 0, tCountsNew, 0, tCounts.length);
			System.arraycopy(cCounts, 0, cCountsNew, 0, cCounts.length);

			gCounts = gCountsNew;
			aCounts = aCountsNew;
			tCounts = tCountsNew;
			cCounts = cCountsNew;
		}
		
		for (int i=0; i<seq.length; i++) {
			switch (seq[i]) {
				case 'G': ++gCounts[i]; break;
				case 'A': ++aCounts[i]; break;
				case 'T': ++tCounts[i]; break;
				case 'C': ++cCounts[i]; break;
			}
		}
	}
	
	public void reset () {
		gCounts = new long[0];
		aCounts = new long[0];
		tCounts = new long[0];
		cCounts = new long[0];
		calculated = false;
	}

	public String description() {
		return "Shows the relative amounts of each base at each position in a sequencing run";
	}

	public String name() {
		return "Per base sequence content";
	}

	public boolean raisesError() {
		if (!calculated) getPercentages();
		if (percentages == null) return false;

		for (int i=0; i<percentages[0].length; i++) {
			double gcDiff = Math.abs(percentages[1][i]-percentages[3][i]);
			double atDiff = Math.abs(percentages[0][i]-percentages[2][i]);
			if (gcDiff > moduleConfig.getParam("sequence", "error") || atDiff > moduleConfig.getParam("sequence", "error")) return true;
		}
		return false;
	}

	public boolean raisesWarning() {
		if (!calculated) getPercentages();
		if (percentages == null) return false;

		for (int i=0; i<percentages[0].length; i++) {
			double gcDiff = Math.abs(percentages[1][i]-percentages[3][i]);
			double atDiff = Math.abs(percentages[0][i]-percentages[2][i]);
			if (gcDiff > moduleConfig.getParam("sequence", "warn") || atDiff > moduleConfig.getParam("sequence", "warn")) return true;
		}
		return false;
	}
}