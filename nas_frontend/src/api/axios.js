import axios from 'axios';

const api = axios.create({
  // základní URL je prázdná, protože využíváme proxy ve vite.config.js
  baseURL: '', 
  timeout: 0, // Pro NAS vypínáme timeout, protože nahrávání gigabajtových souborů může trvat dlouho
  withCredentials: true, // Přinutí Axios posílat JSESSIONID cookie při každém požadavku
});

// globální (export = dostupný přes import odkudkoliv) objekt, do kterého z Reactu uložíme funkci pro odhlášení
export const authHandlers = {
  onUnauthorized: () => {} // funkce uložená jako vlastnost objektu
};

  // "globální lapač odpovědí" (Interceptor)
  // Pokud backend vrátí některý z chybových kódů, automaticky uživatele přesměrujeme na login
  api.interceptors.response.use(
    (response) => response,
    (error) => {
      const status = error.response ? error.response.status : null; // kód chyby, pokud server vůbec odpověděl; pokud neodpověděl, status = null
      const requestUrl = error.config && error.config.url ? error.config.url : ''; // URL, na kterou Axios posílal požadavek, pokud vůbec nějaký posílal
      const isAuthEndpoint = requestUrl.includes('/api/auth/login') || requestUrl.includes('/api/auth/init-status');

      // Chytáme 401, 403, 502 i situaci, kdy server neodpoví vůbec (status === null)
      if (!isAuthEndpoint && (status === 401 || status === 403 || status === 502 || status === null)) {
        console.log("Interceptor zachytil chybu, odhlašuji...");
        authHandlers.onUnauthorized();
      }

      return Promise.reject(error);
    }
  );

export default api;
