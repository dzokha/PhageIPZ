// package vn.dzokha.soap.engine.pipeline;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import vn.dzokha.soap.domain.sequence.Sequence;
// import vn.dzokha.soap.io.parser.SequenceFile;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.concurrent.ArrayBlockingQueue;
// import java.util.concurrent.BlockingQueue;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.TimeUnit;

// public class FastqPipelineOrchestrator {
//     private static final Logger log = LoggerFactory.getLogger(FastqPipelineOrchestrator.class);
//     private static final int CHUNK_SIZE = 1000; // Đóng gói 1000 reads/khối
//     private static final int QUEUE_CAPACITY = 50; // Tránh OutOfMemory
    
//     // Hàng đợi Lock-free SPSC (Dùng ArrayBlockingQueue cho Java thuần)
//     private final BlockingQueue<List<Sequence>> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
//     private final boolean isPairedEnd;

//     public FastqPipelineOrchestrator(boolean isPairedEnd) {
//         this.isPairedEnd = isPairedEnd;
//     }

//     public void runOnePassPipeline(SequenceFile file, SequenceWriter outputWriter) {
//         int cores = Runtime.getRuntime().availableProcessors();
//         ExecutorService consumers = Executors.newFixedThreadPool(cores);

//         // 1. Khởi động các Consumer Threads
//         for (int i = 0; i < cores; i++) {
//             consumers.submit(() -> {
//                 try {
//                     while (true) {
//                         List<Sequence> chunk = queue.take();
//                         if (chunk.isEmpty()) break; // Tín hiệu kết thúc (Poison Pill)
                        
//                         List<Sequence> cleanChunk = new ArrayList<>();
//                         for (Sequence seq : chunk) {
//                             // CHẠY ONE-PASS CHUẨN: Lọc qua tất cả các module
//                             if (processOnePass(seq)) {
//                                 cleanChunk.add(seq);
//                             }
//                         }
//                         // Đẩy ra Writer (Cần cấu trúc Thread-safe cho Writer)
//                         outputWriter.writeChunk(cleanChunk);
//                     }
//                 } catch (InterruptedException e) {
//                     Thread.currentThread().interrupt();
//                 }
//             });
//         }

//         // 2. Chạy Producer Thread (Đọc File) trên luồng chính
//         try {
//             List<Sequence> currentChunk = new ArrayList<>(CHUNK_SIZE);
//             while (file.hasNext()) {
//                 currentChunk.add(file.next());
//                 if (currentChunk.size() == CHUNK_SIZE) {
//                     queue.put(currentChunk);
//                     currentChunk = new ArrayList<>(CHUNK_SIZE);
//                 }
//             }
//             if (!currentChunk.isEmpty()) {
//                 queue.put(currentChunk);
//             }
            
//             // Gửi "Poison Pill" để dừng Consumer
//             for (int i = 0; i < cores; i++) queue.put(new ArrayList<>());
            
//         } catch (Exception e) {
//             log.error("Lỗi I/O Producer: ", e);
//         } finally {
//             consumers.shutdown();
//             try { consumers.awaitTermination(1, TimeUnit.HOURS); } catch (Exception ignored) {}
//         }
//     }

//     /**
//      * VÒNG LẶP SỰ KIỆN DUY NHẤT (ONE-PASS LOGIC)
//      * Trả về TRUE nếu giữ lại, FALSE nếu vứt bỏ (vì quá ngắn, chất lượng kém, hoặc trùng lặp)
//      */
//     private boolean processOnePass(Sequence seq) {
//         // 1. Trích xuất UMI
//         extractUMI(seq);

//         // 2. Cắt Adapter (Overlap cho PE hoặc Seed-based cho SE)
//         trimAdapter(seq);

//         // 3. Cắt chất lượng cửa sổ trượt (Sliding Window & PolyG)
//         if (!qualityPruning(seq)) return false; // Dài < ngưỡng -> Vứt

//         // 4. Base Correction (Chỉ áp dụng nếu là Paired-end và có overlap)
//         if (isPairedEnd) baseCorrection(seq);

//         // 5. Lọc Trùng lặp (Bloom Filter)
//         if (isDuplicateBloomFilter(seq)) return false; // Trùng -> Vứt

//         // 6. Ghi nhận Metrics tĩnh (JSON/HTML Report)
//         updateQCStats(seq);

//         return true; // Read sạch, giữ lại
//     }

//     // --- Các hàm mockup logic ---
//     private void extractUMI(Sequence seq) { /* Dịch chuỗi từ đầu 5' sang Header */ }
//     private void trimAdapter(Sequence seq) { /* Thuật toán Needleman-Wunsch cắt đuôi 3' */ }
//     private boolean qualityPruning(Sequence seq) { return true; /* Trượt cửa sổ, nếu < 35bp -> return false */ }
//     private void baseCorrection(Sequence seq) { /* Sửa Q15 -> Q30 nếu khớp overlap */ }
//     private void updateQCStats(Sequence seq) { /* Cập nhật vào ModuleFactory / BasicStats */ }
// }