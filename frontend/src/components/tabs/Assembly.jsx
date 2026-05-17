import React, { useState } from 'react';
import axios from 'axios';
import { FaLayerGroup, FaMicrochip } from 'react-icons/fa';
import { FaJava } from 'react-icons/fa6';

export default function Assembly() {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState('');

  const handleAssemble = async () => {
    if (files.length === 0) return alert("Vui lòng tải lên file Clean Reads!");
    
    setLoading(true);
    setStatus("Lõi De Bruijn Graph đang chạy... Xin vui lòng chờ.");
    
    const formData = new FormData();
    files.forEach(file => formData.append("files", file));

    try {
      // Dùng responseType 'blob' để tải file
      const response = await axios.post('/api/v1/phageipz/assemble', formData, {
        responseType: 'blob' 
      });
      
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'phageipz_assembled.fasta');
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      
      setStatus("Lắp ráp thành công! File fasta đã được tải xuống.");
    } catch (error) {
      setStatus(`Lỗi hệ thống: Quá trình lắp ráp thất bại.`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-fade-in pb-10">
      <div className="mb-6"><h2 className="text-2xl font-bold text-slate-800">Bước 4: Genome Assembly</h2></div>
      
      <div className="bg-gradient-to-r from-indigo-500 to-blue-600 p-8 rounded-2xl text-white shadow-lg mb-6">
        <h2 className="text-xl font-bold mb-2 flex items-center"><FaJava className="mr-2 text-2xl" /> PhageIPZ Native Assembly Engine</h2>
        <p className="opacity-90 text-sm">Hệ thống đang sử dụng Lõi thuật toán De Bruijn Graph do PhageIPZ tự phát triển bằng Pure Java. Tối ưu hóa đặc biệt cho hệ gen vòng của thực khuẩn thể (Phage).</p>
      </div>

      <div className="bg-white p-8 rounded-2xl shadow-sm border border-slate-200 text-center hover:border-indigo-400 transition">
        <input type="file" id="assemblyFiles" multiple accept=".fastq,.fq,.gz" className="hidden" onChange={(e) => setFiles(Array.from(e.target.files))} />
        
        <label htmlFor="assemblyFiles" className="cursor-pointer block">
          <div className="w-20 h-20 mx-auto bg-indigo-50 text-indigo-500 rounded-full flex items-center justify-center mb-4 transition-transform hover:scale-110">
            <FaLayerGroup className="text-3xl" />
          </div>
          <h3 className="text-lg font-semibold text-slate-800 mb-1">Kéo thả file đã làm sạch (Clean Reads) vào đây</h3>
        </label>
        
        {files.length > 0 && (
           <div className="flex flex-wrap justify-center gap-2 mt-4 text-sm">
             {files.map((f, i) => <span key={i} className="inline-block bg-indigo-100 text-indigo-700 px-3 py-1 rounded-full text-xs font-medium border border-indigo-200 m-1">{f.name}</span>)}
           </div>
        )}
        
        {status && <div className={`mt-6 text-center font-semibold ${status.includes('Lỗi') ? 'text-red-500' : 'text-indigo-600'}`}>{status}</div>}
        
        <button 
          onClick={handleAssemble} 
          disabled={loading}
          className={`mt-6 px-8 py-3 font-semibold rounded-xl transition shadow-lg flex justify-center items-center mx-auto ${loading ? 'bg-slate-200 text-slate-500 cursor-not-allowed' : 'bg-indigo-600 text-white hover:bg-indigo-700'}`}
        >
          {loading ? <span className="w-5 h-5 border-2 border-slate-400 border-t-transparent rounded-full animate-spin mr-2"></span> : <FaMicrochip className="mr-2" />}
          Bắt đầu Lắp ráp bằng PhageIPZ Native
        </button>
      </div>
    </div>
  );
}