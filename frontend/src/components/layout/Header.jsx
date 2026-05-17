import React from 'react';
import { FaDna, FaBrain, FaDatabase, FaServer } from 'react-icons/fa';

export default function Header() {
  return (
    <header className="bg-white shadow-sm border-b border-slate-200 z-10">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        
        {/* Logo & Tên dự án */}
        <div className="flex items-center gap-3">
          <div className="bg-blue-600 text-white p-2 rounded-lg">
            <FaDna className="text-xl" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-slate-900 leading-tight">PhageIPZ Workspace</h1>
            <p className="text-xs text-slate-500 font-medium">Intelligent Phage Genomics Pipeline</p>
          </div>
        </div>
        
        {/* Khu vực công cụ */}
        <div className="flex items-center gap-4">
          <div className="flex items-center bg-slate-100 rounded-full px-1 border border-slate-200 shadow-sm hidden md:flex">
            <span className="text-[10px] font-bold text-slate-400 uppercase ml-3 mr-2 flex items-center">
              <FaBrain className="mr-1" /> AI Engine:
            </span>
            <select id="global-ai-select" className="text-xs font-bold text-indigo-600 bg-transparent py-1.5 outline-none cursor-pointer">
              <option value="chatgpt"> ChatGPT 4.0 (Online)</option>
              <option value="ollama"> Llama 3 (Offline)</option>
            </select>
          </div>

          <button className="text-sm font-semibold px-4 py-1.5 bg-slate-100 text-slate-700 rounded-full hover:bg-slate-200 transition shadow-sm border border-slate-200 flex items-center">
            <FaDatabase className="mr-1" /> Quản lý CSDL
          </button>
          
          <span className="text-sm font-medium px-3 py-1 bg-emerald-100 text-emerald-700 rounded-full flex items-center hidden sm:flex">
            <FaServer className="mr-1" /> Java Backend Ready
          </span>
        </div>
      </div>
    </header>
  );
}