import React, { useState } from 'react';
import api from '../api/axios';
import { Database, Download, Upload, CheckSquare, Square, AlertTriangle, CheckCircle2 } from 'lucide-react';

export default function DatabaseBackup() {
  // stav pro výběr tabulek k záloze
  const [selectedTables, setSelectedTables] = useState({
    users: true,
    folders: true,
    files: true,
    shares: true,
    storage: true
  });

  const [importFile, setImportFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [statusMessage, setStatusMessage] = useState(null);

  // Přepínání výběrů v tabulce k záloze
  const toggleTable = (key) => {
    setSelectedTables(prev => ({ ...prev, [key]: !prev[key] }));
  };

  // hromadný výběr / zrušení
  const toggleAll = (value) => {
    setSelectedTables({
      users: value,
      folders: value,
      files: value,
      shares: value,
      storage: value
    });
  };

  // EXPORT DATABÁZE PŘES POST A BLOB
  const handleExport = async () => {
    const activeTables = Object.keys(selectedTables).filter(k => selectedTables[k]);

    if (activeTables.length === 0) {
      alert("Vyberte alespoň jednu tabulku k exportu.");
      return;
    }

    try {
      setLoading(true);
      setStatusMessage(null);

      let payload = [];

      // Pokud jsou vybrány všechny, posíláme ['all'], jinak konkrétní názvy tabulek
      if (activeTables.length === 5) {
        payload.push('all');
      } else {
        // mapování klíčů frontendu na reálné názvy tabulek očekávané backendem
        activeTables.forEach(t => {
          if (t === 'files') 
            payload.push('stored_files');
          else if (t === 'storage') 
            payload.push('storage_roots');
          else if (t === 'shares') 
            payload.push('share_permissions');
          else payload.push(t);
        });
      }

      const response = await api.post('/api/nas/database/export', payload, {
        responseType: 'blob' // pro zachování binárních dat JSONu
      });

      // Vytvoření dočasného skrytého stahovacího odkazu
      const blob = new Blob([response.data], { type: 'application/json' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');

      link.href = url;
      // nastavení názvu souboru pro stažení
      link.setAttribute('download', 'nas_db_backup.json');
      document.body.appendChild(link);

      link.click();

      // vyčištění stop v HTML a uvolnění URL objektu
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);

      setStatusMessage({ type: 'success', text: 'Záloha databáze byla úspěšně stažena.' });
    } catch (err) {
      console.error("Export databáze selhal", err);
      setStatusMessage({ type: 'error', text: 'Export selhal: ' + (err.response?.data || err.message || 'Neznámá chyba serveru') });
    } finally {
      setLoading(false);
    }
  };


  // IMPORT / OBNOVA DATABÁZE
  const handleImport = async (e) => {
    e.preventDefault();
    if (!importFile) return;

    if (!window.confirm("VAROVÁNÍ: Import doplní chybějící data do stávající DB. Chcete pokračovat?")) {
    return;
    }

    const formData = new FormData();
    formData.append('file', importFile);

    try {
      setLoading(true);
      setStatusMessage(null);

      await api.post('/api/nas/database/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
      });

      setStatusMessage({ type: 'success', text: 'Databáze byla úspěšně obnovena!' });
      setImportFile(null);
    } catch (err) {
      console.error("Import databáze selhal", err);
      setStatusMessage({ type: 'error', text: 'Import DB selhal: ' + (err.response?.data || err.message) });
    } finally {
      setLoading(false);
    }

  };

  return (
    <div className="space-y-6 w-full">
      {/* Hlavička */}
      <div className="bg-zinc-900 border border-zinc-800 p-6 rounded-2xl flex items-center space-x-4 shadow-xl">
        <div className="p-3 bg-emerald-500/10 text-emerald-400 rounded-xl">
          <Database size={32} />
        </div>
        <div>
          <h2 className="text-xl font-bold text-white">Správa a zálohování databáze</h2>
          <p className="text-xs text-zinc-400 mt-0.5">Exportujte data z tabulek databáze nebo obnovte databázi ze zálohy.</p>
        </div>
      </div>

      {statusMessage && (
        <div className={`p-4 rounded-xl border flex items-center space-x-3 text-sm ${
          statusMessage.type === 'success'
            ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
            : 'bg-red-500/10 border-red-500/30 text-red-300'
        }`}>
          {statusMessage.type === 'success' ? <CheckCircle2 size={20} /> : <AlertTriangle size={20} />}
          <span>{statusMessage.text}</span>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">

        {/* KARTA EXPORTU */}
        <div className="bg-zinc-900 border border-zinc-800 p-6 rounded-2xl space-y-5 flex flex-col justify-between shadow-lg">
          <div className="space-y-4">
            <div className="flex items-center justify-between border-b border-zinc-800 pb-3">
              <h3 className="font-bold text-white text-base flex items-center space-x-2">
                <Download size={18} className="text-emerald-400" />
                <span>Export Databáze</span>
              </h3>
              <div className="space-x-2 text-xs">
                <button onClick={() => toggleAll(true)} className="text-emerald-400 hover:underline">Vše</button>
                <span className="text-zinc-600">|</span>
                <button onClick={() => toggleAll(false)} className="text-zinc-500 hover:underline">Nic</button>
              </div>
            </div>

            <p className="text-xs text-zinc-400">Vyberte tabulky, které mají být zahrnuty do záložního JSON souboru:</p>

            <div className="space-y-2">
              {[
                { id: 'users', label: 'Uživatelé' },
                { id: 'folders', label: 'Složky' },
                { id: 'files', label: 'Soubory' },
                { id: 'shares', label: 'Pravidla sdílení' },
                { id: 'storage', label: 'Disky NASu' }
              ].map(item => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => toggleTable(item.id)}
                  className="w-full flex items-center space-x-3 p-2.5 rounded-lg bg-zinc-950/60 border border-zinc-800/80 hover:bg-zinc-800/40 text-left transition"
                >
                  {selectedTables[item.id] ? <CheckSquare size={16} className="text-emerald-400" /> : <Square size={16} className="text-zinc-600" />}
                  <span className="text-xs font-semibold text-zinc-300">{item.label}</span>
                </button>
              ))}
            </div>
          </div>

          <button
            onClick={handleExport}
            disabled={loading}
            className="w-full py-3 bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 text-white text-xs font-bold rounded-xl transition flex items-center justify-center space-x-2 shadow-lg shadow-emerald-500/10 cursor-pointer"
          >
            <Download size={16} />
            <span>Stáhnout vybranou zálohu (.json)</span>
          </button>
        </div>

        {/* KARTA IMPORTU / OBNOVY */}
        <div className="bg-zinc-900 border border-zinc-800 p-6 rounded-2xl space-y-5 flex flex-col justify-between shadow-lg">
          <div className="space-y-4">
            <div className="border-b border-zinc-800 pb-3">
              <h3 className="font-bold text-white text-base flex items-center space-x-2">
                <Upload size={18} className="text-blue-400" />
                <span>Obnova / Import DB</span>
              </h3>
            </div>

            <p className="text-xs text-zinc-400">Nahrajte dříve vyexportovaný `.json` soubor se zálohou databáze pro obnovení dat.</p>

            <form id="importForm" onSubmit={handleImport} className="space-y-4">
              <label className="border-2 border-dashed border-zinc-800 hover:border-blue-500/50 bg-zinc-950/60 p-6 rounded-xl flex flex-col items-center justify-center cursor-pointer transition text-center space-y-2">
                <Upload size={24} className="text-zinc-500" />
                <span className="text-xs text-zinc-300 font-medium">
                  {importFile ? importFile.name : 'Klikněte pro výběr JSON souboru zálohy'}
                </span>
                <input
                  type="file"
                  accept=".json"
                  onChange={e => setImportFile(e.target.files[0])}
                  className="hidden"
                />
              </label>
            </form>
          </div>

          <button
            type="submit"
            form="importForm"
            disabled={!importFile || loading}
            className="w-full py-3 bg-blue-600 hover:bg-blue-500 disabled:bg-zinc-800 disabled:text-zinc-600 text-white text-xs font-bold rounded-xl transition flex items-center justify-center space-x-2 shadow-lg shadow-blue-500/10 cursor-pointer"
          >
            <Upload size={16} />
            <span>Spustit obnovu databáze</span>
          </button>
        </div>

      </div>
    </div>
  );
}
