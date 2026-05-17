import React, { useState } from 'react';
import axios from 'axios';
import { FaRocket, FaPlay, FaCloudUploadAlt } from 'react-icons/fa';

export default function AutoPilot() {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState('');

  const handleUpload = async () => {
    if (files.length === 0) return alert("Vui lòng chọn file FASTQ thô để bắt đầu Auto-Pilot!");
    setLoading(true);
    setStatus('Đang khởi động hệ thống...');
    
    // Gửi lần lượt từng file như logic gốc
    let successCount = 0;
    for (let i = 0; i < files.length; i++) {
      setStatus(`Đang xử lý file ${i + 1}/${files.length}: ${files[i].name}...`);
      const formData = new FormData();
      formData.append("file", files[i]);
      formData.append("qualityCutoff", 20);
      formData.append("minLength", 35);
      formData.append("autoDetectAdapter", true);

      try {
        await axios.post('/api/v1/phageipz/run-pipeline', formData);
        successCount++;
      } catch (error) {
        console.error("Lỗi pipeline:", error);
      }
    }
    
    setLoading(false);
    if (successCount > 0) {
      setStatus(`Hoàn tất! Đã xử lý tự động thành công ${successCount} file.`);
    } else {
      setStatus('Thất bại trong quá trình xử lý. Vui lòng kiểm tra log hệ thống.');
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-fade-in pb-10">
      <div className="bg-gradient-to-r from-amber-500 to-orange-600 p-8 rounded-2xl text-white shadow-lg">
        <h2 className="text-2xl font-bold mb-2">PhageIPZ Auto-Pilot</h2>
        <p className="opacity-90">Hệ thống sẽ tự động thực hiện: QC ban đầu ➔ Trimming rác ➔ QC kiểm tra lại lần lượt cho tất cả các file đã chọn.</p>
      </div>

      <div className="bg-white p-8 rounded-2xl shadow-sm border border-slate-200 text-center">
        <input type="file" id="autoFiles" multiple accept=".fastq,.fq,.gz" className="hidden" onChange={(e) => setFiles(Array.from(e.target.files))} />
        
        <label htmlFor="autoFiles" className="cursor-pointer block border-2 border-dashed border-slate-200 rounded-xl mb-6 py-10 hover:border-amber-400 transition">
          <FaRocket className="text-5xl text-amber-500 mb-4 mx-auto" />
          {files.length === 0 ? (
             <p className="text-slate-600 font-medium">Click để chọn file FASTQ cần chạy tự động</p>
          ) : (
             <p className="text-emerald-600 font-bold">Đã chọn {files.length} file sẵn sàng xử lý</p>
          )}
        </label>

        <button 
          onClick={handleUpload} 
          disabled={loading}
          className={`px-10 py-4 font-bold rounded-2xl transition shadow-xl mx-auto flex items-center gap-3 ${loading ? 'bg-slate-300 text-slate-500 cursor-not-allowed' : 'bg-amber-600 text-white hover:bg-amber-700 shadow-amber-100'}`}
        >
          {loading ? <span className="w-5 h-5 border-2 border-slate-500 border-t-transparent rounded-full animate-spin"></span> : <FaPlay />}
          KÍCH HOẠT QUY TRÌNH ĐỒNG BỘ
        </button>
        
        {status && (
          <div className={`mt-6 font-medium ${status.includes('lỗi') || status.includes('Thất bại') ? 'text-red-500' : 'text-amber-700'}`}>
            {status}
          </div>
        )}
      </div>
    </div>
  );
}