package vn.ipz.phageipz.dto.response.chart;

import java.util.List;

public class BoxPlotPointDTO {
    private String base;
    private double lw;
    private double q1;
    private double median;
    private double q3;
    private double uw;
    private double mean;

    public BoxPlotPointDTO(String base, double lw, double q1, double median, double q3, double uw, double mean) {
        this.base = base;
        this.lw = lw;
        this.q1 = q1;
        this.median = median;
        this.q3 = q3;
        this.uw = uw;
        this.mean = mean;
    }

    public String getBase() { return base; }
    public double getLw() { return lw; }
    public double getQ1() { return q1; }
    public double getMedian() { return median; }
    public double getQ3() { return q3; }
    public double getUw() { return uw; }
    public double getMean() { return mean; }
}