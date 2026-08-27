import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { Folder, Share2, Trash2, Cpu, HardDrive, LogOut, Menu, X, Database } from 'lucide-react';
import FileExplorer from './FileExplorer';
import StorageManager from './StorageManager';
import HwMonitoring from './HwMonitoring';
import TrashBin from './TrashBin';
import SharedWithMe from './SharedWithMe';
import ShareManager from './ShareManager';
import DatabaseBackup from './DatabaseBackup';

export default function Dashboard({ user, onLogout }) {
  // podívá se do lokální paměti prohlížeče, pokud tam nic nenajde, použije jako výchozí 'files'; určuje to která sekce se má zobrazit
  const [currentView, setCurrentView] = useState(() => {
    return localStorage.getItem('nas_current_view') || 'files';
  });

  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false); // Stav pro zobrazení/skrytí mobilního menu

  const handleLogout = async () => {
    try {
      await api.post('/api/auth/logout'); // Zavolá backend endpoint pro odhlášení
      onLogout(); // Vymaže uživatele ze stavového kontextu v App.jsx a hodí ho zpět na login
    } catch (err) {
      console.error("Logout selhal", err);
    }
  };


  useEffect(() => {
    localStorage.setItem('nas_current_view', currentView);
  }, [currentView]); // spustí se při initu komponenty a při každé změně currentView, uloží se akutální hodnota do localStorage


  // Seznam položek v (levém) menu 
  const menuItems = [
    { id: 'files', name: 'Moje soubory', icon: <Folder size={20} />, adminOnly: false },
    { id: 'shared-with-me', name: 'Sdílené se mnou', icon: <Share2 size={20} />, adminOnly: false },
    { id: 'share-manager', name: 'Správa sdílení', icon: <Share2 size={20} />, adminOnly: false },
    { id: 'trash', name: 'Koš', icon: <Trash2 size={20} />, adminOnly: false },
    // Sekce pro ADMINA
    { id: 'admin-stats', name: 'Monitoring HW', icon: <Cpu size={20} />, adminOnly: true },
    { id: 'admin-disks', name: 'Správa disků', icon: <HardDrive size={20} />, adminOnly: true },
    { id: 'admin-db', name: 'Záloha & Import DB', icon: <Database size={20} />, adminOnly: true },
  ];

  return (
    <div className="min-h-screen bg-zinc-950 flex flex-col md:flex-row text-zinc-100 font-sans">
      
      {/* MOBILNÍ LIŠTA (Zobrazí se pouze na telefonech) */}
      <div className="md:hidden flex items-center justify-between p-4 bg-zinc-900 border-b border-zinc-800">
        <div className="flex items-center space-x-2">
          <HardDrive className="text-emerald-500" size={24} />
          <span className="font-bold text-white text-lg tracking-tight">Home NAS</span>
        </div>
        <button onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)} className="text-zinc-400 hover:text-white">
          {isMobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
        </button>
      </div>

      {/* BOČNÍ MENU (SIDEBAR) */}
      <div className={`${isMobileMenuOpen ? 'block' : 'hidden'} md:flex flex-col w-full md:w-64 bg-zinc-900/50 border-r border-zinc-900 backdrop-blur-md p-6 justify-between shrink-0`}>
        <div className="space-y-8">
          {/* Logo sekce (Skrytá na mobilu, protože tam už je - výše) */}
          <div className="hidden md:flex items-center space-x-3 px-2">
            <div className="bg-emerald-500/10 p-2 rounded-lg text-emerald-400">
              <HardDrive size={24} />
            </div>
            <span className="font-extrabold text-white text-xl tracking-tight">Home NAS</span>
          </div>

          {/* POLOŽKY MENU */}
          <nav className="space-y-1">
            {menuItems.map((item) => { // pro každou položku v menu vykreslíme tlačítko; provádí transformaci z pole objektů na pole React elementů
              // Pokud je položka pouze pro admina a přihlášený uživatel ADMIN není, řádek přeskočíme/nevykreslíme
              if (item.adminOnly && user.role !== 'ADMIN') return null;

              const isActive = currentView === item.id; // aktivní položka má jinou barvu pozadí a textu, aby bylo jasné, která sekce je právě otevřená
              
              // React pak tlačítka naskládá do toho "nav" elemu; říká jak má map ten objekt přetransformovat 
              return (
                <button
                    key={item.id}
                    onClick={() => { setCurrentView(item.id); setIsMobileMenuOpen(false); }}
                    className={`w-full flex items-center space-x-3 px-4 py-3 rounded-xl font-medium text-sm border transition-all duration-150 ${
                    isActive 
                        ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' 
                        : 'text-zinc-400 border-transparent hover:bg-zinc-800/50 hover:text-zinc-200'
                    }`}
                >
                    {item.icon}
                    <span>{item.name}</span>
                </button>
                );
            })}
          </nav>
        </div>

        {/* PROFIL UŽIVATELE SPODNÍ ČÁST SIDEBARU */}
        <div className="mt-8 md:mt-0 pt-6 border-t border-zinc-800/60 flex flex-col space-y-4">
          <div className="flex items-center space-x-3 px-2">
            <div className="w-10 h-10 rounded-xl bg-zinc-800 border border-zinc-700 flex items-center justify-center font-bold text-emerald-400 uppercase">
              {user.username.substring(0, 2)}
            </div>
            <div className="flex flex-col min-w-0">
              <span className="text-sm font-semibold text-white truncate">{user.username}</span>
              <span className="text-xs text-zinc-500 font-medium truncate uppercase tracking-wider">{user.role}</span>
            </div>
          </div>
          
          <button onClick={handleLogout} className="w-full flex items-center space-x-3 px-4 py-3 rounded-xl font-semibold text-sm text-red-400 hover:bg-red-500/5 transition duration-150">
            <LogOut size={18} />
            <span>Odhlásit se</span>
          </button>
        </div>
      </div>

      {/* HLAVNÍ PRACOVNÍ PLOCHA (OBSAH) */}
      <main className="flex-1 p-4 md:p-8 overflow-y-auto w-full">
        <div className="w-full">
            {currentView === 'files' && <FileExplorer />}
            {currentView === 'shared-with-me' && <SharedWithMe user={user} />}
            {currentView === 'share-manager' && <ShareManager />}
            {currentView === 'trash' && <TrashBin />} 
            {currentView === 'admin-stats' && <HwMonitoring />} 
            {currentView === 'admin-disks' && <StorageManager />} 
            {currentView === 'admin-db' && <DatabaseBackup />}
        </div>
      </main>

    </div>
  );
}
