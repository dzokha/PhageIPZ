package vn.ipz.phageipz.io.writer;

import vn.ipz.phageipz.domain.sequence.Sequence;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPOutputStream;

public class FastQWriter implements AutoCloseable {

    private final BufferedWriter writer;

    public FastQWriter(File outputFile, boolean compress) throws Exception {
        if (compress) {
            // Bao bọc luồng ghi bằng GZIPOutputStream để nén file siêu tốc
            this.writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outputFile))));
        } else {
            this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile)));
        }
    }

    public void write(Sequence seq) throws Exception {
        // Đảm bảo ID luôn bắt đầu bằng ký tự '@' theo chuẩn định dạng FASTQ
        String id = seq.getId();
        if (!id.startsWith("@")) {
            id = "@" + id;
        }
        
        writer.write(id);
        writer.newLine();
        writer.write(seq.getSequence());
        writer.newLine();
        writer.write("+"); // Dòng thứ 3 của FastQ luôn là dấu +
        writer.newLine();
        writer.write(seq.getQualityString());
        writer.newLine();
    }

    @Override
    public void close() throws Exception {
        if (writer != null) {
            writer.close(); // Đóng luồng ghi để giải phóng RAM và hoàn tất nén file
        }
    }
}