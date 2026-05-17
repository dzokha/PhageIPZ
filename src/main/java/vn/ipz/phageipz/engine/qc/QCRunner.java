package vn.ipz.phageipz.engine.qc;

import org.springframework.stereotype.Component;
import vn.ipz.phageipz.engine.qc.modules.BasicStats;
import vn.ipz.phageipz.engine.qc.core.QCModule;
import vn.ipz.phageipz.domain.sequence.Sequence;
import vn.ipz.phageipz.io.parser.SequenceFile;
import vn.ipz.phageipz.io.parser.SequenceFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

@Component
public class QCRunner {

    private static final Logger log = LoggerFactory.getLogger(QCRunner.class);

    /**
     * Chạy phân tích đồng bộ trên luồng được giao bởi Service.
     */
    public void runAnalysisSync(SequenceFile file, QCModule[] modules) throws SequenceFormatException {

        if (modules == null) return; 

        for (QCModule module : modules) {
            if (module != null) {
                module.reset();
            }
        }

        // Scanner keyboardScanner = new Scanner(System.in);
        int seqCount = 0;
        while (file.hasNext()) {
            seqCount++;
            Sequence seq = file.next();

            // ==========================================
            // 1. IN RA DỮ LIỆU THÔ (RAW DATA)
            // ==========================================
            // System.out.println("\n==================================================");
            // System.out.println("ĐANG XỬ LÝ ĐOẠN DNA THỨ: " + seqCount);
            // System.out.println("[THÔ] Chuỗi DNA  : " + seq.getSequence());
            // System.out.println("[THÔ] Điểm Quality: " + seq.getQualityString());

            for (QCModule module : modules) {
                if (module == null) continue; 

                if (seq.isFiltered() && module.ignoreFilteredSequences()) {
                    continue;
                }
                module.processSequence(seq);
            }

            // ==========================================
            // 2. IN DỮ LIỆU SAU XỬ LÝ & CHỜ BẤM ENTER
            // ==========================================
            // System.out.println("[XỬ LÝ] Đoạn DNA trên đã được cập nhật thành công vào " + modules.length + " trạm thống kê.");
            // System.out.print(" Bấm phím [ENTER] để tiếp tục chạy đoạn tiếp theo (hoặc gõ chữ rồi Enter)... ");
            
            // Lệnh này sẽ "đóng băng" hệ thống cho đến khi bạn gõ Enter ở cửa sổ Console/Terminal
            // keyboardScanner.nextLine(); 
            // ==========================================
            
            if (seqCount % 10000 == 0) {
                // log.info("Đang xử lý... Đã đọc được {} đoạn DNA.", seqCount);
            }
        }

        // Xử lý trường hợp file rỗng (giữ nguyên logic gốc)
        if (seqCount == 0) {
            for (QCModule module : modules) {
                if (module instanceof BasicStats) {
                    ((BasicStats) module).setFileName(file.name());
                }
            }
        }
    }
}