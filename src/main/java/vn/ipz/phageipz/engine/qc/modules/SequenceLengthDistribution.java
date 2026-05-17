package vn.ipz.phageipz.engine.qc.modules;

import java.io.IOException;

import vn.ipz.phageipz.config.PhageIPZProperties;
import vn.ipz.phageipz.domain.sequence.Sequence;
import vn.ipz.phageipz.dto.response.chart.LineChartDTO; // Import DTO mới
import vn.ipz.phageipz.engine.qc.core.QCModule;
import vn.ipz.phageipz.engine.qc.core.ModuleConfig;

public class SequenceLengthDistribution implements QCModule {

	private final PhageIPZProperties properties;
	private long [] lengthCounts = new long[0];
	private double [] graphCounts = null;
	private String [] xCategories = new String[0];
	private double max = 0;
	private boolean calculated = false;

	public SequenceLengthDistribution(PhageIPZProperties properties) {
        this.properties = properties;
    }
	
	@Override
	public Object getResultsPanel() {
	
		if (!calculated) calculateDistribution();

		if (graphCounts == null) return null;

        // Trả về DTO thay vì LineGraph
        return new LineChartDTO(
            xCategories, 
            new double[][] {graphCounts}, 
            "Distribution of sequence lengths over all sequences"
        );
	}

	// Bổ sung hàm này để cung cấp dữ liệu phẳng cho REST API
	public java.util.List<vn.ipz.phageipz.dto.response.chart.PointDTO> getChartData() {
		if (!calculated) calculateDistribution();
		java.util.List<vn.ipz.phageipz.dto.response.chart.PointDTO> data = new java.util.ArrayList<>();
		if (graphCounts == null || xCategories == null) return data;
		
		for (int i = 0; i < xCategories.length; i++) {
			data.add(new vn.ipz.phageipz.dto.response.chart.PointDTO(xCategories[i], graphCounts[i]));
		}
		return data;
	}
	
	public boolean ignoreFilteredSequences() {
		return true;
	}

	@Override
	public boolean ignoreInReport() {
	    return properties.getAnalysis().isIgnoreSequenceLength();
	}
	
	private synchronized void calculateDistribution () {
		int maxLen = 0;
		int minLen = -1;
		max = 0;
		
		for (int i=0; i<lengthCounts.length; i++) {
			if (lengthCounts[i]>0) {
				if (minLen < 0) minLen = i;
				maxLen = i;
			}
		}
		
		if (minLen < 0) minLen = 0;
		if (minLen > 0) minLen--;
		maxLen++;
		
		int [] startAndInterval = getSizeDistribution(minLen, maxLen);
				
		int categories = 0;
		int currentValue = startAndInterval[0];
		while (currentValue <= maxLen) {
			++categories;
			currentValue += startAndInterval[1];
		}
		
		graphCounts = new double[categories];
		xCategories = new String[categories];
		
		for (int i=0; i<graphCounts.length; i++) {
			int minValue = startAndInterval[0] + (startAndInterval[1] * i);
			int maxValue = (startAndInterval[0] + (startAndInterval[1] * (i+1))) - 1;

			if (maxValue > maxLen) maxValue = maxLen;
			
			for (int bp=minValue; bp<=maxValue; bp++) {
				if (bp < lengthCounts.length) {
					graphCounts[i] += lengthCounts[bp];
				}
			}

			if (startAndInterval[1] == 1) {
				xCategories[i] = "" + minValue;
			} else {
				xCategories[i] = minValue + "-" + maxValue;
			}
			
			if (graphCounts[i] > max) max = graphCounts[i];
		}
		calculated = true;
	}

	// Tối ưu hóa: Sử dụng tham số từ FastQCProperties thay vì SOAPConfig
    private int[] getSizeDistribution(int min, int max) {
        // Kiểm tra cấu hình nogroup từ file application.yml thông qua properties
        if (properties.getAnalysis().isNogroup()) {
            return (new int[] {min, 1});
        }

        int base = 1;
        while (base > (max - min)) {
            base /= 10;
        }

        int interval;
        int[] divisions = new int[] {1, 2, 5};

        OUTER: while (true) {
            for (int d = 0; d < divisions.length; d++) {
                int tester = base * divisions[d];
                if (tester > 0 && ((max - min) / tester) <= 50) {
                    interval = tester;
                    break OUTER;
                }
            }
            base *= 10;
        }

        int starting = (min / interval) * interval;
        return new int[] {starting, interval};
    }

	@Override
	public void processSequence(Sequence sequence) {
        // Sửa lỗi tiềm ẩn: Đảm bảo class Sequence có phương thức getSequence() trả về String
		int seqLen = sequence.getSequence().length();

		if (seqLen + 2 > lengthCounts.length) {
			long [] newLengthCounts = new long[seqLen + 2];
			System.arraycopy(lengthCounts, 0, newLengthCounts, 0, lengthCounts.length);
			lengthCounts = newLengthCounts;
		}
		
		++lengthCounts[seqLen];
	}
	

	@Override
    public void reset() {
        lengthCounts = new long[0];
        calculated = false;
        graphCounts = null;
        xCategories = new String[0];
    }

	@Override
	public String description() {
		return "Shows the distribution of sequence length over all sequences";
	}

	@Override
	public String name() {
		return "Sequence Length Distribution";
	}

	@Override
	public boolean raisesError() {
		// if (!calculated) calculateDistribution();
		// if (ModuleConfig.getParam("sequence_length", "error") == 0) return false;

		// if (lengthCounts.length > 0 && lengthCounts[0] > 0) return true;
		return false;
	}

	@Override
	public boolean raisesWarning() {
		// if (!calculated) calculateDistribution();
		// if (ModuleConfig.getParam("sequence_length", "warn") == 0) return false;
		
		// boolean seenLength = false;
		// for (int i=0; i<lengthCounts.length; i++) {
		// 	if (lengthCounts[i] > 0) {
		// 		if (seenLength) return true;
		// 		else seenLength = true;
		// 	}
		// }
		return false;
	}
}