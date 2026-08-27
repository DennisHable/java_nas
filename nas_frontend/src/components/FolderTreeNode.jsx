import React, { useState } from 'react';
import { ChevronDown, ChevronRight, Folder } from 'lucide-react';

export default function FolderTreeNode({ node, onSelect, selectedId, disabledId, isParentDisabled = false }) {
  const [isOpen, setIsOpen] = useState(false);

  // Složka je zakázaná na přesun pokud:
  //  Je to ona sama (node.id === disabledId)
  //  její rodič byl zakázán (isParentDisabled === true) -> zákaz se přišel shora (nelze přesunout do podadresáře, pokud je nadřazená zakázaná)
  const isDisabled = node.id === disabledId || isParentDisabled;

  const hasChildren = node.children && node.children.length > 0;
  const isSelected = node.id.toString() === selectedId;

  return (
    <div className="pl-4 font-sans select-none">
      <div className="flex items-center space-x-1 py-1">
        
        {/* ŠIPKA PRO ROZBALENÍ / SBALENÍ PODSLOŽEK */}
        <button
          type="button"
          onClick={() => setIsOpen(!isOpen)}
          className={`p-1 hover:bg-zinc-800 rounded text-zinc-500 hover:text-zinc-300 transition ${!hasChildren ? 'opacity-0 pointer-events-none' : ''}`}
        >
          {isOpen ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
        </button>

        {/* SAMOTNÝ ŘÁDEK SLOŽKY */}
        <button
          type="button"
          disabled={isDisabled} // Pokud je zakázaná, HTML prvek se natvrdo zamkne
          onClick={() => onSelect(node.id)}
          className={`flex items-center space-x-2 px-3 py-1.5 rounded-lg text-xs font-semibold border transition text-left flex-1 cursor-pointer ${
            isDisabled 
              ? 'opacity-20 bg-zinc-950/20 border-transparent text-zinc-600 cursor-not-allowed select-none' // zešedivění
              : isSelected
                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                : 'text-zinc-300 border-transparent hover:bg-zinc-800/40 hover:text-zinc-100'
          }`}
        >
          <Folder size={14} className={isDisabled ? 'text-zinc-700' : isSelected ? 'text-emerald-400' : 'text-amber-500'} />
          <span className="truncate">{node.name}</span>
        </button>
      </div>

      {/* REKURZIVNÍ VYKRESLENÍ PŘI ROZBALENÍ */}
      {isOpen && hasChildren && (
        <div className="border-l border-zinc-800/80 ml-3 mt-0.5">
          {node.children.map(childNode => (
            <FolderTreeNode
              key={childNode.id}
              node={childNode}
              onSelect={onSelect}
              selectedId={selectedId}
              disabledId={disabledId}
              // předáme dál informaci, zda je aktuální uzel zakázaný
              // Pokud ano, celá tato větev stromu směrem dolů bude zakázána
              isParentDisabled={isDisabled} 
            />
          ))}
        </div>
      )}
    </div>
  );
}
