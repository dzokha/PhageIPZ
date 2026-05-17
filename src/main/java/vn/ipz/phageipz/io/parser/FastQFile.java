package vn.ipz.phageipz.io.parser;

import java.io.*;
import java.util.regex.Pattern;
// ĐÃ THAY THẾ: Sử dụng thư viện chuẩn công nghiệp của Apache thay cho thư viện cũ
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import vn.ipz.phageipz.config.PhageIPZProperties;

import vn.ipz.phageipz.domain.sequence.Sequence;

public class FastQFile implements SequenceFile {

    private static final Pattern COLORSPACE_PATTERN = Pattern.compile("^[GATCNgatcn][\\.0123456]+$");

    private final PhageIPZProperties properties;
    private Sequence nextSequence = null;
    private final String name;
    private final File representativeFile;
    private final InputStream inputStream;
    private final BufferedReader br;
    
    private long lineNumber = 0;
    private boolean isColorspace = false;
    private final boolean casavaMode;
    private final boolean nofilter;

    public FastQFile(PhageIPZProperties properties, InputStream inputStream, String fileName) throws IOException, SequenceFormatException {
        this.properties = properties;
        this.name = fileName;
        this.representativeFile = new File(fileName);
        this.inputStream = inputStream;

        boolean casava = false;
        boolean filter = false;
        if (properties != null && properties.getAnalysis() != null) {
            casava = properties.getAnalysis().isCasava();
            filter = properties.getAnalysis().isNofilter();
        }
        this.casavaMode = casava;
        this.nofilter = filter;

        InputStream wrappedStream = inputStream;
        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".gz")) {
            wrappedStream = new MultiMemberGZIPInputStream(inputStream);
        } else if (lowerName.endsWith(".bz2")) {
            // ĐÃ THAY THẾ: Khởi tạo luồng giải nén bằng Apache Compress
            wrappedStream = new BZip2CompressorInputStream(inputStream);
        }

        this.br = new BufferedReader(new InputStreamReader(wrappedStream));
        readNext(); 
    }

    private void readNext() throws SequenceFormatException {
        try {
            String id;
            while (true) {
                id = br.readLine();
                if (id == null) {
                    nextSequence = null;
                    close();
                    return;
                }
                lineNumber++;
                if (!id.trim().isEmpty()) break;
            }

            if (!id.startsWith("@")) {
                throw new SequenceFormatException("Lỗi định dạng dòng ID (thiếu @) tại dòng: " + lineNumber);
            }

            String seq = br.readLine(); lineNumber++;
            String midLine = br.readLine(); lineNumber++;
            String quality = br.readLine(); lineNumber++;

            if (seq == null || midLine == null || quality == null) {
                throw new IOException("File FASTQ bị cắt ngang tại dòng " + lineNumber);
            }

            if (!midLine.startsWith("+")) {
                throw new SequenceFormatException("Thiếu dấu '+' tại dòng " + lineNumber);
            }

            if (lineNumber <= 4) {
                isColorspace = COLORSPACE_PATTERN.matcher(seq).matches();
            }

            String finalSeq = isColorspace ? convertColorspaceToBases(seq) : seq;
            nextSequence = new Sequence(id, finalSeq, quality, representativeFile, isColorspace);

            if (casavaMode && !nofilter && id.contains(":Y:")) {
                nextSequence.setFiltered(true);
            }

        } catch (IOException ioe) {
            throw new SequenceFormatException("Lỗi đọc file: " + ioe.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            if (br != null) br.close();
        } catch (IOException e) {
            // Log quietly
        }
    }

    private String convertColorspaceToBases(String s) { return s; }

    @Override public String name() { return name; }
    @Override public boolean hasNext() { return nextSequence != null; }
    @Override public boolean isColorspace() { return isColorspace; }
    @Override public int getPercentComplete() { return hasNext() ? 0 : 100; }
    @Override public String getId() { return name; }
    @Override public InputStream getInputStream() { return inputStream; }

    @Override
    public Sequence next() throws SequenceFormatException {
        Sequence seq = nextSequence;
        readNext();
        return seq;
    }
}