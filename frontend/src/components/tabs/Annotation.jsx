import React, { useState } from 'react';
import { FaTags, FaFileCode } from 'react-icons/fa';

export default function Annotation() {
  const [file, setFile] = useState(null);
  const [status, setStatus] = useState('');

  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-fade-in">
      <div className="mb-6"><h2 className="text-2xl font-bold text-slate-800">Bước 5: Chú giải bộ gene (Annotation)</h2></div>
      <div className="bg-white p-8 rounded-2xl shadow-sm border border-slate-200 text-center hover:border-emerald-400 transition mb-6">
        <input type="file" id="fileInputAnnotation" accept=".fasta,.fa,.fna" className="hidden" onChange={(e) => setFile(e.target.files[0])} />
        <label htmlFor="fileInputAnnotation" className="cursor-pointer block">
          <div className="w-20 h-20 mx-auto bg-emerald-50 text-emerald-500 rounded-full flex items-center justify-center mb-4"><FaFileCode className="text-3xl" /></div>
          <h3 className="text-lg font-semibold text-slate-800 mb-1">Kéo thả file Fasta vào đây</h3>
          {file && <span className="inline-block mt-2 bg-emerald-100 text-emerald-700 px-3 py-1 rounded-full text-xs font-medium">{file.name}</span>}
        </label>
      </div>
      <div className="bg-white p-8 rounded-2xl shadow-sm border border-slate-200 text-center">
        <FaTags className="text-6xl text-emerald-400 mb-4 mx-auto" />
        <h3 className="text-xl font-bold text-slate-800 mb-2">PhageIPZ Annotation Engine</h3>
        {status && <div className="mt-4 font-semibold text-emerald-600">{status}</div>}
        <button onClick={() => setStatus("Đang kích hoạt quy trình chú giải gen Phage...")} className="mt-6 px-8 py-3 bg-emerald-600 text-white font-semibold rounded-xl hover:bg-emerald-700 transition">Bắt đầu chú giải</button>
      </div>
    </div>
  );
}