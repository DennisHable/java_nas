import React, { useState, useEffect } from 'react';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import api,{ authHandlers } from './api/axios';
import { Loader2 } from 'lucide-react'; // ikona pro točící se kolečko

export default function App() {
  const [user, setUser] = useState(null); // objekt přihlášeného uživatele (id, username, role)
  const [loading, setLoading] = useState(true); 

  // okamžitě zkontrolujeme při načtení aplikace, zda je uživatel přihlášený (pokud má v prohlížeči platné JSESSIONID)
  useEffect(() => {
    // nastavení globální funkce pro odhlášení, která se volá z interceptoru v axios.js
    authHandlers.onUnauthorized = () => {
      setUser(null);
    };

    api.get('/api/auth/me')
      .then(res => {
        // Pokud má uživatel v prohlížeči platné JSESSIONID, backend vrátí 200 OK a jeho data
        setUser(res.data);
      })
      .catch(() => {
        // Pokud vrátí 401 nebo 204, uživatel není přihlášen, necháme user = null
        setUser(null);
      })
      .finally(() => {
        // ať už dotaz uspěl nebo selhal, načítání skončilo
        // skryjeme loading spinner a zobrazíme buď login, nebo dashboard
        setLoading(false); 
      });
  }, []); // spustí se pouze jednou při prvním renderu komponenty

  // dokud zjišťujeme stav přihlášení ze serveru, zobrazíme pouze čistou tmavou plochu s načítáním
  if (loading) {
    return (
      <div className="min-h-screen bg-zinc-950 flex items-center justify-center text-zinc-500">
        <Loader2 className="animate-spin text-emerald-500" size={32} />
      </div>
    );
  }

  // dokud uživatel není přihlášen, zobrazujeme login obrazovku
  if (!user) { 
    return <Login onLoginSuccess={(userData) => setUser(userData)} />;
  }

  // Po přihlášení předáme objekt uživatele do Dashboardu
  return <Dashboard user={user} onLogout={() => setUser(null)} />;
}
