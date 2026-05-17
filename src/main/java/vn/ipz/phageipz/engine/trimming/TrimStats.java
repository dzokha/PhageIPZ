package vn.ipz.phageipz.engine.trimming;

import org.slf4j.Logger;

public class TrimStats {
    public long totalReads = 0;
    public long totalBases = 0;
    public long readsWithAdapter = 0;
    public long qualityTrimmedBases = 0;
    public long adapterTrimmedBases = 0;
    public long readsTooShort = 0;
    public long outputReads = 0;
    public long outputBases = 0;

    public void printCutadaptReport(Logger log) {
        log.info("=== BÁO CÁO CẮT TỈA (SOAP NATIVE TRIMMER) ===");
        log.info(String.format("Total reads processed: %,15d", totalReads));
        log.info(String.format("Total basepairs processed: %,11d bp", totalBases));
        log.info(String.format("Reads with adapters:   %,15d (%.1f%%)", readsWithAdapter, getPercent(readsWithAdapter, totalReads)));
        log.info(String.format("Quality-trimmed bases: %,11d bp (%.1f%%)", qualityTrimmedBases, getPercent(qualityTrimmedBases, totalBases)));
        log.info(String.format("Adapter-trimmed bases: %,11d bp (%.1f%%)", adapterTrimmedBases, getPercent(adapterTrimmedBases, totalBases)));
        log.info(String.format("Reads too short:       %,15d (%.1f%%)", readsTooShort, getPercent(readsTooShort, totalReads)));
        log.info("---------------------------------------------");
        log.info(String.format("Reads written (passing filters): %,5d (%.1f%%)", outputReads, getPercent(outputReads, totalReads)));
        log.info(String.format("Total basepairs written: %,13d bp (%.1f%%)", outputBases, getPercent(outputBases, totalBases)));
        log.info("=============================================");
    }

    private double getPercent(long part, long total) {
        if (total == 0) return 0.0;
        return (double) part / total * 100.0;
    }
}