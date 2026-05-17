package vn.ipz.phageipz.engine.qc.util;

import java.awt.Color;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Lớp đại diện cho màu sắc RGB, hỗ trợ chuyển đổi Web (JSON/Hex) 
 * và đồ họa Server-side (AWT).
 */
public class RGB {
    
    private final int r;
    private final int g;
    private final int b;

    // Hằng số màu sắc thường dùng trong báo cáo SOAP
    public static final RGB WHITE = new RGB(255, 255, 255);
    public static final RGB BLACK = new RGB(0, 0, 0);
    public static final RGB RED   = new RGB(255, 0, 0);
    public static final RGB GREEN = new RGB(0, 255, 0);
    public static final RGB BLUE  = new RGB(0, 0, 255);

    /**
     * Khởi tạo màu RGB với ràng buộc giá trị 0-255.
     */
    public RGB(@JsonProperty("r") int r, 
               @JsonProperty("g") int g, 
               @JsonProperty("b") int b) {
        this.r = Math.max(0, Math.min(255, r));
        this.g = Math.max(0, Math.min(255, g));
        this.b = Math.max(0, Math.min(255, b));
    }

    // Getters theo chuẩn JavaBean cho Jackson Serialization
    public int getR() { return r; }
    public int getG() { return g; }
    public int getB() { return b; }

    // Chuyển đổi định dạng
    public String toHex() { 
        return String.format("#%02x%02x%02x", r, g, b); 
    }

    public Color toAwtColor() { 
        return new Color(r, g, b); 
    }

    public static RGB fromAwtColor(Color color) { 
        if (color == null) return BLACK;
        return new RGB(color.getRed(), color.getGreen(), color.getBlue()); 
    }

    // Override các phương thức cơ bản của Object
    @Override
    public String toString() { 
        return "rgb(" + r + "," + g + "," + b + ")"; 
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RGB rgb = (RGB) o;
        return r == rgb.r && g == rgb.g && b == rgb.b;
    }

    @Override
    public int hashCode() {
        return Objects.hash(r, g, b);
    }
}