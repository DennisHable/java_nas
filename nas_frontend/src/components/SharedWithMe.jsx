import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { Folder, File, Image, Video, Music, FileText, Download, Share2 } from 'lucide-react';
import VideoPlayer from './VideoPlayer';
import TextEditor from './TextEditor';
import ImageLightbox from './ImageLightbox';

export default function SharedWithMe() {
  const [content, setContent] = useState({ subFolders: [], files: [] });
  const [loading, setLoading] = useState(false);

  // stavy pro aktivní modální okna multimédií a editoru
  const [activeVideo, setActiveVideo] = useState(null);
  const [activeTextFile, setActiveTextFile] = useState(null);
  const [activeImage, setActiveImage] = useState(null);

  const fetchSharedContent = () => {
    setLoading(true);
    api.get('/api/nas/share/shared-with-me')
      .then(res => setContent(res.data))
      .catch(err => console.error("Nelze načíst sdílené soubory", err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchSharedContent();
  }, []);

  // Pomocná funkce pro stažení souboru
  const handleDownload = (fileId) => {
    window.open(`/api/nas/files/download/${fileId}`, '_blank');
  };

  // kliknutí na soubor (funguje pro text, video, audio i fotky)
  const handleFileClick = (file) => {
    const mime = file.contentType ? file.contentType.toLowerCase() : '';
    const name = file.originalName.toLowerCase();
    
    const editableExtensions = ['.cpp', '.java', '.h', '.json', '.yaml', '.sh', '.py'];
    const hasEditableExtension = editableExtensions.some(ext => name.endsWith(ext));

    if (mime.startsWith('video/') || mime.startsWith('audio/')) {
      setActiveVideo({ id: file.id, name: file.originalName });
  } else if (mime.startsWith('image/') || name.endsWith('.pdf')) { 
    // pokud jde o obrázek, otevřeme Lightbox galerii; nebo o pdf 
    setActiveImage({ id: file.id, name: file.originalName });
    } else if (mime.startsWith('text/') || hasEditableExtension) {
      setActiveTextFile({ id: file.id, name: file.originalName });
    } else {
      handleDownload(file.id);
    }
  };

  const getFileIcon = (contentType) => {
    if (!contentType) return <File size={20} className="text-zinc-400" />;
    if (contentType.startsWith('image/')) return <Image size={20} className="text-blue-400" />;
    if (contentType.startsWith('video/')) return <Video size={20} className="text-amber-400" />;
    if (contentType.startsWith('audio/')) return <Music size={20} className="text-purple-400" />;
    if (contentType.startsWith('text/') || contentType.includes('pdf')) return <FileText size={20} className="text-emerald-400" />;
    return <File size={20} className="text-zinc-400" />;
  };

  return (
    <div className="space-y-6">
      <div className="bg-zinc-900 border border-zinc-800 p-4 rounded-xl shadow-md">
        <h1 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
          <Share2 className="text-blue-400" size={22} />
          Soubory sdílené se mnou
        </h1>
      </div>

      <div className="bg-zinc-900/40 border border-zinc-900 rounded-xl overflow-hidden shadow-xl">
        <div className="divide-y divide-zinc-800/40">
          {loading ? (
            <div className="p-8 text-center text-zinc-500 text-sm">Načítám sdílené úložiště...</div>
          ) : content.subFolders.length === 0 && content.files.length === 0 ? (
            <div className="p-8 text-center text-zinc-500 text-sm">Žádný uživatel s vámi aktuálně nic nesdílí.</div>
          ) : (
            <>
              {/* CIZÍ SLOŽKY */}
              {content.subFolders.map(f => (
                <div key={f.id} className="p-4 flex items-center justify-between hover:bg-zinc-800/20">
                  <div className="flex items-center space-x-3">
                    <Folder className="text-blue-400 fill-blue-500/5" size={20} />
                    <div>
                      <span className="text-sm font-semibold text-zinc-200">{f.name}</span>
                      <div className="text-xs text-zinc-500 mt-0.5">Vlastník: <span className="text-zinc-400 font-bold">{f.ownerUsername}</span></div>
                    </div>
                  </div>
                </div>
              ))}

              {/* CIZÍ SOUBORY */}
              {content.files.map(file => (
                <div key={file.id} className="p-4 flex items-center justify-between hover:bg-zinc-800/20 transition duration-150">
                  <div className="flex items-center space-x-3 min-w-0 flex-1">
                    {getFileIcon(file.contentType)}
                    <div className="min-w-0 flex-1">
                      {/* KLIKNUTELNÝ TEXT NÁZVU – CHYTRÉ ROZPOZNÁNÍ OPERACE */}
                      <button 
                        onClick={() => handleFileClick(file)}
                        className="text-sm font-medium text-zinc-300 truncate hover:text-emerald-400 hover:underline text-left block w-full"
                      >
                        {file.originalName}
                      </button>
                      <div className="text-xs text-zinc-500 mt-0.5">Vlastník: <span className="text-zinc-400 font-bold">{file.ownerUsername}</span> • {(file.fileSize / (1024 * 1024)).toFixed(2)} MB</div>
                    </div>
                  </div>
                  
                  {/* TLAČÍTKO PRO PŘÍMÉ STÁHNUTÍ SOUBORU */}
                  <button 
                    onClick={() => handleDownload(file.id)}
                    className="p-2 text-zinc-400 hover:text-emerald-400 hover:bg-emerald-500/5 rounded-lg transition"
                    title="Stáhnout do PC"
                  >
                    <Download size={16} />
                  </button>
                </div>
              ))}
            </>
          )}
        </div>
      </div>

      {/* VYKRRESLENÍ MODÁLNÍCH OKEN PŘI AKTIVACI KLIKNUTÍM */}
      {activeVideo && (
        <VideoPlayer 
          fileId={activeVideo.id} 
          filename={activeVideo.name} 
          onClose={() => setActiveVideo(null)} 
        />
      )}

      {activeTextFile && (
        <TextEditor 
          fileId={activeTextFile.id} 
          filename={activeTextFile.name} 
          onClose={() => setActiveTextFile(null)} 
          onSaveSuccess={() => { fetchSharedContent(); setActiveTextFile(null); }} // Znovu načte seznam pro promítnutí nového času editace
        />
      )}

      {activeImage && (
        <ImageLightbox 
          fileId={activeImage.id} 
          filename={activeImage.name} 
          onClose={() => setActiveImage(null)} 
        />
      )}

    </div>
  );
}
