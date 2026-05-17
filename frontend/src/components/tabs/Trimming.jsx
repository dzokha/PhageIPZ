import React, { useState } from 'react';
import axios from 'axios';

export default function Trimming() {
  const [quality, setQuality] = useState(20);
  const [minLength, setMinLength] = useState(35);
  const [adapter, setAdapter] = useState(true);
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState('');

  const handleTrim = async () => {
    setLoading(true);
    setStatus("Hệ thống đang thực hiện cắt lọc trimmer... Vui lòng chờ.");
    // Thực hiện logic API gọi /api/v1/phageipz/trim tương tự như cũ tại đây
    setTimeout(() => { setStatus("Tính năng Trimming đã sẵn sàng xử lý."); setLoading(false); }, 1500);
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-fade-in">
      <div className="mb-6"><h2 className="text-2xl font-bold text-slate-800">Bước 2: Trimming (Làm sạch dữ liệu)</h2></div>
      <div className="bg-white p-8 rounded-2xl shadow-sm border border-slate-200">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">Quality Phred Score cutoff</label>
            <input type="range" min="10" max="35" value={quality} onChange={(e) => setQuality(e.target.value)} className="w-full accent-blue-600" />
            <div className="flex justify-between text-xs text-slate-400 mt-1"><span>10</span><span className="font-bold text-blue-600">Đã chọn: {quality}</span><span>35</span></div>
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-2">Độ dài đoạn đọc tối thiểu (Minimum Length)</label>
            <input type="number" value={minLength} onChange={(e) => setMinLength(e.target.value)} className="w-full border border-slate-300 rounded-lg px-4 py-2 outline-none" />
          </div>
        </div>
        <div className="mt-6">
          <label className="flex items-center space-x-3 text-slate-700 cursor-pointer">
            <input type="checkbox" checked={adapter} onChange={(e) => setAdapter(e.target.checked)} className="w-5 h-5 rounded border-slate-300 text-blue-600" />
            <span className="font-medium">Tự động phát hiện và cắt Illumina Adapters</span>
          </label>
        </div>
        {status && <div className="mt-4 text-center font-semibold text-blue-600">{status}</div>}
        <div className="mt-8 pt-6 border-t flex justify-end">
          <button onClick={handleTrim} disabled={loading} className="px-6 py-2.5 bg-slate-800 text-white font-semibold rounded-lg hover:bg-slate-700 transition">Tiến hành Trimming (Thủ công)</button>
        </div>
      </div>
    </div>
  );
}