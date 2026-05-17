package vn.ipz.phageipz.engine.qc.util;

import java.awt.Color;

/**
 * Cung cấp dải màu chuyển sắc mượt mà từ Xanh dương (Lạnh) 
 * qua Xanh lá đến Đỏ (Nóng).
 * Phiên bản tối ưu cho môi trường Headless/Web.
 */
public class HotColdColourGradient {
	
	// Pre-cached palette để tiết kiệm tài nguyên và bộ nhớ
	private final Color [] colors;
	
	public HotColdColourGradient() {
		this.colors = makeColors();
	}

	@Override
	public String toString() {
		return "Hot Cold Colour Gradient";
	}
	
	/**
	 * Tạo ra một bảng 100 màu để dùng chung. 
	 * Sử dụng thang đo căn bậc hai (log-like) để làm nổi bật các giá trị 
	 * cực thấp và cực cao, tránh việc màu xanh lá chiếm quá nhiều diện tích.
	 */
	protected Color [] makeColors() {
		Color [] colorCache = new Color[100];
		
		// Tính toán min/max dựa trên căn bậc hai để tạo độ dốc phi tuyến tính
		double min = 0 - Math.pow(50, 0.5);
		double max = Math.pow(99-50, 0.5);

		for (int c=0; c<100; c++) {
			int actualC = c - 50;
			if (actualC < 0) actualC = -actualC;
			
			double corrected = Math.pow(actualC, 0.5);
			if (c < 50 && corrected > 0) corrected = -corrected;
			
			RGB rgb = getRGB(corrected, min, max);
			
			// GIẢI PHÁP CHO LỖI BUILD: 
			// Sử dụng phương thức toAwtColor() đã viết trong lớp RGB
			colorCache[c] = rgb.toAwtColor();
		}
				
		return colorCache;
	}
	
	/**
	 * Lấy màu tương ứng với giá trị đầu vào.
	 */
	public Color getColor (double value, double min, double max) {
		// Tránh chia cho 0 nếu min == max
		if (Math.abs(max - min) < 1e-9) return colors[0];

		int percentage = (int)((100 * (value-min)) / (max-min));
	
		if (percentage > 100) percentage = 100;
		if (percentage < 1) percentage = 1;
		
		return colors[percentage-1];
	}

	/**
	 * Logic tính toán giá trị RGB riêng lẻ dựa trên 4 phân vùng của dải phổ.
	 */
	private RGB getRGB (double value, double min, double max) {
		int red, green, blue;
		double diff = max - min;
		
		/* * Chia dải phổ thành 4 phần (quarters):
		 * 1. Blue (200) -> Blue (200) + Green (0-200)
		 * 2. Green (200) + Blue (200-0)
		 * 3. Green (200) + Red (0-200)
		 * 4. Red (200) + Green (200-0)
		 */
		
		if (value < (min + (diff * 0.25))) {
			red = 0;
			blue = 200;
			green = (int)(200 * ((value - min) / (diff * 0.25)));
		}
		else if (value < (min + (diff * 0.5))) {
			red = 0;
			green = 200;
			blue = (int)(200 - (200 * ((value - (min + (diff * 0.25))) / (diff * 0.25))));
		}
		else if (value < (min + (diff * 0.75))) {
			green = 200;
			blue = 0;
			red = (int)(200 * ((value - (min + (diff * 0.5))) / (diff * 0.25)));
		}
		else {
			red = 200;
			blue = 0;
			green = (int)(200 - (200 * ((value - (min + (diff * 0.75))) / (diff * 0.25))));
		}

		return new RGB(red, green, blue);
	}
}