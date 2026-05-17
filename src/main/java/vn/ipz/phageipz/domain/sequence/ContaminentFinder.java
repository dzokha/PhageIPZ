package vn.ipz.phageipz.domain.sequence;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import vn.ipz.phageipz.config.PhageIPZProperties;

@Component
public class ContaminentFinder {

    private final PhageIPZProperties properties;
    private final ResourceLoader resourceLoader;

    private static Contaminant[] contaminants;

    public ContaminentFinder(PhageIPZProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }
    
    /**
     * Tìm kiếm trình tự tạp nhiễm tốt nhất cho một chuỗi đầu vào.
     */
    public ContaminantHit findContaminantHit(String sequence) {
        if (contaminants == null) {
            contaminants = makeContaminantList();
        }

        ContaminantHit bestHit = null;

        for (int c = 0; c < contaminants.length; c++) {
            ContaminantHit thisHit = contaminants[c].findMatch(sequence);

            if (thisHit == null) continue;

            if (bestHit == null || thisHit.length() > bestHit.length() 
                || (thisHit.length() == bestHit.length() && thisHit.percentID() > bestHit.percentID())) {
                bestHit = thisHit;
            }
        }

        return bestHit;
    }

    /**
     * Khởi tạo danh sách các trình tự tạp nhiễm từ file cấu hình.
     * ÁP DỤNG LOGIC "GOM NHÓM THÔNG MINH" (Smart Grouping).
     */
    private Contaminant[] makeContaminantList() {
        Vector<Contaminant> c = new Vector<Contaminant>();

        try {
            BufferedReader br = null;
            String configPath = properties.getAnalysis().getContaminantFile();

            if (configPath == null || configPath.isEmpty()) {
                InputStream rsrc = ContaminentFinder.class.getResourceAsStream("/data/contaminant_list.txt");
                if (rsrc == null) throw new FileNotFoundException("Cannot find default /data/contaminant_list.txt");
                br = new BufferedReader(new InputStreamReader(rsrc));
            } else {
                Resource resource = resourceLoader.getResource(configPath);
                br = new BufferedReader(new InputStreamReader(resource.getInputStream()));
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) continue; 
                if (line.trim().length() == 0) continue; 

                String[] sections = line.split("\\t+");
                if (sections.length != 2) {
                    System.err.println("Expected 2 sections for contaminant line but got " + sections.length + " from " + line);
                    continue;
                }
                
                // --- ÁP DỤNG LOGIC GOM NHÓM ---
                // Thay vì đưa tên nguyên bản (vd: TruSeq Adapter Index 1), 
                // ta sẽ đưa tên đã được gom nhóm để hiển thị đẹp hơn trên Web.
                String rawName = sections[0].trim();
                String sequence = sections[1].trim();
                String groupedName = getGroupName(rawName);

                // Khởi tạo Contaminant với tên đã được gom nhóm
                Contaminant con = new Contaminant(groupedName, sequence);
                c.add(con);
            }

            br.close();
        } catch (IOException e) {
            System.err.println("Error loading contaminants: " + e.getMessage());
        }

        return c.toArray(new Contaminant[0]);
    }

    /**
     * Hàm Tối thiểu hóa Đầu ra (Output Minimization): 
     * Gom nhóm các adapter có cùng nguồn gốc để bảng kết quả không bị rối (mạng nhện).
     */
    private String getGroupName(String rawName) {
        String name = rawName.toUpperCase();
        
        if (name.contains("TRUSEQ ADAPTER INDEX") || name.contains("TRUSEQ UNIVERSAL ADAPTER")) {
            return "Illumina TruSeq Adapter (All Indices)";
        } 
        else if (name.contains("NEXTERA PCR PRIMER") || name.contains("NEXTERA TRANSPOSASE")) {
            return "Illumina Nextera Protocol";
        } 
        else if (name.contains("SMALL RNA ADAPTER") || name.contains("RNA ADAPTER")) {
            return "Illumina Small RNA Protocol";
        } 
        else if (name.contains("ABI SOLID")) {
            return "ABI Solid Adapter";
        } 
        else if (name.contains("POLYA") || name.contains("POLYG")) {
            return "Artifact: Poly-A/G Tail";
        }
        
        // Nếu không thuộc nhóm nào, trả về tên gốc
        return rawName;
    }
}