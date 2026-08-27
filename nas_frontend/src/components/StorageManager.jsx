import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { HardDrive, Plus, CheckCircle, Circle } from 'lucide-react';

export default function StorageManager() {
  const [disks, setDisks] = useState([]);
  const [diskName, setDiskName] = useState('');
  const [basePath, setBasePath] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const fetchDisks = () => {
    api.get('/api/admin/disks')
      .then(res => setDisks(res.data))
      .catch(err => console.error("Nelze načíst disky", err));
  };

  useEffect(() => {
    fetchDisks();
  }, []);

  const handleAddDisk = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    try {
      await api.post('/api/admin/disks', { diskName, basePath });
      setDiskName('');
      setBasePath('');
      setSuccess('Nový úložný prostor byl úspěšně přidán.');
      fetchDisks();
    } catch (err) {
      setError(err.response?.data || 'Nepodařilo se přidat disk.');
    }
  };

  const handleActivateDisk = async (id) => {
    try {
      await api.post(`/api/admin/disks/${id}/activate`);
      setSuccess('Disk byl úspěšně aktivován pro nahrávání.');
      fetchDisks();
    } catch (err) {
      alert("Aktivace selhala: " + (err.response?.data || err.message));
    }
  };

  const handleDeactivateDisk = async (id) => {
    try {
        await api.post(`/api/admin/disks/${id}/deactivate`);
        setSuccess('Disk byl úspěšně odpojen pro nahrávání.');
        fetchDisks(); // Znovu načíst tabulku
    } catch (err) {
        alert("Odpojení selhalo: " + (err.response?.data || err.message));
    }
  };

const handleDeleteDisk = async (id) => {
  if (!window.confirm("Opravdu chcete tento disk odebrat ze systému NAS?")) return;
  try {
    await api.delete(`/api/admin/disks/${id}`);
    setSuccess('Disk byl úspěšně odebrán ze systému.');
    fetchDisks();
  } catch (err) {
    setError(err.response?.data || 'Nepodařilo se odebrat disk.');
  }
};

  return (
    <div className="space-y-6">
      <div className="bg-zinc-900 border border-zinc-800 p-4 rounded-xl shadow-md">
        <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
          <HardDrive className="text-emerald-500" size={22} />
          Správa úložných prostorů (Disky)
        </h1>
      </div>

      {error && <div className="p-3 bg-red-500/10 border border-red-500/20 text-red-400 rounded-lg text-sm">{error}</div>}
      {success && <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 rounded-lg text-sm">{success}</div>}

      {/* FORMULÁŘ PRO PŘIDÁNÍ DISKU */}
    <form onSubmit={handleAddDisk} className="bg-zinc-900 border border-zinc-800 p-4 rounded-xl flex flex-col sm:flex-row items-stretch sm:items-center gap-3 w-full max-w-4xl">
      <input 
        type="text" 
        required 
        value={diskName} 
        onChange={e => setDiskName(e.target.value)} 
        placeholder="Název (např. NAS_SSD_1)"
        className="flex-1 bg-zinc-950 border border-zinc-800 rounded-lg px-4 py-2.5 text-sm text-white focus:outline-none focus:border-emerald-500 min-w-[200px]" 
      />
      
      <input 
        type="text" 
        required 
        value={basePath} 
        onChange={e => setBasePath(e.target.value)} 
        placeholder="Absolutní cesta (např. /mnt/nas)"
        className="flex-1 bg-zinc-950 border border-zinc-800 rounded-lg px-4 py-2.5 text-sm text-white focus:outline-none focus:border-emerald-500 min-w-[250px]" 
      />
      
      <button 
        type="submit" 
        className="flex items-center justify-center space-x-2 px-5 py-2.5 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg text-sm font-bold transition duration-150 whitespace-nowrap"
      >
        <Plus size={16} />
        <span>Přidat disk</span>
      </button>
    </form>


      {/* TABULKA REGISTROVANÝCH DISKŮ */}
      <div className="bg-zinc-900/40 border border-zinc-900 rounded-xl overflow-hidden shadow-xl">
        <div className="p-4 border-b border-zinc-800/50 bg-zinc-900/60">
          <h2 className="text-xs font-bold uppercase tracking-wider text-zinc-400">Seznam připojených disků</h2>
        </div>
        <div className="divide-y divide-zinc-800/40">
          {disks.length === 0 ? (
            <div className="p-8 text-center text-zinc-500 text-sm">V systému nejsou registrovány žádné disky.</div>
          ) : (
            disks.map(disk => (
              <div key={disk.id} className="p-4 flex items-center justify-between hover:bg-zinc-800/10 transition">
                <div className="flex items-center space-x-3">
                  {disk.active ? <CheckCircle size={20} className="text-emerald-500" /> : <Circle size={20} className="text-zinc-600" />}
                  <div>
                    <div className="text-sm font-bold text-zinc-200 flex items-center gap-2">
                      {disk.diskName}
                      {disk.active && <span className="text-[10px] bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-1.5 py-0.5 rounded font-bold uppercase tracking-wider">Aktivní pro zápis</span>}
                    </div>
                    <div className="text-xs text-zinc-500 font-mono mt-0.5">{disk.basePath}</div>
                  </div>
                </div>
                {/* Úprava tlačítek v tabulce disků */}
                {disk.active ? (
                  <button onClick={() => handleDeactivateDisk(disk.id)} className="px-3 py-1.5 bg-red-500/10 hover:bg-red-500/20 text-red-400 text-xs font-bold rounded-lg border border-red-500/20 transition cursor-pointer">
                    Odpojit disk
                  </button>
                ) : (
                  <div className="flex items-center space-x-2">
                    <button onClick={() => handleActivateDisk(disk.id)} className="px-3 py-1.5 bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-xs font-bold rounded-lg border border-zinc-700 transition cursor-pointer">
                      Aktivovat
                    </button>
                    <button onClick={() => handleDeleteDisk(disk.id)} className="px-3 py-1.5 bg-red-500/10 hover:bg-red-500/20 text-red-400 text-xs font-bold rounded-lg border border-red-500/20 transition cursor-pointer">
                      Odebrat
                    </button>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
