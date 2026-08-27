import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { Cpu, HardDrive, LayoutGrid } from 'lucide-react';

export default function HwMonitoring() {
  const [stats, setStats] = useState(null);

  const fetchStats = () => {
    api.get('/api/admin/monitoring/stats')
      .then(res => setStats(res.data))
      .catch(err => console.error(err));
  };

  useEffect(() => {
    fetchStats();
    const interval = setInterval(fetchStats, 3000);
    return () => clearInterval(interval);
  }, []);

  if (!stats) return <div className="text-zinc-500 py-8 text-center">Načítám HW statistiky serveru...</div>;

  const ramPercent = Math.round((stats.ramUsedBytes / stats.ramTotalBytes) * 100);
  const formatGb = (bytes) => (bytes / (1024 * 1024 * 1024)).toFixed(1) + " GB";

  return (
    <div className="space-y-6">
      <div className="bg-zinc-900 border border-zinc-800 p-4 rounded-xl shadow-md">
        <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
          <Cpu className="text-emerald-500" size={22} />
          Monitoring Debian serveru (Real-time)
        </h1>
      </div>

      {/* DYNAMICKÁ MŘÍŽKA S KARTAMI */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        
        {/* CPU */}
        <div className="bg-zinc-900 border border-zinc-800 p-6 rounded-2xl shadow-xl flex flex-col justify-between">
          <div className="flex items-center justify-between mb-4">
            <span className="text-sm font-bold uppercase tracking-wider text-zinc-400">Procesor (CPU)</span>
            <Cpu className="text-emerald-400" size={20} />
          </div>
          <div className="text-4xl font-extrabold text-white">{stats.cpuUsagePercent} %</div>
          <div className="w-full bg-zinc-950 rounded-full h-2.5 mt-4 overflow-hidden border border-zinc-800">
            <div className="bg-emerald-500 h-2.5 rounded-full transition-all" style={{ width: `${stats.cpuUsagePercent}%` }}></div>
          </div>
        </div>

        {/* RAM */}
        <div className="bg-zinc-900 border border-zinc-800 p-6 rounded-2xl shadow-xl flex flex-col justify-between">
          <div className="flex items-center justify-between mb-4">
            <span className="text-sm font-bold uppercase tracking-wider text-zinc-400">Operační paměť (RAM)</span>
            <LayoutGrid className="text-blue-400" size={20} />
          </div>
          <div className="text-4xl font-extrabold text-white">{ramPercent} %</div>
          <div className="text-xs text-zinc-500 font-medium mt-1">Využito {formatGb(stats.ramUsedBytes)} z {formatGb(stats.ramTotalBytes)}</div>
          <div className="w-full bg-zinc-950 rounded-full h-2.5 mt-4 overflow-hidden border border-zinc-800">
            <div className="bg-blue-500 h-2.5 rounded-full transition-all" style={{ width: `${ramPercent}%` }}></div>
          </div>
        </div>

        {/* PRO VŠECHNY REGISTROVANÉ DISKY V NASU (dynamicky) */}
        {stats.disks.map((disk, idx) => {
          const diskPercent = disk.totalBytes > 0 ? Math.round((disk.usedBytes / disk.totalBytes) * 100) : 0;
          return (
            <div key={idx} className="bg-zinc-900 border border-zinc-800 p-6 rounded-2xl shadow-xl flex flex-col justify-between relative overflow-hidden">
              <div className="flex items-center justify-between mb-4">
                <div className="flex flex-col">
                  <span className="text-sm font-bold uppercase tracking-wider text-zinc-400 truncate max-w-[180px]">{disk.diskName}</span>
                  <span className="text-[10px] font-mono text-zinc-600 truncate max-w-[180px] mt-0.5">{disk.basePath}</span>
                </div>
                <HardDrive className={disk.active ? "text-emerald-400" : "text-zinc-600"} size={20} />
              </div>
              <div className="text-4xl font-extrabold text-white">
                {diskPercent} %
                {disk.active && <span className="ml-2 text-[9px] bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-1.5 py-0.5 rounded font-bold uppercase tracking-widest align-middle">Zápis</span>}
              </div>
              <div className="text-xs text-zinc-500 font-medium mt-1">Obsazeno {formatGb(disk.usedBytes)} z {formatGb(disk.totalBytes)}</div>
              <div className="w-full bg-zinc-950 rounded-full h-2.5 mt-4 overflow-hidden border border-zinc-800">
                <div className={`h-2.5 rounded-full transition-all ${disk.active ? 'bg-emerald-500' : 'bg-amber-500'}`} style={{ width: `${diskPercent}%` }}></div>
              </div>
            </div>
          );
        })}

      </div>
    </div>
  );
}
