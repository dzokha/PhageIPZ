import React, { useState } from 'react';
import axios from 'axios';

// Hãy sửa lại thành:
import { FaCloudUploadAlt, FaChartLine, FaRobot } from "react-icons/fa";
import { FaListCheck } from "react-icons/fa6"; // Tách riêng FaListCheck lấy từ bộ FontAwesome 6 (fa6)

// Import toàn bộ các Component biểu đồ đã viết ở bước trước
import { 
    AdapterChart, 
    PerBaseQualityChart, 
    PerSequenceQualityChart,
    PerBaseSequenceContentChart,
    PerSequenceGCContentChart,
    PerBaseNContentChart,
    SequenceLengthDistributionChart,
    DuplicationLevelsChart,
    OverrepresentedTable,
    KmerChart
} from '../charts/QcCharts';

export default function RawDataQC() {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState('');
  const [qcResults, setQcResults] = useState(null);
  
  // State quản lý trạng thái loading và kết quả của AI cho từng report
  const [aiLoading, setAiLoading] = useState({});
  const [aiResponses, setAiResponses] = useState({});

  // Xử lý khi chọn file
  const handleFileChange = (e) => {
    setFiles(Array.from(e.target.files));
  };

  // Hàm gọi API phân tích QC
  const handleUpload = async () => {
    if (files.length === 0) return alert("Vui lòng chọn ít nhất một file!");
    
    setLoading(true);
    setStatus("Đang phân tích dữ liệu...");
    setQcResults(null); // Reset kết quả cũ
    
    const formData = new FormData();
    files.forEach(file => formData.append("files", file));
    
    // Gửi kèm cấu hình Limits (Có thể lấy từ State nếu bạn làm form Settings)
    const limits = { adapterWarn: 5, adapterError: 10, nWarn: 5, nError: 20, dupWarn: 70, dupError: 50 };
    Object.keys(limits).forEach(key => formData.append(`limits_${key}`, limits[key]));

    try {
      const response = await axios.post('/api/v1/phageipz/analyze', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      
      setStatus(`Phân tích thành công ${response.data.length} file.`);
      setQcResults(response.data);
    } catch (error) {
      setStatus(`Lỗi: ${error.response?.data?.message || error.message}`);
    } finally {
      setLoading(false);
    }
  };

  // Hàm gọi API Trợ lý AI Đánh giá
  const handleExplainAI = async (report) => {
    const sectionId = report.fileName;
    setAiLoading(prev => ({ ...prev, [sectionId]: true }));
    
    try {
      const response = await axios.post('/api/v1/phageipz/explain-qc', {
        modelChoice: 'ollama', // Hoặc lấy từ State Context chung
        qcData: report.basicStats
      });
      setAiResponses(prev => ({ ...prev, [sectionId]: response.data.explanation }));
    } catch (error) {
      setAiResponses(prev => ({ ...prev, [sectionId]: `<div class="text-red-500">Lỗi gọi AI: ${error.message}</div>` }));
    } finally {
      setAiLoading(prev => ({ ...prev, [sectionId]: false }));
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-fade-in pb-10">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-slate-800">Bước 1: Quality Control (QC)</h2>
        <p className="text-slate-500 text-sm mt-1">Phân tích chất lượng dữ liệu giải trình tự thô (Raw Reads).</p>
      </div>
      
      {/* KHU VỰC UPLOAD */}
      <div className="bg-white p-8 rounded-2xl shadow-sm border border-slate-200 text-center hover:border-blue-400 transition">
        <input type="file" id="fileInput" multiple accept=".fastq,.fq,.gz" className="hidden" onChange={handleFileChange} />
        
        <label htmlFor="fileInput" className="cursor-pointer block">
          <div className="w-20 h-20 mx-auto bg-blue-50 text-blue-500 rounded-full flex items-center justify-center mb-4 transition-transform hover:scale-105">
            <FaCloudUploadAlt size={40} />
          </div>
          <h3 className="text-lg font-semibold text-slate-800 mb-1">Kéo thả file FASTQ vào đây</h3>
          <p className="text-xs text-slate-400">Hỗ trợ định dạng: .fastq, .fq, .gz</p>
        </label>
        
        {files.length > 0 && (
          <div className="flex flex-wrap justify-center gap-2 mt-6 p-4 bg-slate-50 rounded-lg border border-slate-100">
            {files.map((f, i) => (
              <span key={i} className="inline-flex items-center bg-white text-blue-700 px-3 py-1.5 rounded-md text-xs font-medium border border-blue-200 shadow-sm">
                <span className="w-2 h-2 rounded-full bg-blue-500 mr-2"></span> {f.name}
              </span>
            ))}
          </div>
        )}

        <div className="flex justify-center items-center gap-4 mt-6">
          <button 
            onClick={handleUpload} 
            disabled={loading} 
            className={`px-8 py-3 font-semibold rounded-xl transition shadow-md flex items-center ${loading ? 'bg-slate-100 text-slate-400 cursor-not-allowed border border-slate-200' : 'bg-blue-600 hover:bg-blue-700 text-white'}`}
          >
            {loading ? (
              <><span className="w-5 h-5 border-2 border-slate-400 border-t-transparent rounded-full animate-spin mr-2"></span> Đang xử lý...</>
            ) : (
              <><FaChartLine className="mr-2" /> Bắt đầu phân tích (QC)</>
            )}
          </button>
        </div>

        {status && (
          <div className={`mt-4 font-semibold text-sm ${status.includes('Lỗi') ? 'text-red-500' : 'text-emerald-600'}`}>
            {status}
          </div>
        )}
      </div>

      {/* KHU VỰC HIỂN THỊ KẾT QUẢ TỪNG FILE */}
      {qcResults && qcResults.map((report, idx) => (
        <div key={idx} className="bg-white border border-slate-200 rounded-2xl p-6 md:p-8 shadow-sm">
          
          {/* Header Báo cáo & Nút gọi AI */}
          <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-6 border-l-4 border-blue-500 pl-4">
            <h3 className="text-lg font-bold text-slate-800">{report.fileName}</h3>
            
            <button 
              onClick={() => handleExplainAI(report)}
              disabled={aiLoading[report.fileName]}
              className="mt-3 md:mt-0 px-4 py-2 bg-indigo-50 text-indigo-600 hover:bg-indigo-100 border border-indigo-200 text-sm font-bold rounded-lg transition shadow-sm flex items-center"
            >
              {aiLoading[report.fileName] ? (
                <><span className="w-4 h-4 border-2 border-indigo-600 border-t-transparent rounded-full animate-spin mr-2"></span> Đang suy nghĩ...</>
              ) : (
                <><FaRobot className="mr-2" /> AI Đánh giá</>
              )}
            </button>
          </div>

          {/* Khung hiển thị kết quả AI (nếu có) */}
          {aiResponses[report.fileName] && (
            <div className="mb-8 p-5 bg-indigo-50/50 rounded-xl border border-indigo-100 text-sm text-slate-700">
              <div className="font-bold text-indigo-800 mb-2 pb-2 border-b border-indigo-100 flex items-center">
                <FaRobot className="mr-2" /> Phân tích từ Trợ lý AI
              </div>
              <div dangerouslySetInnerHTML={{ __html: aiResponses[report.fileName] }} />
            </div>
          )}

          {/* Bảng Basic Statistics */}
          <div className="mb-10">
            <h4 className="text-sm font-semibold text-slate-500 uppercase mb-3 flex items-center">
              <FaListCheck className="mr-2" /> Basic Statistics
            </h4>
            <div className="overflow-hidden border border-slate-200 rounded-lg shadow-sm">
              <table className="w-full text-left text-sm text-slate-700">
                <thead className="bg-[#000080] text-white font-semibold">
                  <tr>
                    <th className="px-4 py-3 w-1/2 border-r border-blue-900/50">Measure</th>
                    <th className="px-4 py-3 w-1/2">Value</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  <tr className="bg-slate-50">
                    <td className="px-4 py-2.5 font-medium">Filename</td>
                    <td className="px-4 py-2.5 font-mono text-xs">{report.basicStats?.filename || 'N/A'}</td>
                  </tr>
                  <tr className="bg-white">
                    <td className="px-4 py-2.5 font-medium">Total Sequences</td>
                    <td className="px-4 py-2.5 font-mono text-xs">{report.basicStats?.totalSequences || 0}</td>
                  </tr>
                  <tr className="bg-slate-50">
                    <td className="px-4 py-2.5 font-medium">Sequence length</td>
                    <td className="px-4 py-2.5 font-mono text-xs">{report.basicStats?.sequenceLength || 'N/A'}</td>
                  </tr>
                  <tr className="bg-white">
                    <td className="px-4 py-2.5 font-medium">%GC</td>
                    <td className="px-4 py-2.5 font-mono text-xs">{report.basicStats ? Math.round(report.basicStats.gcContent) : 0}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          {/* CÁC BIỂU ĐỒ D3.JS ĐƯỢC RENDER TẠI ĐÂY */}
          <div className="grid grid-cols-1 gap-10">
            <div>
              <h4 className="text-sm font-semibold text-slate-500 uppercase mb-2">1. Per Base Sequence Quality</h4>
              <PerBaseQualityChart data={report.perBaseQualityData} />
            </div>

            <div>
              <h4 className="text-sm font-semibold text-slate-500 uppercase mb-2">2. Per Sequence Quality Scores</h4>
              <PerSequenceQualityChart data={report.perSeqQualityData} />
            </div>

            <div>
              <h4 className="text-sm font-semibold text-slate-500 uppercase mb-2">3. Per Base Sequence Content</h4>
              <PerBaseSequenceContentChart data={report.perBaseContentData} />
            </div>

            <div>
              <h4 className="text-sm font-semibold text-slate-500 uppercase mb-2">4. Per Sequence GC Content</h4>
              <PerSequenceGCContentChart data={report.gcContentData} />
            </div>

            <div>
              <h4 className="text-sm font-semibold text-slate-500 uppercase mb-2">5. Per Base N Content</h4>
              <PerBaseNContentChart data={report.nContentData} limits={{ nWarn: 5, nError: 20 }} />
            </div>

            <div>
              <h4 className="text-sm font-semibold text-slate-500 uppercase mb-2">6. Sequence Length Distribution</h4>
              <SequenceLengthDistributionChart data={report.lengthDistributionData} />
            </div>

            <div>
              <h4 className="text-sm font-semibold text-slate-500 uppercase mb-2">7. Sequence Duplication Levels</h4>
              <DuplicationLevelsChart data={report.duplicationLevelsData} limits={{ dupWarn: 70, dupError: 50 }} />
            </div>

            <div>
              <h4 className="text-sm font-semibold text-slate-500 uppercase mb-2">8. Overrepresented Sequences</h4>
              <OverrepresentedTable data={report.overrepresentedData} />
            </div>

            <div>
              <h4 className="text-sm font-semibold text-slate-500 uppercase mb-2">9. Adapter Content</h4>
              <AdapterChart data={report.adapterData} limits={{ adapterWarn: 5, adapterError: 10 }} />
            </div>

            <div>
              <h4 className="text-sm font-semibold text-slate-500 uppercase mb-2">10. Kmer Content</h4>
              <KmerChart data={report.kmerData} />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}