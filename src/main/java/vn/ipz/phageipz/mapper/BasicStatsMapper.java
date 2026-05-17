package vn.ipz.phageipz.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import vn.ipz.phageipz.engine.qc.modules.BasicStats;
import vn.ipz.phageipz.dto.response.BasicStatsDTO;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BasicStatsMapper {
    // Để MapStruct tự tìm các trường trùng tên (nếu có). 
    // Nếu không trùng, nó sẽ để null/default nhờ ReportingPolicy.IGNORE
    BasicStatsDTO toDTO(BasicStats basicStats);
}