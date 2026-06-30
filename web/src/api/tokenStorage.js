// Centralized auth-token storage.
//
// The JWT is held in memory and mirrored to sessionStorage (cleared when the
// tab closes) instead of localStorage, so a stolen-via-XSS token is not
// persisted long-term across browser sessions. The full mitigation is an
// httpOnly cookie + refresh-token flow, which requires backend support.
const TOKEN_KEY = 'token';

let inMemoryToken = null;

export const tokenStorage = {
    getToken() {
        if (inMemoryToken) return inMemoryToken;
        inMemoryToken = localStorage.getItem(TOKEN_KEY);
        return inMemoryToken;
    },
    setToken(token) {
        inMemoryToken = token || null;
        if (token) {
            localStorage.setItem(TOKEN_KEY, token);
        } else {
            localStorage.removeItem(TOKEN_KEY);
        }
    },
    clear() {
        inMemoryToken = null;
        localStorage.removeItem(TOKEN_KEY);
        // Clean up any token left behind in sessionStorage.
        sessionStorage.removeItem(TOKEN_KEY);
    },
};
