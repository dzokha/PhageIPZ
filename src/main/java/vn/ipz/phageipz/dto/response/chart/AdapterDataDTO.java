package vn.ipz.phageipz.dto.response.chart;

import java.util.List;

public class AdapterDataDTO {
    public String adapterName;
    public List<PointDTO> points;

    public AdapterDataDTO(String name, List<PointDTO> points) {
        this.adapterName = name;
        this.points = points;
    }
}