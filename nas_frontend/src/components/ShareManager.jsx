import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { Folder, File, UserMinus, Share2 } from 'lucide-react';

export default function ShareManager() {
  const [iShare, setIShare] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchPermissions = () => {
    setLoading(true);
    api.get('/api/nas/share/i-share')
      .then(res => setIShare(res.data))
      .catch(err => console.error(err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchPermissions();
  }, []);

  const handleRevoke = async (permId) => {
    if (!window.confirm("Opravdu chcete zrušit přístup k tomuto zdroji?")) return;
    try {
      await api.delete(`/api/nas/share/${permId}`);
      fetchPermissions(); // Okamžitá aktualizace tabulky
    } catch (err) {
      alert("Chyba při rušení práv: " + (err.response?.data || err.message));
    }
  };

  if (loading) return <div className="text-zinc-500 text-center py-12">Načítám přehled sdílení...</div>;

  return (
    <div className="space-y-6">
      <div className="bg-zinc-900 border border-zinc-800 p-4 rounded-xl shadow-md">
        <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
          <Share2 className="text-emerald-400" size={22} />
          Správa sdílení (Moje udělená práva)
        </h1>
      </div>

      <div className="bg-zinc-900/40 border border-zinc-900 rounded-xl overflow-hidden shadow-xl">
        <div className="divide-y divide-zinc-800/40">
          {iShare.length === 0 ? (
            <div className="p-8 text-center text-zinc-500 text-sm">Aktuálně nikomu žádný soubor ani složku nesdílíte.</div>
          ) : (
            iShare.map(perm => {
              // soubor nebo složka
              const isFile = perm.file != null;
              const resourceName = isFile ? perm.file.originalName : perm.folder.name;
              
              return (
                <div key={perm.id} className="p-4 flex items-center justify-between hover:bg-zinc-800/10 transition">
                  <div className="flex items-center space-x-3">
                    {isFile ? <File className="text-zinc-400" size={20} /> : <Folder className="text-amber-500" size={20} />}
                    <div>
                      <div className="text-sm font-bold text-zinc-200">
                        {resourceName}
                        <span className="text-xs text-zinc-500 font-normal ml-2">({isFile ? 'soubor' : 'složka'})</span>
                      </div>
                      <div className="text-xs text-zinc-500 mt-1">
                        Uživatel s přístupem: <span className="text-emerald-400 font-bold">{perm.sharedWith.username}</span> ({perm.sharedWith.email})
                        <span className="mx-2">•</span> Oprávnění: 
                        <span className={`ml-1 font-bold ${perm.canWrite ? 'text-amber-500' : 'text-blue-400'}`}>
                          {perm.canWrite ? 'ČTENÍ + ZÁPIS (WRITE)' : 'POUZE ČTENÍ (READ)'}
                        </span>
                      </div>
                    </div>
                  </div>
                  {/* TLAČÍTKO PRO ODEBRÁNÍ PRÁVA */}
                  <button onClick={() => handleRevoke(perm.id)} className="p-2 text-zinc-500 hover:text-red-400 hover:bg-red-500/5 rounded-lg transition" title="Zrušit sdílení">
                    <UserMinus size={16} />
                  </button>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
}
