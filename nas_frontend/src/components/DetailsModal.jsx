import React from 'react';
import { X, Info, Calendar, HardDrive, User, Tag, Key } from 'lucide-react';

export default function DetailsModal({ item, type, onClose }) {
  // pomocná funkce na formátování velikosti souborů
  const formatSize = (bytes) => {
    if (!bytes && bytes !== 0) return '---';
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k)); // dle počtu bajtů určíme maximální index jednotky (B, KB, MB, GB)
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  // pomocná funkce pro normální zobrazení datumu z Javy
  const formatDate = (dateString) => {
    if (!dateString) return '---';
    const date = new Date(dateString);
    return date.toLocaleString('cs-CZ', { dateStyle: 'medium', timeStyle: 'short' });
  };

  const isFile = type === 'file';

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center p-4 z-50 backdrop-blur-sm animate-fadeIn">
      <div className="bg-zinc-900 border border-zinc-800 p-6 rounded-2xl max-w-md w-full space-y-5 shadow-2xl relative">
        
        {/* HLAVIČKA */}
        <div className="flex justify-between items-center border-b border-zinc-800/60 pb-3 shrink-0">
          <div className="flex items-center space-x-2.5 text-emerald-400">
            <Info size={18} />
            <h3 className="text-sm font-bold text-white uppercase tracking-wide">Podrobnosti o {isFile ? 'souboru' : 'složce'}</h3>
          </div>
          <button onClick={onClose} className="p-1.5 text-zinc-400 hover:text-white bg-zinc-800/40 hover:bg-zinc-700/60 rounded-xl transition cursor-pointer">
            <X size={14} />
          </button>
        </div>

        {/* VLASTNOSTI / DETAILY */}
        <div className="space-y-3.5 text-sm">
          
          {/* NÁZEV */}
          <div className="bg-zinc-950 border border-zinc-900 p-3 rounded-xl flex items-start space-x-3">
            <Tag size={16} className="text-zinc-500 mt-0.5 shrink-0" />
            <div className="min-w-0 flex-1">
              <div className="text-[10px] font-bold text-zinc-500 uppercase tracking-wider">Název</div>
              <div className="text-zinc-200 font-semibold truncate mt-0.5">{isFile ? item.originalName : item.name}</div>
            </div>
          </div>

          {/* UNIKÁTNÍ ID (bylo nutné pro přesuny, teď je tam stromová struktura na ty přesuny a tohle je více méně navíc...) */}
          <div className="bg-zinc-950 border border-zinc-900 p-3 rounded-xl flex items-start space-x-3">
            <Key size={16} className="text-zinc-500 mt-0.5 shrink-0" />
            <div className="min-w-0 flex-1">
              <div className="text-[10px] font-bold text-zinc-500 uppercase tracking-wider">Unikátní ID (Databáze)</div>
              <div className="text-emerald-400 font-mono text-xs select-all bg-zinc-900/40 px-1.5 py-0.5 rounded border border-zinc-800/20 mt-1 inline-block break-all">
                {item.id}
              </div>
            </div>
          </div>

          {/* VELIKOST a TYP (Jen pro soubory) */}
          {isFile && (
            <div className="grid grid-cols-2 gap-3">
              <div className="bg-zinc-950 border border-zinc-900 p-3 rounded-xl flex items-start space-x-3">
                <HardDrive size={16} className="text-zinc-500 mt-0.5 shrink-0" />
                <div>
                  <div className="text-[10px] font-bold text-zinc-500 tracking-wider uppercase">Velikost</div>
                  <div className="text-zinc-200 font-semibold mt-0.5">{formatSize(item.fileSize)}</div>
                </div>
              </div>
              <div className="bg-zinc-950 border border-zinc-900 p-3 rounded-xl flex items-start space-x-3 min-w-0">
                <div className="min-w-0">
                  <div className="text-[10px] font-bold text-zinc-500 tracking-wider uppercase truncate">Datový typ</div>
                  <div className="text-zinc-300 font-medium text-xs mt-1 truncate bg-zinc-900/30 px-1.5 py-0.5 rounded border border-zinc-800/10 inline-block">{item.contentType}</div>
                </div>
              </div>
            </div>
          )}

          {/* VLASTNÍK */}
          <div className="bg-zinc-950 border border-zinc-900 p-3 rounded-xl flex items-start space-x-3">
            <User size={16} className="text-zinc-500 mt-0.5 shrink-0" />
            <div>
              <div className="text-[10px] font-bold text-zinc-500 tracking-wider uppercase">Vlastník (Owner)</div>
              <div className="text-zinc-200 font-semibold mt-0.5">{item.ownerUsername}</div>
            </div>
          </div>

          {/* DATUMY */}
          <div className="grid grid-cols-2 gap-3">
            <div className="bg-zinc-950 border border-zinc-900 p-3 rounded-xl flex items-start space-x-2">
              <Calendar size={15} className="text-zinc-500 mt-0.5 shrink-0" />
              <div>
                <div className="text-[10px] font-bold text-zinc-500 tracking-wider uppercase">Nahráno</div>
                <div className="text-zinc-300 font-medium text-xs mt-0.5">{formatDate(item.createdAt)}</div>
              </div>
            </div>
            <div className="bg-zinc-950 border border-zinc-900 p-3 rounded-xl flex items-start space-x-2">
              <Calendar size={15} className="text-zinc-500 mt-0.5 shrink-0" />
              <div>
                <div className="text-[10px] font-bold text-zinc-500 tracking-wider uppercase">Upraveno</div>
                <div className="text-zinc-300 font-medium text-xs mt-0.5">{formatDate(isFile ? item.editedAt : item.createdAt)}</div>
              </div>
            </div>
          </div>

        </div>

        {/* TLAČÍTKO OK */}
        <div className="pt-2">
          <button onClick={onClose} className="w-full py-2.5 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 text-xs font-bold rounded-xl border border-zinc-700 transition cursor-pointer">
            Rozumím
          </button>
        </div>

      </div>
    </div>
  );
}
