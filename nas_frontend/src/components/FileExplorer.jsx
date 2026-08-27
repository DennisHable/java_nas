import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import {
  Folder, File, FileText, Image, Video, Music, Upload, Plus, ChevronRight,
  Info, Trash2, Share2, HardDrive, Download, Edit2, Move, MoreVertical,
  CheckSquare, Square, X
} from 'lucide-react';
import VideoPlayer from './VideoPlayer';
import TextEditor from './TextEditor';
import ImageLightbox from './ImageLightbox';
import DetailsModal from './DetailsModal';
import FolderTreeNode from './FolderTreeNode';

export default function FileExplorer() {
  const [folderId, setFolderId] = useState(null); // null == kořenový adresář NASu 
  const [content, setContent] = useState({ subFolders: [], files: [] }); // obsah adresáře 
  const [breadcrumbs, setBreadcrumbs] = useState([{ id: null, name: 'Domů' }]); // interaktivní lišta cesty (navigace)

  const [activeVideo, setActiveVideo] = useState(null);   //  stav pro aktivní přehrávané video
  const [activeTextFile, setActiveTextFile] = useState(null); // stav pro text editor
  const [activeImage, setActiveImage] = useState(null); // fotky
  const [detailsTarget, setDetailsTarget] = useState(null); // zobrazení info o souboru/složce

  const [editTarget, setEditTarget] = useState(null); // pro přejmenování souboru/složky
  const [renameValue, setRenameValue] = useState('');  

  const [moveTarget, setMoveTarget] = useState(null); // pro přesun  
  const [moveFolderId, setMoveFolderId] = useState(''); // ID cílové složky 

  const [isDragActive, setIsDragActive] = useState(false); // zvýraznění přetažení souborů do okna 
  const [folderTree, setFolderTree] = useState([]); // uložení načteného stromu; stromový zobrazení složek pro přesun 

  const [newFolderName, setNewFolderName] = useState(''); // vytvoření adresáře v NASu
  const [isCreatingFolder, setIsCreatingFolder] = useState(false); 

  const [loading, setLoading] = useState(false);

  // sdílení souborů/složek
  const [shareTarget, setShareTarget] = useState(null);
  const [shareWithUser, setShareWithUser] = useState('');
  const [shareCanWrite, setShareCanWrite] = useState(false);

  // multi-select pro hromadné akce
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [selectedFolders, setSelectedFolders] = useState([]);
  const [activeMenuId, setActiveMenuId] = useState(null);

  // načtení kontextu adreáře (buď kořenový, nebo zvolený)
  const fetchContent = () => {
    setLoading(true);
    const url = folderId ? `/api/nas/content?folderId=${folderId}` : '/api/nas/content';

    api.get(url)
      .then(res => setContent(res.data))
      .catch(err => console.error("Nelze načíst obsah složky", err))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    setSelectedFiles([]);
    setSelectedFolders([]);
    setActiveMenuId(null);
    fetchContent(); // okamžitá obnova seznamu na obrazovce 
  }, [folderId]); // přesun do nové složky, nebo zpět do nadřazené složky


  // hromadný výběr pro akce
  const toggleSelectFile = (id) => {
    setSelectedFiles(prev => prev.includes(id) ? prev.filter(fId => fId !== id) : [...prev, id]);
  };

  const toggleSelectFolder = (id) => {
    setSelectedFolders(prev => prev.includes(id) ? prev.filter(fId => fId !== id) : [...prev, id]);
  };


  const totalSelected = selectedFiles.length + selectedFolders.length;
  const isAllSelected = content.files.length > 0 || content.subFolders.length > 0
    ? selectedFiles.length === content.files.length && selectedFolders.length === content.subFolders.length
    : false;

  const handleSelectAll = () => {
    if (isAllSelected) {
      setSelectedFiles([]);
      setSelectedFolders([]);
    } else {
      setSelectedFiles(content.files.map(f => f.id));
      setSelectedFolders(content.subFolders.map(f => f.id));
    }
  };

  const clearSelection = () => {
    setSelectedFiles([]);
    setSelectedFolders([]);
  };

  const handleBatchTrash = async () => {
    if (!window.confirm(`Opravdu chcete přesunout ${totalSelected} vybraných položek do koše?`)) return;

    try {
      setLoading(true);
      const filePromises = selectedFiles.map(id => api.delete(`/api/nas/files/${id}/trash`));
      const folderPromises = selectedFolders.map(id => api.delete(`/api/nas/folders/${id}/trash`));

      await Promise.all([...filePromises, ...folderPromises]);
      clearSelection();
      fetchContent();
    } catch (err) {
      alert("Hromadné mazání selhalo: " + (err.response?.data || err.message));
    } finally {
      setLoading(false);
    }
  };


  // stahování 
  const handleBatchDownload = async () => {
    if (selectedFiles.length === 0) return;

    if (selectedFiles.length === 1) {
      // Při 1 souboru stahujeme přímo původní soubor
      window.open(`/api/nas/files/download/${selectedFiles[0]}`, '_blank');
      } else {
      // Při více souborech stahujeme streamovaný ZIP přes POST a Blob
      try {
        setLoading(true);
        const response = await api.post('/api/nas/files/download-zip', selectedFiles, {
        responseType: 'blob' // říká Axiosu, že chceme zachovat surová binární data
        });
        // vytvoříme dočasný skrytý odkaz v paměti prohlížeče
        const blob = new Blob([response.data], { type: 'application/octet-stream' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');

        link.href = url;
        link.setAttribute('download', 'files.zip'); // název stahovaného souboru
        document.body.appendChild(link);
        link.click(); // simulujeme kliknutí pro spuštění stahování

        // Vyčištění paměti prohlížeče
        link.parentNode.removeChild(link);
        window.URL.revokeObjectURL(url);

      } catch (err) {
        console.error("Stahování ZIP archivu selhalo", err);
        alert("Nepodařilo se vygenerovat a stáhnout ZIP archiv.");
      } finally {
        setLoading(false);
      }
    }

  };

  // přepočet velikosti souboru
  const formatFileSize = (bytes) => {
    if (!bytes || bytes === 0) return '0.00 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`;
  };

  const handleBatchMoveSubmit = async (e) => {
    e.preventDefault();
    if (!moveFolderId) return;

    try {
      setLoading(true);
      const targetId = moveFolderId.toLowerCase() === 'root' ? null : moveFolderId;

      const filePromises = selectedFiles.map(fileId => {
        const params = new URLSearchParams();
        if (targetId) params.append('targetFolderId', targetId);
        return api.patch(`/api/nas/files/${fileId}/move`, params);
      });

      const folderPromises = selectedFolders.map(fId => {
        const params = new URLSearchParams();
        if (targetId) params.append('targetParentId', targetId);
        return api.patch(`/api/nas/folders/${fId}/move`, params);
      });

      await Promise.all([...filePromises, ...folderPromises]);

      setMoveTarget(null);
      setMoveFolderId('');
      clearSelection();
      fetchContent();
    } catch (err) {
      alert("Hromadný přesun selhal: " + (err.response?.data || err.message));
    } finally {
      setLoading(false);
    }
  };

  const handleRenameSubmit = async (e) => {
    e.preventDefault();
    if (!renameValue.trim()) return;

    const isFile = editTarget.type === 'file';
    const url = isFile ? `/api/nas/files/${editTarget.id}/rename` : `/api/nas/folders/${editTarget.id}/rename`;

    try {
      const params = new URLSearchParams();
      params.append('newName', renameValue);

      await api.patch(url, params);
      setEditTarget(null);
      setRenameValue('');
      fetchContent();
    } catch (err) {
      alert("Přejmenování selhalo: " + (err.response?.data || err.message));
    }
  };

  const handleMoveSubmit = async (e) => {
    if (moveTarget?.type === 'batch') {
      return handleBatchMoveSubmit(e);
    }

    e.preventDefault();
    const isFile = moveTarget.type === 'file';
    const url = isFile ? `/api/nas/files/${moveTarget.id}/move` : `/api/nas/folders/${moveTarget.id}/move`;

    try {
      const params = new URLSearchParams();
      if (moveFolderId && moveFolderId.toLowerCase() !== 'root') {
        params.append(isFile ? 'targetFolderId' : 'targetParentId', moveFolderId);
      }

      await api.patch(url, params);
      setMoveTarget(null);
      setMoveFolderId('');
      fetchContent();
    } catch (err) {
      alert("Přesun selhal: " + (err.response?.data || err.message));
    }
  };

  const handleOpenMoveDialog = (type, id) => {
    setMoveTarget({ type, id });
    setMoveFolderId('');
    api.get('/api/nas/folders/tree')
      .then(res => setFolderTree(res.data))
      .catch(err => console.error("Nelze načíst strom složek", err));
  };

  const handleCreateFolder = async (e) => {
    e.preventDefault();
    if (!newFolderName.trim()) return;

    try {
      const params = new URLSearchParams();
      params.append('name', newFolderName);
      if (folderId) params.append('parentId', folderId);

      await api.post('/api/nas/folders/create', params);
      setNewFolderName('');
      setIsCreatingFolder(false);
      fetchContent();
    } catch (err) {
      alert("Chyba při vytváření složky: " + (err.response?.data || err.message));
    }
  };

  const handleMultipleFileUpload = async (filesList) => {
    if (!filesList || filesList.length === 0) return;

    const formData = new FormData();
    Array.from(filesList).forEach(file => {
      formData.append('file', file);
    });

    if (folderId) formData.append('folderId', folderId);

    try {
      setLoading(true);
      await api.post('/api/nas/files/upload/multi', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      fetchContent();
    } catch (err) {
      alert("Nahrávání selhalo: " + (err.response?.data || err.message));
    } finally {
      setLoading(false);
    }
  };

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setIsDragActive(true);
    } else if (e.type === "dragleave") {
      setIsDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragActive(false);

    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
      handleMultipleFileUpload(files);
    }
  };

  const handleMoveToTrash = async (fileId) => {
    try {
      await api.delete(`/api/nas/files/${fileId}/trash`);
      fetchContent();
    } catch (err) {
      alert("Nelze smazat: " + (err.response?.data || err.message));
    }
  };

  const handleFolderToTrash = async (fId) => {
    try {
      await api.delete(`/api/nas/folders/${fId}/trash`);
      fetchContent();
    } catch (err) {
      alert("Složku se nepodařilo přesunout do koše: " + (err.response?.data || err.message));
    }
  };

  const handleShareSubmit = async (e) => {
    e.preventDefault();
    try {
      await api.post('/api/nas/share', {
        shareWithUsername: shareWithUser,
        fileId: shareTarget.type === 'file' ? shareTarget.id : null,
        folderId: shareTarget.type === 'folder' ? shareTarget.id : null,
        canWrite: shareCanWrite
      });
      alert("Práva byla úspěšně nasdílena!");
      setShareTarget(null);
      setShareWithUser('');
      setShareCanWrite(false);
    } catch (err) {
      alert("Sdílení selhalo: " + (err.response?.data || err.message));
    }
  };

  const handleDownload = (fileId) => {
    window.open(`/api/nas/files/download/${fileId}`, '_blank');
  };

  const handleFolderClick = (folder) => {
    setBreadcrumbs([...breadcrumbs, { id: folder.id, name: folder.name }]);
    setFolderId(folder.id);
  };

  const handleBreadcrumbClick = (crumb, index) => {
    setBreadcrumbs(breadcrumbs.slice(0, index + 1));
    setFolderId(crumb.id);
  };

  const getFileIcon = (contentType) => {
    if (!contentType) return <File size={18} className="text-zinc-400 shrink-0" />;
    if (contentType.startsWith('image/')) return <Image size={18} className="text-blue-400 shrink-0" />;
    if (contentType.startsWith('video/')) return <Video size={18} className="text-amber-400 shrink-0" />;
    if (contentType.startsWith('audio/')) return <Music size={18} className="text-purple-400 shrink-0" />;
    if (contentType.startsWith('text/') || contentType.includes('pdf')) return <FileText size={18} className="text-emerald-400 shrink-0" />;
    return <File size={18} className="text-zinc-400 shrink-0" />;
  };

  return (
    <div className="space-y-4 sm:space-y-6 relative pb-20">

      {/* NAVIGACE (cesta) */}
      <div className="flex items-center space-x-1 text-xs sm:text-sm bg-zinc-900/40 border border-zinc-800/80 px-3 sm:px-4 py-2.5 rounded-xl text-zinc-400 overflow-x-auto">
        {breadcrumbs.map((crumb, index) => {
          const isLast = index === breadcrumbs.length - 1;
          return (
            <div key={crumb.id || 'root'} className="flex items-center space-x-1 shrink-0">
              {index > 0 && <ChevronRight size={14} className="text-zinc-600" />}
              <button
                onClick={() => handleBreadcrumbClick(crumb, index)}
                disabled={isLast}
                className={`font-semibold hover:underline px-1 py-0.5 rounded transition ${
                  isLast ? 'text-white font-bold cursor-default' : 'text-zinc-400 hover:text-zinc-200'
                }`}
              >
                {crumb.name}
              </button>
            </div>
          );
        })}
      </div>

      {/* LIŠTA TLAČÍTEK */}
      <div className="flex flex-col sm:flex-row justify-between items-stretch sm:items-center gap-3 bg-zinc-900 border border-zinc-800 p-3 sm:p-4 rounded-xl shadow-md">
        <h2 className="text-base sm:text-lg font-bold text-white tracking-tight truncate px-1">
          {breadcrumbs[breadcrumbs.length - 1].name}
        </h2>

        <div className="flex items-center space-x-2">
          <label className="flex-1 sm:flex-none flex items-center justify-center space-x-2 px-3.5 py-2 bg-emerald-500 hover:bg-emerald-600 text-white rounded-lg text-xs sm:text-sm font-bold cursor-pointer transition duration-150 shadow-lg shadow-emerald-500/10">
            <Upload size={16} />
            <span>Nahrát</span>
            <input
              type="file"
              multiple
              onChange={(e) => handleMultipleFileUpload(e.target.files)}
              className="hidden"
            />
          </label>

          <button onClick={() => setIsCreatingFolder(!isCreatingFolder)} className="flex-1 sm:flex-none flex items-center justify-center space-x-2 px-3.5 py-2 bg-zinc-800 hover:bg-zinc-700 text-zinc-200 border border-zinc-700 rounded-lg text-xs sm:text-sm font-bold transition duration-150">
            <Plus size={16} />
            <span>Nová složka</span>
          </button>
        </div>
      </div>

      {/* FORMULÁŘ SLOŽKY */}
      {isCreatingFolder && (
        <form onSubmit={handleCreateFolder} className="flex gap-2 bg-zinc-900 border border-zinc-800 p-3 sm:p-4 rounded-xl max-w-md animate-fadeIn">
          <input
            type="text"
            required
            value={newFolderName}
            onChange={e => setNewFolderName(e.target.value)}
            placeholder="Název nové složky"
            className="flex-1 bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-xs sm:text-sm text-white focus:outline-none focus:border-emerald-500"
          />
          <button type="submit" className="px-3 sm:px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white text-xs sm:text-sm font-bold rounded-lg transition">
            Vytvořit
          </button>
          <button type="button" onClick={() => setIsCreatingFolder(false)} className="px-3 py-2 bg-zinc-800 text-zinc-400 text-xs sm:text-sm rounded-lg hover:text-white">
            Zrušit
          </button>
        </form>
      )}

      {/* MULTISELECT LIŠTA */}
      {totalSelected > 0 && (
        <div className="sticky top-4 z-30 bg-emerald-950/90 border border-emerald-500/40 backdrop-blur-md p-3 rounded-xl shadow-2xl flex flex-wrap items-center justify-between gap-3 animate-fadeIn">
          <div className="flex items-center space-x-2 text-xs sm:text-sm font-bold text-emerald-300 px-1">
            <CheckSquare size={16} className="text-emerald-400" />
            <span>Vybráno položek: {totalSelected}</span>
          </div>

          <div className="flex items-center space-x-1.5 sm:space-x-2">
            {selectedFiles.length > 0 && (
              <button
                onClick={handleBatchDownload}
                className="flex items-center space-x-1 px-2.5 sm:px-3 py-1.5 bg-zinc-900/80 hover:bg-zinc-800 text-zinc-200 border border-emerald-500/30 rounded-lg text-xs font-semibold transition cursor-pointer"
                title={selectedFiles.length > 1 ? "Stáhnout jako ZIP archiv" : "Stáhnout soubor"}
              >
                <Download size={14} className="text-emerald-400" />
                <span className="hidden sm:inline">
                  {selectedFiles.length > 1 ? 'Stáhnout ZIP' : 'Stáhnout'}
                </span>
              </button>
            )}

            <button
              onClick={() => handleOpenMoveDialog('batch', null)}
              className="flex items-center space-x-1 px-2.5 sm:px-3 py-1.5 bg-zinc-900/80 hover:bg-zinc-800 text-zinc-200 border border-emerald-500/30 rounded-lg text-xs font-semibold transition"
            >
              <Move size={14} className="text-blue-400" />
              <span>Přesunout</span>
            </button>

            <button
              onClick={handleBatchTrash}
              className="flex items-center space-x-1 px-2.5 sm:px-3 py-1.5 bg-red-950/60 hover:bg-red-900/80 text-red-200 border border-red-500/40 rounded-lg text-xs font-semibold transition"
            >
              <Trash2 size={14} className="text-red-400" />
              <span>Do koše</span>
            </button>

            <button
              onClick={clearSelection}
              className="p-1.5 text-zinc-400 hover:text-white rounded-lg transition"
            >
              <X size={16} />
            </button>
          </div>
        </div>
      )}

      {/* DRAG & DROP ZÓNA */}
      <div
        onDragEnter={handleDrag}
        onDragOver={handleDrag}
        onDragLeave={handleDrag}
        onDrop={handleDrop}
        className={`relative rounded-2xl transition-all duration-200 border-2 border-dashed ${
          isDragActive ? 'border-emerald-500 bg-emerald-500/5 scale-[0.99] p-1' : 'border-transparent bg-transparent'
        }`}
      >
        {isDragActive && (
          <div className="absolute inset-0 bg-zinc-950/40 backdrop-blur-sm rounded-2xl flex items-center justify-center z-40 pointer-events-none">
            <div className="text-center text-emerald-400 font-bold text-xs sm:text-sm tracking-wide uppercase bg-zinc-900 border border-emerald-500/20 px-6 py-4 rounded-xl shadow-2xl animate-pulse">
              Pusťte tlačítko myši pro nahrání souborů
            </div>
          </div>
        )}

        {loading ? (
          <div className="text-center py-12 text-zinc-500 font-medium text-xs sm:text-sm">Načítám úložiště...</div>
        ) : content.subFolders.length === 0 && content.files.length === 0 ? (
          <div className="text-center py-16 sm:py-20 bg-zinc-900/20 border border-zinc-900 border-dashed rounded-2xl text-zinc-500">
            <Folder size={40} className="mx-auto mb-3 text-zinc-700" />
            <p className="text-xs sm:text-sm font-medium">Tato složka je prázdná</p>
          </div>
        ) : (
          /* BEZ OVERFLOW-HIDDEN - ABY SE DROPDOWN NEOŘEZÁVAL (mobilní prohlížeče) */
          <div className="bg-zinc-900/40 border border-zinc-900 rounded-xl shadow-xl">

            {/* HLAVIČKA */}
            <div className="flex items-center justify-between px-3 sm:px-4 py-2.5 bg-zinc-900/80 border-b border-zinc-800/80 text-[11px] uppercase tracking-wider font-bold text-zinc-400 select-none rounded-t-xl">
              <div className="flex items-center space-x-3">
                <button
                  onClick={handleSelectAll}
                  className="p-1 text-zinc-400 hover:text-emerald-400 transition"
                >
                  {isAllSelected ? <CheckSquare size={16} className="text-emerald-500" /> : <Square size={16} />}
                </button>
                <span>Název</span>
              </div>
              <span className="hidden sm:inline">Vlastník / Akce</span>
            </div>

            <div className="divide-y divide-zinc-800/40">

              {/* SLOŽKY */}
              {content.subFolders.map(folder => {
                const isSelected = selectedFolders.includes(folder.id);
                const isMenuOpen = activeMenuId?.type === 'folder' && activeMenuId?.id === folder.id;

                return (
                  <div
                    key={folder.id}
                    className={`flex items-center justify-between p-3 sm:p-4 transition duration-150 relative ${
                      isSelected ? 'bg-emerald-500/10' : 'hover:bg-zinc-800/20'
                    }`}
                  >
                    <div className="flex items-center space-x-3 flex-1 min-w-0 pr-2">
                      <button onClick={() => toggleSelectFolder(folder.id)} className="p-1 text-zinc-500 hover:text-emerald-400 transition shrink-0">
                        {isSelected ? <CheckSquare size={16} className="text-emerald-500" /> : <Square size={16} />}
                      </button>

                      <button onClick={() => handleFolderClick(folder)} className="flex items-center space-x-2.5 text-left flex-1 min-w-0 group">
                        <Folder size={20} className="text-amber-500 fill-amber-500/5 group-hover:scale-105 transition-transform shrink-0" />
                        <span className="text-xs sm:text-sm font-semibold text-zinc-200 group-hover:text-white truncate">
                          {folder.name}
                        </span>
                      </button>
                    </div>

                    {/* ZAROVNÁNÍ DOPRAVA */}
                    <div className="flex items-center gap-3 shrink-0 ml-auto justify-end">
                      {/* prvek pro zachování stejného sloupce jako u souborů */}
                        <span className="w-14"></span>

                        {/* Vlastník */}
                        <span className="w-14 hidden sm:inline text-right text-xs text-zinc-500 font-medium truncate">
                          {folder.ownerUsername}
                        </span>

                        {/* Oddělovač */}
                        <div className="hidden sm:block h-4 w-px bg-zinc-700/60" />

                      {/* DESKTOP AKCE */}
                      <div className="w-44 hidden sm:flex items-center justify-end space-x-1">
                        <button onClick={() => { setEditTarget({ type: 'folder', id: folder.id, name: folder.name }); setRenameValue(folder.name); }} className="p-1.5 text-zinc-400 hover:text-emerald-400 rounded-lg transition" title="Přejmenovat"><Edit2 size={15} /></button>
                        <button onClick={() => handleOpenMoveDialog('folder', folder.id)} className="p-1.5 text-zinc-400 hover:text-blue-400 rounded-lg transition" title="Přesunout"><Move size={15} /></button>
                        <button onClick={() => setShareTarget({ type: 'folder', id: folder.id })} className="p-1.5 text-zinc-400 hover:text-blue-400 rounded-lg transition" title="Nasdílet"><Share2 size={15} /></button>
                        <button onClick={() => setDetailsTarget({ type: 'folder', item: folder })} className="p-1.5 text-zinc-400 hover:text-emerald-400 rounded-lg transition" title="Podrobnosti"><Info size={15} /></button>
                        <button onClick={() => handleFolderToTrash(folder.id)} className="p-1.5 text-zinc-400 hover:text-red-400 rounded-lg transition" title="Smazat do koše"><Trash2 size={15} /></button>
                      </div>

                      {/* MOBILNÍ MENU - S Z-INDEX 50 ABY NEBYLO OŘÍZNUTO */}
                      <div className="w-7 sm:hidden relative">
                        <button
                          onClick={() => setActiveMenuId(isMenuOpen ? null : { type: 'folder', id: folder.id })}
                          className="p-1.5 text-zinc-400 hover:text-white rounded-lg bg-zinc-800/40"
                        >
                          <MoreVertical size={16} />
                        </button>

                        {isMenuOpen && (
                          <div className="absolute right-0 top-8 z-50 bg-zinc-900 border border-zinc-800 rounded-xl shadow-2xl py-1 text-xs w-44 divide-y divide-zinc-800/60 animate-fadeIn">
                            <div className="py-1">
                              <button onClick={() => { setEditTarget({ type: 'folder', id: folder.id, name: folder.name }); setRenameValue(folder.name); setActiveMenuId(null); }} className="w-full text-left px-3 py-2 hover:bg-zinc-800 text-zinc-200 flex items-center space-x-2">
                                <Edit2 size={14} className="text-emerald-400" />
                                <span>Přejmenovat</span>
                              </button>
                              <button onClick={() => { handleOpenMoveDialog('folder', folder.id); setActiveMenuId(null); }} className="w-full text-left px-3 py-2 hover:bg-zinc-800 text-zinc-200 flex items-center space-x-2">
                                <Move size={14} className="text-blue-400" />
                                <span>Přesunout</span>
                              </button>
                              <button onClick={() => { setShareTarget({ type: 'folder', id: folder.id }); setActiveMenuId(null); }} className="w-full text-left px-3 py-2 hover:bg-zinc-800 text-zinc-200 flex items-center space-x-2">
                                <Share2 size={14} className="text-blue-400" />
                                <span>Nasdílet</span>
                              </button>
                              <button onClick={() => { setDetailsTarget({ type: 'folder', item: folder }); setActiveMenuId(null); }} className="w-full text-left px-3 py-2 hover:bg-zinc-800 text-zinc-200 flex items-center space-x-2">
                                <Info size={14} className="text-emerald-400" />
                                <span>Podrobnosti</span>
                              </button>
                            </div>
                            <div className="py-1">
                              <button onClick={() => { handleFolderToTrash(folder.id); setActiveMenuId(null); }} className="w-full text-left px-3 py-2 hover:bg-zinc-800 text-red-400 flex items-center space-x-2">
                                <Trash2 size={14} />
                                <span>Do koše</span>
                              </button>
                            </div>
                          </div>
                        )}
                      </div>

                    </div>
                  </div>
                );
              })}

              {/* SOUBORY */}
              {content.files.map(file => {
                const isSelected = selectedFiles.includes(file.id);
                const isMenuOpen = activeMenuId?.type === 'file' && activeMenuId?.id === file.id;

                return (
                  <div
                    key={file.id}
                    className={`flex items-center justify-between p-3 sm:p-4 transition duration-150 relative ${
                      isSelected ? 'bg-emerald-500/10' : 'hover:bg-zinc-800/20'
                    }`}
                  >
                    <div className="flex items-center space-x-3 flex-1 min-w-0 pr-2">
                      <button onClick={() => toggleSelectFile(file.id)} className="p-1 text-zinc-500 hover:text-emerald-400 transition shrink-0">
                        {isSelected ? <CheckSquare size={16} className="text-emerald-500" /> : <Square size={16} />}
                      </button>

                      {getFileIcon(file.contentType)}

                      <button
                        onClick={() => {
                          const mime = file.contentType ? file.contentType.toLowerCase() : '';
                          const name = file.originalName.toLowerCase();
                          const editableExtensions = ['.cpp', '.java', '.h', '.json', '.yaml', '.sh', '.py'];
                          const hasEditableExtension = editableExtensions.some(ext => name.endsWith(ext));

                          if (mime.startsWith('video/') || mime.startsWith('audio/')) {
                            setActiveVideo({ id: file.id, name: file.originalName });
                          } else if (mime.startsWith('text/') || hasEditableExtension) {
                            setActiveTextFile({ id: file.id, name: file.originalName });
                          } else if (mime.startsWith('image/') || name.endsWith('.pdf')) {
                            setActiveImage({ id: file.id, name: file.originalName });
                          } else {
                            handleDownload(file.id);
                          }
                        }}
                        className="text-xs sm:text-sm font-medium text-zinc-300 truncate hover:text-emerald-400 hover:underline text-left"
                      >
                        {file.originalName}
                      </button>
                    </div>

                    {/* ZAROVNÁNÍ DOPRAVA  */}
                    <div className="flex items-center gap-3 shrink-0 ml-auto justify-end">
                      {/* Velikost  */}
                        <span className="w-20 text-right text-[11px] sm:text-xs text-zinc-500 font-mono">
                          {formatFileSize(file.fileSize)}
                        </span>

                        {/* Vlastník */}
                        <span className="w-14 hidden sm:inline text-right text-xs text-zinc-500 font-medium truncate">
                          {file.ownerUsername}
                        </span>

                      {/* Oddělovač */}
                      <div className="hidden sm:block h-4 w-px bg-zinc-700/60" />

                      {/* DESKTOP AKCE */}
                      <div className="w-44 hidden sm:flex items-center justify-end space-x-1">
                        <button onClick={() => handleDownload(file.id)} className="p-1.5 text-zinc-400 hover:text-emerald-400 rounded-lg transition" title="Stáhnout"><Download size={15} /></button>
                        <button onClick={() => { setEditTarget({ type: 'file', id: file.id, name: file.originalName }); setRenameValue(file.originalName); }} className="p-1.5 text-zinc-400 hover:text-emerald-400 rounded-lg transition" title="Přejmenovat"><Edit2 size={15} /></button>
                        <button onClick={() => handleOpenMoveDialog('file', file.id)} className="p-1.5 text-zinc-400 hover:text-blue-400 rounded-lg transition" title="Přesunout"><Move size={15} /></button>
                        <button onClick={() => setShareTarget({ type: 'file', id: file.id })} className="p-1.5 text-zinc-400 hover:text-blue-400 rounded-lg transition" title="Nasdílet"><Share2 size={15} /></button>
                        <button onClick={() => setDetailsTarget({ type: 'file', item: file })} className="p-1.5 text-zinc-400 hover:text-emerald-400 rounded-lg transition" title="Podrobnosti"><Info size={15} /></button>
                        <button onClick={() => handleMoveToTrash(file.id)} className="p-1.5 text-zinc-400 hover:text-red-400 rounded-lg transition" title="Do koše"><Trash2 size={15} /></button>
                      </div>

                      {/* MOBILNÍ MENU S Z-INDEX 50 ABY NEBYLO OŘÍZNUTO */}
                      <div className="sm:hidden relative">
                        <button
                          onClick={() => setActiveMenuId(isMenuOpen ? null : { type: 'file', id: file.id })}
                          className="p-1.5 text-zinc-400 hover:text-white rounded-lg bg-zinc-800/40"
                        >
                          <MoreVertical size={16} />
                        </button>

                        {isMenuOpen && (
                          <div className="absolute right-0 top-8 z-50 bg-zinc-900 border border-zinc-800 rounded-xl shadow-2xl py-1 text-xs w-44 divide-y divide-zinc-800/60 animate-fadeIn">
                            <div className="py-1">
                              <button onClick={() => { handleDownload(file.id); setActiveMenuId(null); }} className="w-full text-left px-3 py-2 hover:bg-zinc-800 text-zinc-200 flex items-center space-x-2">
                                <Download size={14} className="text-emerald-400" />
                                <span>Stáhnout</span>
                              </button>
                              <button onClick={() => { setEditTarget({ type: 'file', id: file.id, name: file.originalName }); setRenameValue(file.originalName); setActiveMenuId(null); }} className="w-full text-left px-3 py-2 hover:bg-zinc-800 text-zinc-200 flex items-center space-x-2">
                                <Edit2 size={14} className="text-emerald-400" />
                                <span>Přejmenovat</span>
                              </button>
                              <button onClick={() => { handleOpenMoveDialog('file', file.id); setActiveMenuId(null); }} className="w-full text-left px-3 py-2 hover:bg-zinc-800 text-zinc-200 flex items-center space-x-2">
                                <Move size={14} className="text-blue-400" />
                                <span>Přesunout</span>
                              </button>
                              <button onClick={() => { setShareTarget({ type: 'file', id: file.id }); setActiveMenuId(null); }} className="w-full text-left px-3 py-2 hover:bg-zinc-800 text-zinc-200 flex items-center space-x-2">
                                <Share2 size={14} className="text-blue-400" />
                                <span>Nasdílet</span>
                              </button>
                              <button onClick={() => { setDetailsTarget({ type: 'file', item: file }); setActiveMenuId(null); }} className="w-full text-left px-3 py-2 hover:bg-zinc-800 text-zinc-200 flex items-center space-x-2">
                                <Info size={14} className="text-emerald-400" />
                                <span>Podrobnosti</span>
                              </button>
                            </div>
                            <div className="py-1">
                              <button onClick={() => { handleMoveToTrash(file.id); setActiveMenuId(null); }} className="w-full text-left px-3 py-2 hover:bg-zinc-800 text-red-400 flex items-center space-x-2">
                                <Trash2 size={14} />
                                <span>Do koše</span>
                              </button>
                            </div>
                          </div>
                        )}
                      </div>

                    </div>
                  </div>
                );
              })}

            </div>
          </div>
        )}
      </div>

      {/* MODÁLY A FORMULÁŘE */}
      {shareTarget && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center p-4 z-50 backdrop-blur-sm">
          <form onSubmit={handleShareSubmit} className="bg-zinc-900 border border-zinc-800 p-6 rounded-2xl max-w-sm w-full space-y-4 shadow-2xl">
            <h3 className="text-md font-bold text-white">Nasdílet {shareTarget.type === 'file' ? 'soubor' : 'složku'}</h3>
            <input type="text" required value={shareWithUser} onChange={e => setShareWithUser(e.target.value)} placeholder="Uživatelské jméno kolegy"
              className="w-full bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500" />
            <label className="flex items-center space-x-2 text-sm text-zinc-400 cursor-pointer">
              <input type="checkbox" checked={shareCanWrite} onChange={e => setShareCanWrite(e.target.checked)} className="rounded bg-zinc-950 border-zinc-800 text-emerald-500 accent-emerald-500 focus:ring-0" />
              <span>Povolit uživateli zápis (WRITE)</span>
            </label>
            <div className="flex justify-end space-x-2 pt-2">
              <button type="button" onClick={() => setShareTarget(null)} className="px-4 py-2 bg-zinc-800 text-zinc-400 text-sm font-semibold rounded-lg hover:text-white transition">Zrušit</button>
              <button type="submit" className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white text-sm font-bold rounded-lg transition">Nasdílet</button>
            </div>
          </form>
        </div>
      )}

      {editTarget && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center p-4 z-50 backdrop-blur-sm">
          <form onSubmit={handleRenameSubmit} className="bg-zinc-900 border border-zinc-800 p-6 rounded-2xl max-w-sm w-full space-y-4 shadow-2xl">
            <h3 className="text-sm font-bold text-white">Přejmenovat {editTarget.type === 'file' ? 'soubor' : 'složku'}</h3>
            <input type="text" required value={renameValue} onChange={e => setRenameValue(e.target.value)}
              className="w-full bg-zinc-950 border border-zinc-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-emerald-500 font-medium" />
            <div className="flex justify-end space-x-2 pt-1">
              <button type="button" onClick={() => setEditTarget(null)} className="px-4 py-2 bg-zinc-800 text-zinc-400 text-xs font-bold rounded-lg hover:text-white transition">Zrušit</button>
              <button type="submit" className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 text-white text-xs font-bold rounded-lg transition">Přejmenovat</button>
            </div>
          </form>
        </div>
      )}

      {moveTarget && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center p-4 z-50 backdrop-blur-sm">
          <div className="bg-zinc-900 border border-zinc-800 p-6 rounded-2xl max-w-sm w-full space-y-4 shadow-2xl flex flex-col max-h-[80vh]">
            <div>
              <h3 className="text-sm font-bold text-white">
                {moveTarget.type === 'batch' ? `Přesunout ${totalSelected} vybraných položek` : `Přesunout ${moveTarget.type === 'file' ? 'soubor' : 'složku'}`}
              </h3>
              <p className="text-xs text-zinc-500 leading-normal mt-1">Vyberte cílovou složku ze stromové struktury níže.</p>
            </div>

            <div className="flex-1 bg-zinc-950 border border-zinc-800/80 rounded-xl p-3 overflow-y-auto min-h-[200px] space-y-1">
              <button
                type="button"
                onClick={() => setMoveFolderId('root')}
                className={`w-full flex items-center space-x-2 px-3 py-2 rounded-lg text-xs font-bold border transition text-left cursor-pointer mb-2 ${
                  moveFolderId === 'root' ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' : 'text-zinc-400 border-transparent hover:bg-zinc-800/30 hover:text-zinc-200'
                }`}
              >
                <HardDrive size={14} />
                <span>Základní kořenový adresář (Root)</span>
              </button>

              {folderTree.length === 0 ? (
                <div className="text-[11px] text-zinc-600 text-center py-6 uppercase font-mono tracking-wider">Nemáte žádné jiné složky</div>
              ) : (
                folderTree.map(node => (
                  <FolderTreeNode
                    key={node.id}
                    node={node}
                    onSelect={(id) => setMoveFolderId(String(id))}
                    selectedId={moveFolderId}
                    disabledId={moveTarget.type === 'folder' ? moveTarget.id : null}
                  />
                ))
              )}
            </div>

            <div className="flex justify-end space-x-2 pt-1 shrink-0">
              <button type="button" onClick={() => setMoveTarget(null)} className="px-4 py-2 bg-zinc-800 text-zinc-400 text-xs font-bold rounded-lg hover:text-white transition cursor-pointer">
                Zrušit
              </button>
              <button
                type="button"
                disabled={!moveFolderId}
                onClick={handleMoveSubmit}
                className="px-4 py-2 bg-emerald-500 hover:bg-emerald-600 disabled:bg-zinc-800 disabled:text-zinc-600 text-white text-xs font-bold rounded-lg transition shadow-lg shadow-emerald-500/5 cursor-pointer"
              >
                Přesunout
              </button>
            </div>
          </div>
        </div>
      )}

      {activeVideo && <VideoPlayer fileId={activeVideo.id} filename={activeVideo.name} onClose={() => setActiveVideo(null)} />}
      {activeTextFile && <TextEditor fileId={activeTextFile.id} filename={activeTextFile.name} onClose={() => setActiveTextFile(null)} onSaveSuccess={() => { fetchContent(); setActiveTextFile(null); }} />}
      {activeImage && <ImageLightbox fileId={activeImage.id} filename={activeImage.name} onClose={() => setActiveImage(null)} />}
      {detailsTarget && <DetailsModal type={detailsTarget.type} item={detailsTarget.item} onClose={() => setDetailsTarget(null)} />}

    </div>
  );
}
