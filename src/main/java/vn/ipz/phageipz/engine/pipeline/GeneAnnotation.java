package vn.ipz.phageipz.engine.pipeline;

public class GeneAnnotation {
    public String id;
    public int start;
    public int end;
    public String strand;
    public String product = "hypothetical protein";
    public String function = "unknown function";
    public String phrog = "No_PHROG";
    public String topHit = "No_PHROG"; // Giống Pharokka
    public String translation = "";
    public String score = "0.0";        // Lấy từ Prodigal
    public String source = "Prodigal_2.6"; // Tên công cụ
    public double evalue = 1.0;
    public String sourceDb = "None";
}