/*
 * Xin lưu ý: File này chứa mã nguồn gốc từ dự án FastQC.
 * Copyright (C) 2010-2011 Simon Andrews
 * * Đã được chỉnh sửa và tái cấu trúc (Refactored) vào năm 2026 bởi:
 * Dzokha - Dự án SOAP
 * Tối ưu hóa: Loại bỏ File I/O, sử dụng Caching từ QCConfigManager.
 * * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 */
package vn.ipz.phageipz.engine.qc.modules.adapter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;

import java.util.Vector;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import vn.ipz.phageipz.config.PhageIPZProperties;
import vn.ipz.phageipz.config.QCConfigManager; // IMPORT QUẢN LÝ CẤU HÌNH TRÊN RAM
import vn.ipz.phageipz.domain.sequence.Sequence;
import vn.ipz.phageipz.engine.qc.core.QCModule;
import vn.ipz.phageipz.engine.qc.core.ModuleConfig;
import vn.ipz.phageipz.engine.qc.util.BaseGroup;

import vn.ipz.phageipz.dto.response.chart.AdapterDataDTO;
import vn.ipz.phageipz.dto.response.chart.PointDTO;

@Component
@Scope("prototype")
public class AdapterContent implements QCModule {

    private final PhageIPZProperties properties;
    private final ModuleConfig moduleConfig; 

    private int longestSequence = 0;
    private int longestAdapter = 0;
    private long totalCount = 0;
    private boolean calculated = false;

    private Adapter[] adapters;
    private double[][] enrichments = null;
    private String[] labels;
    private String[] xLabels = new String[0];
    private BaseGroup[] groups;

    @Override
    public boolean isCalculated() {
        return this.calculated; 
    }

    /**
     * TỐI ƯU HÓA: Inject trực tiếp QCConfigManager.
     * Không còn dùng ResourceLoader hay BufferedReader ở đây nữa để tránh thắt nút cổ chai I/O.
     */
    @Autowired
    public AdapterContent(PhageIPZProperties properties, ModuleConfig moduleConfig, QCConfigManager qcConfigManager) {
        this.properties = properties;
        this.moduleConfig = moduleConfig;
        
        Vector<Adapter> c = new Vector<Adapter>();
        Vector<String> l = new Vector<String>();

        // Lấy danh sách Adapter đã được nạp sẵn lên RAM từ lúc Server khởi động
        Map<String, String> loadedAdapters = qcConfigManager.getAllAdapters();
        
        if (loadedAdapters != null) {
            for (Map.Entry<String, String> entry : loadedAdapters.entrySet()) {
                Adapter adapter = new Adapter(entry.getKey(), entry.getValue());
                c.add(adapter);
                l.add(adapter.name());
                
                if (adapter.sequence().length() > longestAdapter) {
                    longestAdapter = adapter.sequence().length();
                }
            }
        }
        
        labels = l.toArray(new String[0]);
        adapters = c.toArray(new Adapter[0]);
    }

    // Chuẩn hóa Web: Trả về Object thay vì JPanel
    @Override
    public Object getResultsPanel() {
        return null; // Trả về null vì dùng D3.js trên Frontend
    }

    @Override
    public void processSequence(Sequence sequence) {
        this.calculated = false;
        ++totalCount;

        if (sequence.getSequence().length() > longestSequence && sequence.getSequence().length() - longestAdapter > 0) {
            longestSequence = sequence.getSequence().length();
            for (Adapter adapter : adapters) {
                adapter.expandLengthTo((longestSequence - longestAdapter) + 1);
            }
        }

        for (Adapter adapter : adapters) {
            int index = sequence.getSequence().indexOf(adapter.sequence());
            if (index >= 0) {
                for (int i = index; i <= longestSequence - longestAdapter; i++) {
                    adapter.incrementCount(i);
                }
            }
        }
    }

    public synchronized void calculateEnrichment() {
        if (totalCount == 0) return;
        int maxLength = 0;
        for (Adapter adapter : adapters) {
            if (adapter.getPositions().length > maxLength) {
                maxLength = adapter.getPositions().length;
            }
        }

        groups = BaseGroup.makeBaseGroups(maxLength, this.properties);
        xLabels = new String[groups.length];
        for (int i = 0; i < xLabels.length; i++) {
            xLabels[i] = groups[i].toString();
        }

        enrichments = new double[adapters.length][groups.length];

        for (int a = 0; a < adapters.length; a++) {
            long[] positions = adapters[a].positions;
            for (int g = 0; g < groups.length; g++) {
                for (int p = groups[g].lowerCount() - 1; p < groups[g].upperCount() && p < positions.length; p++) {
                    enrichments[a][g] += (positions[p] * 100d) / totalCount;
                }
                enrichments[a][g] /= (groups[g].upperCount() - groups[g].lowerCount()) + 1;
            }
        }
        calculated = true;
    }

    @Override
    public void reset() {
        calculated = false;
        totalCount = 0;
        longestSequence = 0;
        for (Adapter adapter : adapters) {
            adapter.reset();
        }
    }

    @Override
    public String description() { return "Searches for specific adapter sequences in a library"; }
    
    @Override
    public String name() { return "Adapter Content"; }

    @Override
    public boolean raisesError() {
        if (!calculated) calculateEnrichment();
        if (enrichments == null) return false;
        for (double[] enrichment : enrichments) {
            for (double v : enrichment) {
                if (v > this.moduleConfig.getParam("adapter", "error")) return true;
            }
        }
        return false;
    }

    @Override
    public boolean raisesWarning() {
        if (longestAdapter > longestSequence) return true;
        if (!calculated) calculateEnrichment();
        if (enrichments == null) return false;
        for (double[] enrichment : enrichments) {
            for (double v : enrichment) {
                if (v > this.moduleConfig.getParam("adapter", "warn")) return true;
            }
        }
        return false;
    }

    @Override
    public boolean ignoreInReport() {
        return false;
    }

    public List<AdapterDataDTO> getAdapterContentData() {
        if (!calculated) calculateEnrichment();
        List<AdapterDataDTO> dataList = new ArrayList<>();
        
        for (int a = 0; a < adapters.length; a++) {
            List<PointDTO> points = new ArrayList<>(); 
            for (int g = 0; g < groups.length; g++) {
                points.add(new PointDTO(xLabels[g], enrichments[a][g]));
            }
            dataList.add(new AdapterDataDTO(labels[a], points));
        }
        return dataList;
    }

    @Override
    public boolean ignoreFilteredSequences() { return true; }

    private class Adapter {
        private String name;
        private String sequence;
        private long[] positions = new long[0];

        public Adapter(String name, String sequence) {
            this.name = name;
            this.sequence = sequence;
            positions = new long[1];
        }

        public void incrementCount(int position) { 
            if (position < positions.length) ++positions[position]; 
        }

        public void expandLengthTo(int newLength) {
            long[] newPositions = new long[newLength];
            System.arraycopy(positions, 0, newPositions, 0, positions.length);
            if (positions.length > 0) {
                for (int i = positions.length; i < newPositions.length; i++) {
                    newPositions[i] = positions[positions.length - 1];
                }
            }
            positions = newPositions;
        }

        public long[] getPositions() { return positions; }
        public String sequence() { return sequence; }
        public void reset() { positions = new long[1]; }
        public String name() { return name; }
    }
}