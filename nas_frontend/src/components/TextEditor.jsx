import React, { useState, useEffect, useRef } from 'react';
import api from '../api/axios';
import { X, Save, FileCode, CheckCircle } from 'lucide-react';

export default function TextEditor({ fileId, filename, onClose, onSaveSuccess }) {
  const [text, setText] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [statusMessage, setStatusMessage] = useState('Připraven');
  const textareaRef = useRef(null);

  useEffect(() => {
    setLoading(true);
    api.get(`/api/nas/files/download/${fileId}`, { responseType: 'text' })
      .then(res => {
        setText(res.data);
      })
      .catch(err => {
        console.error(err);
        setStatusMessage('Chyba při načítání souboru');
      })
      .finally(() => setLoading(false));
  }, [fileId]);

  const handleSave = async () => {
    setSaving(true);
    setStatusMessage('Ukládám soubor na disk...');
    try {
      const blob = new Blob([text], { type: 'text/plain' });
      const formData = new FormData();
      formData.append('file', blob, filename);
      formData.append('fileId', fileId);

      await api.put('/api/nas/files/update', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      setStatusMessage('Uloženo úspěšně!');
      setTimeout(() => setStatusMessage('Změny synchronizovány'), 3000);
      onSaveSuccess();
    } catch (err) {
      setStatusMessage('Uložení selhalo!');
      alert("Uložení selhalo: " + (err.response?.data || err.message));
    } finally {
      setSaving(false);
    }
  };

  // Spočítáme počet řádků pro dynamické vykreslení levého panelu s čísly řádků
  const lineCount = text.split('\n').length;
  const lineNumbers = Array.from({ length: lineCount }, (_, i) => i + 1);

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50 backdrop-blur-md">
      <div className="bg-zinc-900 border border-zinc-800 rounded-2xl max-w-5xl w-full h-[85vh] flex flex-col overflow-hidden shadow-2xl relative animate-fadeIn">
        
        {/* LIŠTA OKNA (HEAD HEADER) */}
        <div className="flex justify-between items-center bg-zinc-900 px-6 py-4 border-b border-zinc-800/80 shrink-0">
          <div className="flex items-center space-x-3 min-w-0">
            <div className="bg-emerald-500/10 p-2 rounded-lg text-emerald-400">
              <FileCode size={18} />
            </div>
            <div className="min-w-0">
              <h3 className="text-sm font-bold text-white truncate">{filename}</h3>
              <p className="text-[11px] text-zinc-500 font-medium tracking-wide uppercase mt-0.5">Vývojářský editor kódu</p>
            </div>
          </div>
          
          <div className="flex items-center space-x-2">
            <button onClick={handleSave} disabled={saving || loading}
              className="flex items-center space-x-2 px-4 py-2 bg-emerald-500 hover:bg-emerald-600 disabled:bg-zinc-800 text-white rounded-xl text-xs font-bold transition shadow-lg shadow-emerald-500/10 cursor-pointer">
              <Save size={14} />
              <span>{saving ? 'Ukládám...' : 'Uložit'}</span>
            </button>
            <button onClick={onClose} className="p-2 text-zinc-400 hover:text-white bg-zinc-800/40 hover:bg-zinc-700/60 rounded-xl transition cursor-pointer">
              <X size={14} />
            </button>
          </div>
        </div>

        {/* WORKSPACE: ČÍSLA ŘÁDKŮ + TEXTAREA */}
        <div className="flex-1 w-full bg-zinc-950 flex overflow-hidden relative font-mono text-sm leading-relaxed">
          {loading ? (
            <div className="absolute inset-0 flex items-center justify-center text-zinc-600 text-xs tracking-wider uppercase">
              Načítám zdrojový kód...
            </div>
          ) : (
            <>
              {/* SLOUPEC S ČÍSLY ŘÁDKŮ */}
              <div className="w-12 bg-zinc-900/30 text-right pr-3 py-4 text-zinc-600 border-r border-zinc-900 select-none text-xs font-medium">
                {lineNumbers.map(num => (
                  <div key={num} className="h-[21px]">{num}</div>
                ))}
              </div>

              {/* SAMOTNÁ TEXTAREA S EDITOREM */}
              <textarea
                ref={textareaRef}
                value={text}
                onChange={e => setText(e.target.value)}
                className="flex-1 bg-transparent text-slate-300 p-4 outline-none resize-none overflow-y-auto font-mono text-sm leading-[21px] selection:bg-emerald-500/20 placeholder-zinc-700"
                spellCheck="false"
                placeholder="// Zde začíná váš kód..."
              />
            </>
          )}
        </div>

        {/* STATUS BAR (SPODNÍ LIŠTA RADY) */}
        <div className="bg-zinc-900 border-t border-zinc-800/80 px-6 py-2.5 flex justify-between items-center text-[11px] text-zinc-500 font-semibold tracking-wide uppercase shrink-0 select-none">
          <div className="flex items-center space-x-1.5">
            <CheckCircle size={12} className={statusMessage.includes('Chyba') ? 'text-red-400' : 'text-emerald-400'} />
            <span>Status: {statusMessage}</span>
          </div>
          <div>
            <span>Řádků: {lineCount}</span>
            <span className="mx-3">|</span>
            <span>UTF-8</span>
          </div>
        </div>

      </div>
    </div>
  );
}
