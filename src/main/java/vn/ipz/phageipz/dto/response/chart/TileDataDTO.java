package vn.ipz.phageipz.dto.response.chart;

/**
 * Data Transfer Object chứa dữ liệu phân tích Per Tile Quality.
 * Phiên bản không sử dụng Lombok để đảm bảo tương thích tối đa.
 */
public class TileDataDTO {
    
    private String[] xLabels;      // Các vị trí Base (trục X)
    private int[] tiles;           // Các mã Tile (trục Y)
    private double[][] means;      // Ma trận giá trị sai lệch chất lượng (Heatmap values)
    private double maxDeviation;    // Giá trị sai lệch lớn nhất

    // 1. Constructor không đối số (Bắt buộc cho việc Serialization/Deserialization JSON)
    public TileDataDTO() {}

    // 2. Constructor đầy đủ đối số
    public TileDataDTO(String[] xLabels, int[] tiles, double[][] means, double maxDeviation) {
        this.xLabels = xLabels;
        this.tiles = tiles;
        this.means = means;
        this.maxDeviation = maxDeviation;
    }

    // 3. Getters và Setters
    
    public String[] getXLabels() {  return xLabels;}
    public void setXLabels(String[] xLabels) {  this.xLabels = xLabels;  }

    public int[] getTiles() {  return tiles;  }
    public void setTiles(int[] tiles) {  this.tiles = tiles; }

    public double[][] getMeans() {    return means; }
    public void setMeans(double[][] means) {     this.means = means; }

    public double getMaxDeviation() {       return maxDeviation;    }
    public void setMaxDeviation(double maxDeviation) {        this.maxDeviation = maxDeviation;   }
}