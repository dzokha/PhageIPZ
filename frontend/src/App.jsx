import React, { useState } from 'react';
import Header from './components/layout/Header';
import Sidebar from './components/layout/Sidebar';
import AutoPilot from './components/tabs/AutoPilot';
import RawDataQC from './components/tabs/RawDataQC';
import Trimming from './components/tabs/Trimming';
import Assembly from './components/tabs/Assembly';
import Annotation from './components/tabs/Annotation';
import Visualization from './components/tabs/Visualization';

export default function App() {
  const [activeTab, setActiveTab] = useState('qc1');

  const renderTabContent = () => {
    switch (activeTab) {
      case 'auto': return <AutoPilot />;
      case 'qc1': return <RawDataQC type="raw" />;
      case 'trim': return <Trimming />;
      case 'qc2': return <RawDataQC type="clean" />;
      case 'assembly': return <Assembly />;
      case 'annotation': return <Annotation />;
      case 'visualize': return <Visualization />;
      default: return <RawDataQC type="raw" />;
    }
  };

  return (
    <div className="bg-slate-50 text-slate-800 antialiased h-screen flex flex-col overflow-hidden">
      {/* 1. Header trên cùng */}
      <Header />

      <div className="flex flex-1 overflow-hidden max-w-7xl mx-auto w-full relative">
        {/* 2. Sidebar bên trái (Đã bỏ class 'hidden' để luôn luôn hiển thị) */}
        <div className="w-72 flex-shrink-0 border-r border-slate-200 bg-white z-10">
            <Sidebar activeTab={activeTab} setActiveTab={setActiveTab} />
        </div>

        {/* 3. Nội dung chính bên phải */}
        <main className="flex-1 overflow-y-auto p-6 md:p-8 bg-slate-50">
          {renderTabContent()}
        </main>
      </div>
    </div>
  );
}