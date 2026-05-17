package vn.ipz.phageipz.io.parser;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import vn.ipz.phageipz.config.PhageIPZProperties;

import java.io.IOException;
import java.io.InputStream;

@Service 
public class SequenceFactory {

    // Khai báo các hằng số định dạng để tránh lỗi gõ sai (Typo) và dễ bảo trì
    private static final String FORMAT_BAM = "bam";
    private static final String FORMAT_SAM = "sam";
    private static final String FORMAT_FASTQ = "fastq";

    private final PhageIPZProperties properties;

    public SequenceFactory(PhageIPZProperties properties) {
        this.properties = properties;
    }

    /**
     * Khởi tạo đối tượng SequenceFile phù hợp.
     * Ưu tiên 1: Lấy theo cấu hình trong file properties (Explicit).
     * Ưu tiên 2: Tự động nhận diện qua đuôi file (Auto-detect).
     */
    public SequenceFile getSequenceFile(String fileName, InputStream inputStream) throws SequenceFormatException, IOException {
        String configuredFormat = properties.getAnalysis().getSequenceFormat();

        // Nếu có cấu hình tường minh, dùng cấu hình đó
        if (StringUtils.hasText(configuredFormat)) {
            return createFromExplicitFormat(configuredFormat, fileName, inputStream);
        }

        // Nếu không có cấu hình, tự động nhận diện
        return createFromExtension(fileName, inputStream);
    }

    /**
     * Khởi tạo dựa trên cấu hình (Sử dụng Switch Expression của Java 14+)
     */
    private SequenceFile createFromExplicitFormat(String format, String fileName, InputStream inputStream) throws SequenceFormatException, IOException {
        return switch (format.toLowerCase()) {
            case FORMAT_BAM, FORMAT_SAM -> new BAMFile(inputStream, fileName, false);
            case FORMAT_FASTQ -> new FastQFile(properties, inputStream, fileName);
            default -> throw new SequenceFormatException("Unsupported explicit format: " + format);
        };
    }

    /**
     * Khởi tạo tự động dựa trên đuôi file
     */
    private SequenceFile createFromExtension(String fileName, InputStream inputStream) throws SequenceFormatException, IOException {
        String lowerName = fileName.toLowerCase();
        
        if (lowerName.endsWith("." + FORMAT_BAM) || lowerName.endsWith("." + FORMAT_SAM)) {
            return new BAMFile(inputStream, fileName, false);
        }
        
        if (lowerName.endsWith(".fast5")) {
            return new Fast5File(inputStream, fileName);
        }

        // Mặc định trả về FastQFile
        return new FastQFile(properties, inputStream, fileName);
    }
}
	