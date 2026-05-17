package vn.ipz.phageipz.mapper;

import vn.ipz.phageipz.dto.response.AnalysisReportDTO;
import vn.ipz.phageipz.dto.response.chart.PointDTO;
import vn.ipz.phageipz.domain.job.AnalysisResult;
import vn.ipz.phageipz.engine.qc.core.*;
import vn.ipz.phageipz.engine.qc.modules.*;
import vn.ipz.phageipz.engine.qc.modules.adapter.AdapterContent;

import java.util.List;
import java.util.ArrayList;

public class AnalysisReportMapper {

    public static AnalysisReportDTO toDTO(AnalysisResult result) {
        AnalysisReportDTO dto = new AnalysisReportDTO();
        dto.setFileName(result.getFileName());
        dto.setStatus(result.getStatus());

        // ====================================================================
        // GẮP DỮ LIỆU THẬT TỪ CÁC MODULES
        // ====================================================================
        if (result.getModules() != null) {
            for (QCModule module : result.getModules()) {

                if (module instanceof BasicStats basicStats) {
                    dto.setBasicStats(basicStats.getFullStatsForWeb());
                }

                // 1. Adapter Content & Per Base Quality
                if (module instanceof AdapterContent adapter) {
                    dto.setAdapterData(adapter.getAdapterContentData());
                } 
                else if (module instanceof PerBaseQualityScores qualityModule) {
                    dto.setPerBaseQualityData(qualityModule.getBoxPlotData());
                }

                // 2. Per Sequence Quality Scores
                else if (module instanceof PerSequenceQualityScores seqQual) {
                    List<PointDTO> chartData = seqQual.getChartData();
                    if (chartData != null) {
                        for (PointDTO p : chartData) {
                            dto.getPerSeqQualityData().add(new AnalysisReportDTO.PerSeqQuality(
                                Integer.parseInt(p.x), 
                                (long) p.y             
                            ));
                        }
                    }
                }

                // 3. Per Base Sequence Content
                else if (module instanceof PerBaseSequenceContent baseContent) {
                    List<double[]> data = baseContent.getRawData();
                    if (data != null && data.size() == 4 && baseContent.xCategories != null) {
                        String[] labels = baseContent.xCategories;
                        double[] t = data.get(0);
                        double[] c = data.get(1);
                        double[] a = data.get(2);
                        double[] g = data.get(3);
                        
                        for (int i = 0; i < labels.length; i++) {
                            dto.getPerBaseContentData().add(new AnalysisReportDTO.PerBaseContent(
                                labels[i], a[i], c[i], g[i], t[i]
                            ));
                        }
                    }
                }

                // 4. Per Sequence GC Content
                else if (module instanceof PerSequenceGCContent gcContent) {
                    List<double[]> gcData = gcContent.getChartData();
                    if (gcData != null && gcData.size() == 2) {
                        double[] actual = gcData.get(0);
                        double[] theoretical = gcData.get(1);
                        for (int i = 0; i < actual.length; i++) {
                            dto.getGcContentData().add(new AnalysisReportDTO.GcContent(
                                i, (long) actual[i], (long) theoretical[i]
                            ));
                        }
                    }
                }

                // 5. Per Base N Content
                else if (module instanceof NContent nContent) {
                    List<PointDTO> nData = nContent.getChartData();
                    if (nData != null) {
                        for (PointDTO p : nData) {
                            dto.getNContentData().add(new AnalysisReportDTO.NContent(
                                p.x, p.y 
                            ));
                        }
                    }
                }

                // 6. Sequence Length Distribution (ĐÃ HOÀN THIỆN)
                else if (module instanceof SequenceLengthDistribution lenDist) {
                    List<PointDTO> lenData = lenDist.getChartData();
                    if (lenData != null) {
                        for (PointDTO p : lenData) {
                            // Cắt chuỗi để lấy số (VD: "150-154" -> 150) để DTO khỏi bị lỗi ép kiểu int
                            int lengthVal = 0;
                            try {
                                lengthVal = Integer.parseInt(p.x.split("-")[0]);
                            } catch (Exception e) {
                                lengthVal = 0; // Fallback an toàn
                            }
                            
                            dto.getLengthDistributionData().add(new AnalysisReportDTO.LengthDistribution(
                                lengthVal, 
                                (long) p.y
                            ));
                        }
                    }
                }

                // 7. Sequence Duplication Levels
                else if (module instanceof DuplicationLevel dupLevel) {
                    List<DuplicationLevel.DupPoint> dupData = dupLevel.getChartData();
                    if (dupData != null) {
                        for (DuplicationLevel.DupPoint p : dupData) {
                            // Truyền vào cả p.dedupPercent() và p.totalPercent()
                            dto.getDuplicationLevelsData().add(new AnalysisReportDTO.DuplicationLevel(
                                p.label(), 
                                p.dedupPercent(), 
                                p.totalPercent() 
                            ));
                        }
                    }
                }

                // 8. Overrepresented Sequences
                else if (module instanceof OverRepresentedSeqs overRep) {
                    List<OverRepresentedSeqs.OverrepresentedData> overData = overRep.getChartData();
                    if (overData != null) {
                        for (OverRepresentedSeqs.OverrepresentedData p : overData) {
                            dto.getOverrepresentedData().add(new AnalysisReportDTO.OverrepresentedSeq(
                                p.sequence(), 
                                p.count(), 
                                p.percentage(), 
                                p.source()
                            ));
                        }
                    }
                }

                // 9. Kmer Content
                else if (module instanceof vn.ipz.phageipz.engine.qc.modules.kmer.KmerContent kmerContent) {
                    List<vn.ipz.phageipz.engine.qc.modules.kmer.KmerContent.KmerSeries> kmerChartData = kmerContent.getChartData();
                    if (kmerChartData != null) {
                        for (vn.ipz.phageipz.engine.qc.modules.kmer.KmerContent.KmerSeries series : kmerChartData) {
                            List<AnalysisReportDTO.KmerPoint> points = new ArrayList<>();
                            for (vn.ipz.phageipz.engine.qc.modules.kmer.KmerContent.KmerPoint p : series.points()) {
                                points.add(new AnalysisReportDTO.KmerPoint(p.position(), p.value()));
                            }
                            dto.getKmerData().add(new AnalysisReportDTO.KmerSeries(series.kmer(), points));
                        }
                    }
                }
            }
        }

        return dto;
    }
}