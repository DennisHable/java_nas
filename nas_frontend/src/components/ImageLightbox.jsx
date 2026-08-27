import React from 'react';
import { X } from 'lucide-react';

export default function ImageLightbox({ fileId, filename, onClose }) {
  const fileUrl = `/api/nas/files/download/${fileId}`;
  
  // zjistíme, zda se jedná o PDF soubor na základě přípony názvu
  const isPdf = filename.toLowerCase().endsWith('.pdf');

  return (
    <div className="fixed inset-0 bg-black/85 flex items-center justify-center p-4 z-50 backdrop-blur-md animate-fadeIn">
      {/* TLAČÍTKO PRO ZAVŘENÍ */}
      <button 
        onClick={onClose} 
        className="absolute top-6 right-6 p-2.5 text-zinc-400 hover:text-white bg-zinc-900/60 hover:bg-zinc-800/80 border border-zinc-800 rounded-xl transition cursor-pointer z-50 shadow-2xl"
      >
        <X size={20} />
      </button>

      {/* HLAVNÍ KONTEJNER */}
      <div className={`w-full flex flex-col items-center space-y-3 relative select-none ${isPdf ? 'max-w-5xl h-[85vh]' : 'max-w-5xl max-h-[85vh]'}`}>
        
        {isPdf ? (
          /* DYNAMICKÉ ZOBRAZENÍ PDF DOKUMENTU */
          <embed 
            src={fileUrl} 
            type="application/pdf" 
            className="w-full h-full rounded-xl border border-zinc-800/50 shadow-2xl bg-zinc-900"
          />
        ) : (
          /* KLASICKÉ ZOBRAZENÍ OBRÁZKU */
          <img 
            src={fileUrl} 
            alt={filename} 
            className="max-w-full max-h-[80vh] object-contain rounded-xl border border-zinc-800/50 shadow-2xl"
            crossOrigin="use-credentials" 
          />
        )}
        
        <div className="text-zinc-400 text-xs font-semibold tracking-wide bg-zinc-900/40 border border-zinc-800/40 px-4 py-1.5 rounded-full backdrop-blur-sm max-w-xs truncate shrink-0">
          {filename}
        </div>
      </div>
    </div>
  );
}
