package vn.ipz.phageipz.engine.qc.core;

import vn.ipz.phageipz.domain.sequence.Sequence;

public interface QCModule {

    void processSequence(Sequence sequence);

    // MỚI: Dùng cho fastp/Trimming (Có thể thay đổi chuỗi DNA bên trong object Sequence)
    // Trả về false nếu Module này quyết định vứt bỏ read này
    default boolean processAndFilter(Sequence sequence) {
        processSequence(sequence);
        return true; 
    }

    String name ();
    String description ();
    void reset ();
    boolean raisesError();
    boolean raisesWarning();

    default boolean isCalculated() { return true;  }
    default boolean ignoreFilteredSequences() {return true;}
    default boolean ignoreInReport() { return false;}
    default Object getResultsPanel() {return null; }
}

