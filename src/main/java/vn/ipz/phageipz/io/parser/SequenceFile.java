package vn.ipz.phageipz.io.parser;

import java.io.InputStream;
import java.io.IOException;

import vn.ipz.phageipz.domain.sequence.Sequence;

/**
 * Interface định nghĩa hợp đồng (contract) để xử lý các file trình tự sinh học.
 * Hỗ trợ tự động đóng tài nguyên thông qua AutoCloseable.
 */
public interface SequenceFile extends AutoCloseable {

    /**
     * Kiểm tra xem còn trình tự (sequence) nào để đọc tiếp không.
     */
    boolean hasNext();

    /**
     * Lấy trình tự tiếp theo trong file.
     * @throws SequenceFormatException nếu định dạng file bị lỗi hoặc không hợp lệ.
     */
    Sequence next() throws SequenceFormatException;

    /**
     * Lấy tên của file đang xử lý.
     */
    String name();

    /**
     * Lấy định danh (ID) duy nhất của file hoặc luồng dữ liệu.
     */
    String getId();

    /**
     * Kiểm tra xem file trình tự này có sử dụng không gian màu (Colorspace) hay không.
     */
    boolean isColorspace();

    /**
     * Trả về phần trăm tiến độ đọc file.
     * @return Giá trị từ 0 đến 100.
     */
    int getPercentComplete();

    /**
     * Lấy luồng dữ liệu thô (raw data stream) của file.
     */
    InputStream getInputStream() throws IOException;

    @Override
    void close() throws IOException;
}