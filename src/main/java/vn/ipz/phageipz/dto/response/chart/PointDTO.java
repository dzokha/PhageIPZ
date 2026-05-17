package vn.ipz.phageipz.dto.response.chart;

public class PointDTO {
    public String x; // Ví dụ: "1-5", "6-10" (vị trí bp)
    public double y; // Tỷ lệ % adapter

    public PointDTO(String x, double y) {
        this.x = x;
        this.y = y;
    }
}