// package vn.dzokha.soap.engine.qc.modules;

// import com.google.common.hash.BloomFilter;
// import com.google.common.hash.Funnels;
// import java.nio.charset.StandardCharsets;

// public class BloomDeduplicator {
//     // Khởi tạo Bloom Filter dự kiến cho 50 triệu reads, sai số 0.01% (Tốn khoảng ~100MB RAM)
//     private final BloomFilter<String> bloomFilter = BloomFilter.create(
//             Funnels.stringFunnel(StandardCharsets.UTF_8), 
//             50_000_000, 
//             0.0001
//     );

//     /**
//      * @return true nếu CÓ THỂ là trùng lặp, false nếu CHẮC CHẮN chưa từng xuất hiện.
//      */
//     public synchronized boolean isDuplicate(Sequence seq) {
//         // Lấy chuỗi từ 15bp đầu tiên (Seed) hoặc cả chuỗi để Hash
//         String dna = seq.getSequence();
//         String seed = dna.length() > 50 ? dna.substring(0, 50) : dna;
        
//         if (bloomFilter.mightContain(seed)) {
//             return true; // Bị loại (Duplicate)
//         } else {
//             bloomFilter.put(seed);
//             return false; // Lần đầu xuất hiện -> Giữ lại
//         }
//     }
// }