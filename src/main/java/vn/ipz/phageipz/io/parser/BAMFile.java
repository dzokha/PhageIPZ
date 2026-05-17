/**
 * Copyright Copyright 2010-12 Simon Andrews
 *
 *    This file is part of soap.
 *
 *    SOAP is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    SOAP is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with SOAP; if not, write to the Free Software
 *    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package vn.ipz.phageipz.io.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import htsjdk.samtools.*;


import vn.ipz.phageipz.domain.sequence.Sequence;

public class BAMFile implements SequenceFile {

    private final String name;
    private final boolean onlyMapped;
    private final InputStream inputStream;
    private final SamReader samReader;
    private final Iterator<SAMRecord> iterator;
    
    private Sequence nextSequence = null;
    private long recordsProcessed = 0;

    public BAMFile(InputStream inputStream, String name, boolean onlyMapped) throws SequenceFormatException, IOException {
        this.inputStream = inputStream;
        this.name = name;
        this.onlyMapped = onlyMapped;

        // SamReaderFactory có thể mở trực tiếp từ InputStream
        this.samReader = SamReaderFactory.makeDefault()
                .validationStringency(ValidationStringency.SILENT)
                .open(SamInputResource.of(inputStream));
        
        this.iterator = samReader.iterator();
        readNext();
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Trên Web, việc tính % dựa trên file size của Stream rất khó nếu không biết trước dung lượng.
     * Ta có thể ước tính dựa trên số lượng bản ghi hoặc để Controller quản lý.
     */
    @Override
    public int getPercentComplete() {
        return hasNext() ? 0 : 100; // Đơn giản hóa cho bản Web
    }

    @Override
    public boolean isColorspace() {
        return false;
    }

    @Override
    public boolean hasNext() {
        return nextSequence != null;
    }

    @Override
    public Sequence next() throws SequenceFormatException {
        Sequence returnSeq = nextSequence;
        readNext();
        return returnSeq;
    }

    private void readNext() throws SequenceFormatException {
        SAMRecord record;
        while (true) {
            if (!iterator.hasNext()) {
                nextSequence = null;
                close();
                return;
            }

            try {
                record = iterator.next();
            } catch (SAMFormatException sfe) {
                throw new SequenceFormatException(sfe.getMessage());
            }

            if (onlyMapped && record.getReadUnmappedFlag()) {
                continue;
            }
            break;
        }

        String sequence = record.getReadString();
        String qualities = record.getBaseQualityString();

        // Xử lý Clipping nếu là Mapped data
        if (onlyMapped) {
            List<CigarElement> elements = record.getCigar().getCigarElements();
            if (!elements.isEmpty()) {
                // Clip 3' end
                if (elements.get(elements.size() - 1).getOperator() == CigarOperator.S) {
                    int value = elements.get(elements.size() - 1).getLength();
                    sequence = sequence.substring(0, Math.max(0, sequence.length() - value));
                    qualities = qualities.substring(0, Math.max(0, qualities.length() - value));
                }
                // Clip 5' end
                if (!elements.isEmpty() && elements.get(0).getOperator() == CigarOperator.S) {
                    int value = elements.get(0).getLength();
                    if (sequence.length() > value) {
                        sequence = sequence.substring(value);
                        qualities = qualities.substring(value);
                    }
                }
            }
        }

        // Đưa về hướng gốc nếu là Negative Strand
        if (record.getReadNegativeStrandFlag()) {
            sequence = reverseComplement(sequence);
            qualities = reverse(qualities);
        }

        recordsProcessed++;
        // Lưu ý: Sequence bây giờ không giữ tham chiếu ngược đến BAMFile để tránh lỗi JSON
        nextSequence = new Sequence(record.getReadName(), sequence, qualities);
    }

    @Override
    public void close() {
        try {
            if (samReader != null) samReader.close();
            if (inputStream != null) inputStream.close();
        } catch (IOException e) {
            // Log error
        }
    }

    // Các hàm tiện ích static để xử lý chuỗi
    private String reverseComplement(String sequence) {
        char[] letters = reverse(sequence).toUpperCase().toCharArray();
        for (int i = 0; i < letters.length; i++) {
            switch (letters[i]) {
                case 'G': letters[i] = 'C'; break;
                case 'A': letters[i] = 'T'; break;
                case 'T': letters[i] = 'A'; break;
                case 'C': letters[i] = 'G'; break;
            }
        }
        return new String(letters);
    }

    private String reverse(String sequence) {
        return new StringBuilder(sequence).reverse().toString();
    }

    @Override
	public String getId() {
	    return name;
	}

	@Override
	public InputStream getInputStream() throws IOException {
	    return inputStream;
	}
}
