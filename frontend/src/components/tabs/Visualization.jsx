import React, { useState, useRef, useEffect } from 'react';
import axios from 'axios';
import * as d3 from 'd3';
import { FaEye, FaPlay, FaCircleNodes } from 'react-icons/fa6';

export default function Visualization() {
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState('');
  const [gbkData, setGbkData] = useState(null);
  const [viewMode, setViewMode] = useState('linear');
  const d3Container = useRef(null);

  const handleUpload = async () => {
    if (!file) return alert("Vui lòng tải lên file GenBank (.gbk)!");
    setLoading(true);
    setStatus("Đang phân tích cấu trúc file GBK...");
    setGbkData(null); // Reset

    const formData = new FormData();
    formData.append("file", file);

    try {
      const response = await axios.post('/api/v1/phageipz/visualize', formData);
      const parsedData = parseGBK(response.data);
      setGbkData(parsedData);
      setStatus("Trực quan hóa thành công!");
    } catch (error) {
      setStatus(`Lỗi hệ thống: ${error.message}`);
    } finally {
      setLoading(false);
    }
  };

  // Logic bóc tách dữ liệu GBK (Giữ nguyên thuật toán tuyệt vời của bạn)
  const parseGBK = (text) => {
    const features = [];
    let genomeLength = 0; let sequence = "";
    const locusMatch = text.match(/LOCUS\s+\S+\s+(\d+)\s+bp/);
    if (locusMatch) genomeLength = parseInt(locusMatch[1]);

    let inOrigin = false;
    const lines = text.split('\n');
    for (let line of lines) {
      if (line.startsWith("ORIGIN")) { inOrigin = true; continue; }
      if (inOrigin) {
        if (line.startsWith("//")) break;
        sequence += line.replace(/[\d\s]/g, "").toUpperCase();
      }
    }
    if(sequence.length > 0 && genomeLength === 0) genomeLength = sequence.length;

    const cdsBlocks = text.split("     CDS             ").slice(1);
    cdsBlocks.forEach(block => {
      const locMatch = block.match(/^(complement\()?(\d+)\.\.(\d+)\)?/);
      if (!locMatch) return;
      const isComplement = !!locMatch[1];
      const start = parseInt(locMatch[2]);
      const end = parseInt(locMatch[3]);
      const blockClean = block.replace(/\n\s+/g, " ");
      
      const funcMatch = blockClean.match(/\/function="([^"]+)"/);
      const func = funcMatch ? funcMatch[1] : "unknown function";
      const prodMatch = blockClean.match(/\/product="([^"]+)"/);
      const product = prodMatch ? prodMatch[1] : "hypothetical protein";
      
      let color = "#8A2BE2"; let category = "Hypothetical Protein";
      const fLower = func.toLowerCase() + " " + product.toLowerCase();
      
      if (fLower.includes("lysis") || fLower.includes("holin") || fLower.includes("lysozyme")) { color = "#FFFF00"; category = "Phage Lysis"; } 
      else if (fLower.includes("tail") || fLower.includes("fiber") || fLower.includes("spike")) { color = "#32CD32"; category = "Phage Tail"; } 
      else if (fLower.includes("head") || fLower.includes("capsid") || fLower.includes("terminase")) { color = "#FF1493"; category = "Capsid & Packing"; } 
      else if (fLower.includes("dna") || fLower.includes("rna") || fLower.includes("polymerase")) { color = "#00FFFF"; category = "DNA/RNA Metabolism"; }

      features.push({ start, end, isComplement, function: func, product, color, category });
    });
    return { length: genomeLength, features, sequence };
  };

  // D3.js Effect: Vẽ lại biểu đồ khi gbkData hoặc viewMode thay đổi
  useEffect(() => {
    if (!gbkData || !d3Container.current) return;
    const container = d3.select(d3Container.current);
    container.selectAll("*").remove(); // Xóa khung tranh cũ

    const width = d3Container.current.clientWidth || 1000;
    const height = d3Container.current.clientHeight || 700;
    const svg = container.append("svg").attr("width", width).attr("height", height);

    if (viewMode === 'linear') {
      // BẢN ĐỒ TUYẾN TÍNH
      const margin = { top: 50, right: 50, bottom: 50, left: 50 };
      const yCenter = height / 2;
      const xScale = d3.scaleLinear().domain([1, gbkData.length]).range([margin.left, width - margin.right]);
      const gMain = svg.append("g");

      // Xương sống DNA
      gMain.append("line").attr("x1", xScale(1)).attr("x2", xScale(gbkData.length)).attr("y1", yCenter).attr("y2", yCenter).attr("stroke", "#333").attr("stroke-width", 3);

      // Vẽ Genes (Đơn giản hóa để hiển thị)
      gbkData.features.forEach(d => {
        const x1 = xScale(d.start);
        const x2 = xScale(d.end);
        gMain.append("rect")
          .attr("x", x1).attr("y", yCenter - 10).attr("width", Math.max(x2 - x1, 2)).attr("height", 20)
          .attr("fill", d.color).attr("stroke", "#111").append("title").text(`${d.product} (${d.start}-${d.end})`);
      });

      // Zoom
      svg.call(d3.zoom().scaleExtent([1, 50]).on("zoom", (e) => {
        const newXScale = e.transform.rescaleX(xScale);
        gMain.selectAll("rect").attr("x", d => newXScale(d.start)).attr("width", d => Math.max(newXScale(d.end) - newXScale(d.start), 2));
        gMain.select("line").attr("x1", newXScale(1)).attr("x2", newXScale(gbkData.length));
      }));

    } else {
      // BẢN ĐỒ VÒNG (Circular)
      const cx = width / 2; const cy = height / 2; const R = 250;
      const gMain = svg.append("g").attr("transform", `translate(${cx}, ${cy}) rotate(-90)`);
      const aScale = d3.scaleLinear().domain([1, gbkData.length]).range([0, 2 * Math.PI]);
      
      gMain.append("circle").attr("r", R).attr("fill", "none").attr("stroke", "#666").attr("stroke-width", 2);

      gbkData.features.forEach(d => {
        const a1 = aScale(d.start); const a2 = aScale(d.end);
        const arc = d3.arc().innerRadius(R - 15).outerRadius(R + 15).startAngle(a1).endAngle(a2);
        gMain.append("path").attr("d", arc).attr("fill", d.color).attr("stroke", "#333").attr("stroke-width", 0.5)
             .append("title").text(d.product);
      });
      svg.call(d3.zoom().scaleExtent([0.5, 5]).on("zoom", e => gMain.attr("transform", `translate(${e.transform.x + cx*e.transform.k}, ${e.transform.y + cy*e.transform.k}) scale(${e.transform.k}) rotate(-90)`)));
    }
  }, [gbkData, viewMode]);

  return (
    <div className="max-w-6xl mx-auto space-y-6 animate-fade-in pb-10">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-slate-800">Bước 6: Trực quan hóa Hệ Gen</h2>
        <p className="text-slate-500">Tự động phân tích tệp GenBank (.gbk) và vẽ bản đồ bằng thư viện D3.js.</p>
      </div>

      <div className="bg-white p-8 rounded-2xl shadow-sm border border-slate-200 text-center transition-all hover:border-violet-400">
        <input type="file" id="fileInputVisualize" accept=".gbk,.gb" className="hidden" onChange={(e) => setFile(e.target.files[0])} />
        
        <label htmlFor="fileInputVisualize" className="cursor-pointer block">
          <div className="w-20 h-20 mx-auto bg-violet-50 text-violet-500 rounded-full flex items-center justify-center mb-4">
            <FaEye className="text-3xl" />
          </div>
          <h3 className="text-lg font-semibold text-slate-800 mb-1">Kéo thả file GenBank (.gbk) vào đây</h3>
          {file && <span className="inline-block mt-3 bg-violet-100 text-violet-700 px-3 py-1 rounded-full text-xs font-medium border border-violet-200">{file.name}</span>}
        </label>

        <button 
          onClick={handleUpload} 
          disabled={loading}
          className={`mt-6 px-8 py-3 font-semibold rounded-xl transition shadow-lg mx-auto flex items-center ${loading ? 'bg-slate-200 text-slate-500' : 'bg-violet-600 text-white hover:bg-violet-700'}`}
        >
           {loading ? <span className="w-5 h-5 border-2 border-slate-400 border-t-transparent rounded-full animate-spin mr-2"></span> : <FaPlay className="mr-2" />}
           Hiển thị Bản đồ Gene
        </button>
        {status && <div className="mt-4 font-semibold text-violet-600">{status}</div>}
      </div>

      {gbkData && (
        <div className="flex justify-center gap-4 mb-4 bg-slate-200 p-1 rounded-xl w-max mx-auto shadow-inner">
           <button onClick={() => setViewMode('linear')} className={`px-6 py-2 rounded-lg font-bold transition ${viewMode === 'linear' ? 'bg-white shadow-sm text-violet-600' : 'text-slate-500'}`}>Tuyến Tính</button>
           <button onClick={() => setViewMode('circular')} className={`px-6 py-2 rounded-lg font-bold transition ${viewMode === 'circular' ? 'bg-white shadow-sm text-violet-600' : 'text-slate-500'}`}>Vòng (Circular)</button>
        </div>
      )}

      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 relative overflow-hidden" style={{ height: '750px' }}>
        {!gbkData && (
          <div className="absolute inset-0 flex flex-col items-center justify-center text-slate-400 bg-white z-10">
            <FaCircleNodes className="text-5xl mb-3 opacity-50" />
            <p className="font-medium text-lg">Bản đồ gene sẽ hiển thị tại đây</p>
          </div>
        )}
        {/* Nơi D3 nhúng bản đồ vào */}
        <div ref={d3Container} className="w-full h-full relative z-0 p-4 cursor-grab"></div>
      </div>
    </div>
  );
}