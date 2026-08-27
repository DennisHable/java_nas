import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { Folder, File, RotateCcw, Trash2 } from 'lucide-react';

export default function TrashBin() {
  const [trashContent, setTrashContent] = useState({ subFolders: [], files: [] });
  const [loading, setLoading] = useState(false);

  const fetchTrash = () => {
    setLoading(true);
    api.get('/api/nas/trash') // Zavolá endpoint, který vrací smazané věci
      .then(res => setTrashContent(res.data))
      .catch(err => console.error("Nelze načíst koš", err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchTrash();
  }, []);

  const handleRestoreFile = async (id) => {
    try {
      await api.post(`/api/nas/files/${id}/restore`);
      fetchTrash(); // Obnovit plochu koše
    } catch (err) {
      alert("Obnova selhala: " + (err.response?.data || err.message));
    }
  };

  const handleRestoreFolder = async (id) => {
    try {
      await api.post(`/api/nas/folders/${id}/restore`);
      fetchTrash();
    } catch (err) {
      alert("Obnova složky selhala: " + (err.response?.data || err.message));
    }
  };

    const handlePermanentDeleteFile = async (id) => {
    if (!window.confirm("Opravdu chcete tento soubor definitivně smazat z disku? Tuto akci nelze vrátit.")) return;
    try {
        await api.delete(`/api/nas/files/${id}`);
        fetchTrash(); // Obnovit seznam koše
    } catch (err) {
        setError(err.response?.data || 'Definitivní smazání selhalo.');
    }
    };

    const handlePermanentDeleteFolder = async (id) => {
  if (!window.confirm("VAROVÁNÍ: Opravdu chcete tuto složku a VEŠKERÝ JEJÍ OBSAH definitivně smazat z disku? Tuto akci nelze vrátit.")) return;
  try {
    await api.delete(`/api/nas/folders/${id}`);
    fetchTrash(); // Obnovit koš
  } catch (err) {
    alert("Smazání složky selhalo: " + (err.response?.data || err.message));
  }
};

  return (
    <div className="space-y-6">
      <div className="bg-zinc-900 border border-zinc-800 p-4 rounded-xl shadow-md">
        <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
          <Trash2 className="text-red-400" size={22} />
          Systémový koš (Trash bin)
        </h1>
      </div>

      {loading ? (
        <div className="text-center py-12 text-zinc-500">Načítám smazané položky...</div>
      ) : trashContent.subFolders.length === 0 && trashContent.files.length === 0 ? (
        <div className="text-center py-20 bg-zinc-900/10 border border-zinc-900 border-dashed rounded-2xl text-zinc-500">
          <Trash2 size={44} className="mx-auto mb-3 text-zinc-800" />
          <p className="text-sm font-medium">Koš je prázdný</p>
        </div>
      ) : (
        <div className="bg-zinc-900/40 border border-zinc-900 rounded-xl overflow-hidden shadow-xl">
          <div className="divide-y divide-zinc-800/40">
            
            {/* SMAZANÉ SLOŽKY */}
            {trashContent.subFolders.map(folder => (
              <div key={folder.id} className="flex items-center justify-between p-4 hover:bg-zinc-800/10 transition">
                <div className="flex items-center space-x-3">
                  <Folder size={20} className="text-red-400/70 fill-red-500/5" />
                  <span className="text-sm font-semibold text-zinc-300">{folder.name} <span className="text-xs text-zinc-600 font-normal ml-2">(složka)</span></span>
                </div>
                <div className="flex items-center space-x-1">
                  <button onClick={() => handleRestoreFolder(folder.id)} className="p-2 text-zinc-400 hover:text-emerald-400 hover:bg-emerald-500/5 rounded-lg transition" title="Obnovit složku">
                    <RotateCcw size={16} />
                  </button>
                  <button onClick={() => handlePermanentDeleteFolder(folder.id)} className="p-2 text-zinc-400 hover:text-red-400 hover:bg-red-500/5 rounded-lg transition" title="Definitivně smazat složku i obsah">
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            ))}

            {/* SMAZANÉ SOUBORY */}
            {trashContent.files.map(file => (
              <div key={file.id} className="flex items-center justify-between p-4 hover:bg-zinc-800/10 transition">
                <div className="flex items-center space-x-3 min-w-0 flex-1">
                  <File size={20} className="text-red-400/70" />
                  <span className="text-sm font-medium text-zinc-300 truncate pr-4">{file.originalName}</span>
                </div>
                <div className="flex items-center space-x-4 text-xs font-medium text-zinc-500 shrink-0">
                  <span>{(file.fileSize / (1024 * 1024)).toFixed(2)} MB</span>
                  <div className="flex items-center space-x-1 pl-2">
                    <button onClick={() => handleRestoreFile(file.id)} className="p-2 text-zinc-400 hover:text-emerald-400 hover:bg-emerald-500/5 rounded-lg transition" title="Obnovit soubor">
                      <RotateCcw size={16} />
                    </button>
                    <button onClick={() => handlePermanentDeleteFile(file.id)} className="p-2 text-zinc-400 hover:text-red-400 hover:bg-red-500/5 rounded-lg transition" title="Definitivně smazat z disku">
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
              </div>
            ))}

          </div>
        </div>
      )}
    </div>
  );
}
