package vn.ipz.phageipz.dto.response.chart;

public class LineChartDTO {
    private String[] xCategories;
    private double[][] series; // Cho phép nhiều đường biểu diễn nếu cần
    private String title;

    public LineChartDTO() {}

    public LineChartDTO(String[] xCategories, double[][] series, String title) {
        this.xCategories = xCategories;
        this.series = series;
        this.title = title;
    }

    // Getters và Setters
    public String[] getXCategories() { return xCategories; }
    public void setXCategories(String[] xCategories) { this.xCategories = xCategories; }
    public double[][] getSeries() { return series; }
    public void setSeries(double[][] series) { this.series = series; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}