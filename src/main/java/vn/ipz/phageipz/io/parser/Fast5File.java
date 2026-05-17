package vn.ipz.phageipz.io.parser;

import java.io.IOException;
import java.io.InputStream;
import vn.ipz.phageipz.domain.sequence.Sequence;

/**
 * Lớp này được vô hiệu hóa để tối ưu hóa hệ thống.
 * Dự án SOAP tập trung xử lý dữ liệu Illumina (.fastq.gz).
 * Các thư viện phân tích định dạng Oxford Nanopore (.fast5) đã bị lược bỏ để giảm thiểu sự cồng kềnh.
 */
public class Fast5File implements SequenceFile {

    private final String name;
    private final InputStream inputStream;

    public Fast5File(InputStream inputStream, String name) throws SequenceFormatException, IOException {
        this.name = name;
        this.inputStream = inputStream;
        
        // Báo lỗi ngay lập tức nếu người dùng cố tình đưa file .fast5 vào hệ thống
        throw new SequenceFormatException("Định dạng Fast5 (.fast5) không được hỗ trợ trong phiên bản SOAP này. Vui lòng sử dụng file FastQ.");
    }

    @Override
    public String name() { return name; }

    @Override
    public int getPercentComplete() { return 100; }

    @Override
    public boolean isColorspace() { return false; }

    @Override
    public boolean hasNext() { return false; }

    @Override
    public Sequence next() throws SequenceFormatException { return null; }

    @Override
    public InputStream getInputStream() throws IOException { return this.inputStream; }

    @Override
    public String getId() { return this.name; }

    @Override
    public void close() {
        try {
            if (inputStream != null) inputStream.close();
        } catch (IOException e) {
            // Log quietly
        }
    }
}