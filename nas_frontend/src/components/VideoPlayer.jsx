import React from 'react';
import { X } from 'lucide-react';


function VideoPlayer({ fileId, filename, onClose }) {
  // Sestavíme reálnou absolutní URL adresu pro streamovací endpoint
  // Prohlížeč si při přehrávání sám na pozadí bude posílat hlavičky Range: bytes=...
  const streamUrl = `/api/nas/files/stream/${fileId}`;

  return (
    <div className="fixed inset-0 bg-black/80 flex items-center justify-center p-4 z-50 backdrop-blur-md">
      <div className="bg-zinc-900 border border-zinc-800 p-6 rounded-2xl max-w-3xl w-full flex flex-col space-y-4 shadow-2xl relative animate-fadeIn">
        
        {/* HLAVIČKA PŘEHRÁVAČE */}
        <div className="flex justify-between items-center">
          <h3 className="text-sm font-bold text-zinc-200 truncate pr-4">Přehrávání: {filename}</h3>
          <button onClick={onClose} className="p-1.5 text-zinc-400 hover:text-white bg-zinc-800 hover:bg-zinc-700 rounded-lg transition">
            <X size={16} />
          </button>
        </div>

        {/* SAMOTNÝ VIDEO PŘEHRÁVAČ */}
        <div className="w-full bg-black rounded-xl overflow-hidden aspect-video border border-zinc-800">
          <video 
            src={streamUrl} 
            controls 
            autoPlay
            className="w-full h-full"
            // zpřístupní posílání cookies (JSESSIONID) i uvnitř video streamu,
            // jinak by Spring Security stream zablokoval chybou 401/403
            crossOrigin="use-credentials" 
          >
            Váš prohlížeč nepodporuje přehrávání videí.
          </video>
        </div>

      </div>
    </div>
  );
}


export default VideoPlayer;