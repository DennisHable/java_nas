import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { ShieldAlert, Lock, User, Mail, Database } from 'lucide-react';

export default function Login({ onLoginSuccess }) { // komponenta drží data, vrací HTML 
  const [isFirstRun, setIsFirstRun] = useState(false);
  const [isRegister, setIsRegister] = useState(false);
  const [rememberMe, setRememberMe] = useState(false); // dlouhodobé přihlášení uživatele

  // kvůli probliku při initu serveru (přihlášení a první init)
  const [checkingInit, setCheckingInit] = useState(true);

  const [username, setUsername] = useState('');  // stav ("privátní proměnné")
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');

  // Okamžitě po načtení stránky zjistíme stav systému z backendu
  useEffect(() => { // když se tahle stránka objeví na obrazovce uživ. tak to zavolej
    api.get('/api/auth/init-status') // HTTP požadavek na backend; asynchronně, JS hned pokračuje dál  
      .then(res => { // až dostane někdy odpoveď z backendu, tak vleze sem; odpoveď = res; kódy od 200-299
        if (res.data === true) {
          setIsFirstRun(true); // Nouzový režim pro admina – DB je prázdná, resp. tabulka uživatel
        } // else { // řeší App.js
          // onLoginSuccess(res.data); // Uživatel už je přihlášený, pustíme ho dál; bere data uživ. z DB do glob. stavu apky
        // }
      })
      .catch(() => { // "vyjímky", kódy 4xx a 5xx; chyba sítě, backend neběží, ...
        setError('Nelze se spojit se serverem NAS.')
      })
      .finally(() => {
        // máme odpoveď, vypneme načítání
        setCheckingInit(false);
      });
  }, []); // "[]" => spustí se jen jednou po načtení stránky

  const handleSubmit = async (e) => { // asynchronní funkce, protože čekáme na odpověď z backendu
    e.preventDefault(); // vypnutí reloadu stánky 
    setError('');

    try {
      if (isFirstRun) {
        // Registrujeme prvního uživatele (role ADMIN)
        await api.post('/api/auth/register', { username, password, email, role: 'ADMIN' }); // dokud backend neodpoví, tak se nepokračuje dálším kódem
        // po úspěšné registraci admina ho rovnou přihlásíme
        const loginRes = await api.post('/api/auth/login', { username, password });
        onLoginSuccess(loginRes.data);
      } else if (isRegister) {
        // registrace normálního uživatele (role USER)
        await api.post('/api/auth/register', { username, password, email, role: 'USER' });
        setIsRegister(false); // přepneme na login formulář
        setPassword('');
      } else {
        // standardní přihlášení uživatele (role ADMIN nebo USER)
        const loginRes = await api.post('/api/auth/login', { username, password, rememberMe });
        onLoginSuccess(loginRes.data);
      }
    } catch (err) {
      setError(err.response?.data || 'Došlo k neočekávané chybě.');
    }
  };

  if (checkingInit) {
    return <div className="min-h-screen bg-zinc-950"></div>;
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-zinc-950 p-4">
      <div className="w-full max-w-md bg-zinc-900 border border-zinc-800 rounded-xl p-8 shadow-2xl">
        
        {/* HLAVIČKA FORMULÁŘE */}
        <div className="flex flex-col items-center mb-8">
          <div className={`p-3 rounded-full mb-3 ${isFirstRun ? 'bg-amber-500/10 text-amber-500' : 'bg-emerald-500/10 text-emerald-500'}`}>
            {isFirstRun ? <ShieldAlert size={32} /> : <Database size={32} />}
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">
            {isFirstRun ? 'První nastavení NASu' : isRegister ? 'Vytvořit účet' : 'Přihlášení do NASu'}
          </h1>
          <p className="text-sm text-zinc-400 mt-1 text-center">
            {isFirstRun ? 'V databázi nebyl nalezen žádný správce. Nastavte účet ADMINA.' : 'Vítejte zpět v lokálním úložišti.'}
          </p>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-500/10 border border-red-500/20 text-red-400 rounded-lg text-sm text-center">
            {error}
          </div>
        )}

        {/* SAMOTNÝ FORMULÁŘ */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1">Uživatelské jméno</label>
            <div className="relative">
              <User className="absolute left-3 top-3.5 text-zinc-500" size={18} />
              <input type="text" required value={username} onChange={e => setUsername(e.target.value)}
                className="w-full bg-zinc-950 border border-zinc-800 rounded-lg py-3 pl-10 pr-4 text-white placeholder-zinc-600 focus:outline-none focus:border-emerald-500 text-sm" placeholder="admin / uzivatel" />
            </div>
          </div>

          {(isFirstRun || isRegister) && (
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1">E-mailová adresa</label>
              <div className="relative">
                <Mail className="absolute left-3 top-3.5 text-zinc-500" size={18} />
                <input type="email" required value={email} onChange={e => setEmail(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-lg py-3 pl-10 pr-4 text-white placeholder-zinc-600 focus:outline-none focus:border-emerald-500 text-sm" placeholder="jmeno@domena.cz" />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1">Heslo</label>
            <div className="relative">
              <Lock className="absolute left-3 top-3.5 text-zinc-500" size={18} />
              <input type="password" required value={password} onChange={e => setPassword(e.target.value)}
                className="w-full bg-zinc-950 border border-zinc-800 rounded-lg py-3 pl-10 pr-4 text-white placeholder-zinc-600 focus:outline-none focus:border-emerald-500 text-sm" placeholder="••••••••" />
            </div>
          </div>

          {/* Zapamatovat si přihlášení uživatele, pokud to není první běh a provádí se login (při registraci to nebude vidět) */}
          {!isFirstRun && !isRegister && (
            <div className="flex items-center justify-between pt-1">
              <label className="flex items-center space-x-2.5 cursor-pointer select-none group">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                  className="w-4 h-4 rounded bg-zinc-950 border-zinc-800 text-emerald-500 accent-emerald-500 focus:ring-0 focus:ring-offset-0 cursor-pointer"
                />
                <span className="text-sm text-zinc-400 group-hover:text-zinc-300 transition-colors">
                  Zapamatovat si mě (7 dní)
                </span>
              </label>
            </div>
          )}

          <button type="submit" className={`w-full font-bold py-3 px-4 rounded-lg mt-2 transition duration-200 text-sm text-white ${isFirstRun ? 'bg-amber-500 hover:bg-amber-600' : 'bg-emerald-500 hover:bg-emerald-600'}`}>
            {isFirstRun ? 'Inicializovat systém' : isRegister ? 'Zaregistrovat se' : 'Vstoupit do úložiště'}
          </button>
        </form>

        {/* PŘEPÍNAČ LOGIN / REGISTRACE (Skrytý v nouzovém režimu) */}
        {!isFirstRun && (
          <div className="mt-6 text-center text-sm text-zinc-500">
            {isRegister ? 'Již máte účet?' : 'Nemáte ještě účet?'}
            <button onClick={() => { setIsRegister(!isRegister); setError(''); }} className="text-emerald-500 font-semibold hover:text-emerald-400 hover:underline transition-colors duration-150 ml-1">
              {isRegister ? 'Přihlásit se' : 'Vytvořit účet'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
