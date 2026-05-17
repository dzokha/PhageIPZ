import React from 'react';
import { FaBolt, FaFilter, FaLayerGroup, FaTags, FaEye } from 'react-icons/fa';
import { FaCheckDouble } from 'react-icons/fa6';

export default function Sidebar({ activeTab, setActiveTab }) {
  const tabs = [
    { id: 'auto', label: '1-Click Pipeline', desc: 'Chạy toàn bộ quy trình', icon: <FaBolt />, isSpecial: true },
    { id: 'qc1', label: 'Raw Data QC', desc: 'FastQC Analysis', number: '1' },
    { id: 'trim', label: 'Trimming', desc: 'Làm sạch dữ liệu', number: '2' },
    { id: 'qc2', label: 'Clean QC', desc: 'Kiểm tra lại sau khi cắt', number: '3' },
    { id: 'assembly', label: 'Assembly', desc: 'PhageIPZ Native Engine', number: '4' },
    { id: 'annotation', label: 'Annotation', desc: 'PhageIPZ Engine', number: '5' },
    { id: 'visualize', label: 'Visualization', desc: 'Linear & Circular Map', number: '6' }
  ];

  return (
    <aside className="w-72 bg-white border-r border-slate-200 flex flex-col py-6 overflow-y-auto hidden md:flex">
      <div className="px-6 mb-4">
        <h2 className="text-xs font-bold text-slate-400 uppercase tracking-wider">Chế độ vận hành</h2>
      </div>
      
      <nav className="flex-1 px-4 space-y-1">
        {tabs.map((tab) => {
          const isActive = activeTab === tab.id;
          
          // Logic style cho nút đặc biệt (AutoPilot)
          if (tab.isSpecial) {
            return (
              <React.Fragment key={tab.id}>
                <button
                  onClick={() => setActiveTab(tab.id)}
                  className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl font-bold transition-all border ${isActive ? 'bg-amber-50 text-amber-700 border-amber-200' : 'text-slate-600 hover:bg-slate-50 border-transparent'}`}
                >
                  <div className={`flex items-center justify-center w-8 h-8 rounded-full ${isActive ? 'bg-amber-100 text-amber-600' : 'bg-slate-100 text-slate-500'}`}>
                    {tab.icon}
                  </div>
                  <div className="text-left">
                    <div className="text-sm">{tab.label}</div>
                    <div className="text-xs font-normal opacity-80">{tab.desc}</div>
                  </div>
                </button>
                <div className="my-4 border-t border-slate-100"></div>
              </React.Fragment>
            );
          }

          // Logic style cho các nút thông thường
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl font-medium transition-all ${isActive ? 'bg-blue-50 text-blue-700 font-semibold' : 'text-slate-600 hover:bg-slate-50'}`}
            >
              <div className={`flex items-center justify-center w-8 h-8 rounded-full ${isActive ? 'bg-blue-100 text-blue-600' : 'bg-slate-100 text-slate-500'}`}>
                {tab.number}
              </div>
              <div className="text-left">
                <div className="text-sm">{tab.label}</div>
                <div className="text-xs font-normal opacity-80">{tab.desc}</div>
              </div>
            </button>
          );
        })}
      </nav>

      <div className="px-6 mt-auto">
        <footer className="text-center text-sm text-gray-500 mt-10 pb-5">
          PhageIPZ System &copy; 2026 Dzokha.
        </footer>
      </div>
    </aside>
  );
}