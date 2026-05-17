package vn.ipz.phageipz.io.parser;

import java.io.IOException;
import java.io.InputStream; 
import java.util.List;
import vn.ipz.phageipz.domain.sequence.CasavaBasename;
import vn.ipz.phageipz.exception.NameFormatException;

import vn.ipz.phageipz.domain.sequence.Sequence;


public class SequenceFileGroup implements SequenceFile {

    private final List<InputStreamSource> sources; // Danh sách nguồn dữ liệu (Stream + Name)
    private final SequenceFactory factory;
    private SequenceFile currentSequenceFile;
    private String groupName;
    private int currentIndex = 0;

    /**
     * Lớp phụ trợ để giữ luồng và tên file
     */
    public static record InputStreamSource(java.io.InputStream stream, String fileName) {}

    public SequenceFileGroup(List<InputStreamSource> sources, SequenceFactory factory) throws IOException, SequenceFormatException {
        this.sources = sources;
        this.factory = factory;
        
        // Khởi tạo file đầu tiên trong nhóm
        this.currentSequenceFile = factory.getSequenceFile(
            sources.get(0).fileName(), 
            sources.get(0).stream()
        );

        // Xác định tên nhóm (Casava)
        try {
            this.groupName = CasavaBasename.getCasavaBasename(currentSequenceFile.name());
        } catch (NameFormatException nfe) {
            this.groupName = currentSequenceFile.name();
        }
    }

    @Override
    public String name() {
        return groupName;
    }

    @Override
    public int getPercentComplete() {
        if (sources.isEmpty()) return 100;
        // Tính % dựa trên tổng số file và % của file hiện tại
        return ((100 * currentIndex) / sources.size())
                + (currentSequenceFile.getPercentComplete() / sources.size());
    }

    @Override
    public boolean hasNext() {
        if (currentSequenceFile.hasNext()) {
            return true;
        } else {
            // Nếu file hiện tại hết, đóng nó lại và chuyển sang file kế tiếp
            while (currentIndex < sources.size() - 1) {
                try {
                    currentSequenceFile.close(); // Đóng stream cũ để tránh leak
                    currentIndex++;
                    
                    InputStreamSource nextSource = sources.get(currentIndex);
                    currentSequenceFile = factory.getSequenceFile(nextSource.fileName(), nextSource.stream());
                    
                    if (currentSequenceFile.hasNext()) return true;
                } catch (Exception e) {
                    // Log error thay vì printStackTrace
                    return false;
                }
            }
            return false;
        }
    }

    @Override
    public Sequence next() throws SequenceFormatException {
        return currentSequenceFile.next();
    }

    @Override
    public boolean isColorspace() {
        return currentSequenceFile.isColorspace();
    }

    @Override
    public void close() throws IOException {
        if (currentSequenceFile != null) {
            currentSequenceFile.close();
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        // Trả về luồng của file hiện tại đang xử lý trong nhóm
        return currentSequenceFile.getInputStream();
    }

    @Override
    public String getId() {
        // Trả về tên định danh của nhóm
        return groupName;
    }
}