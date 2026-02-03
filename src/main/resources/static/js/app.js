// ==================== CONFIGURAZIONE API ====================

const API_BASE_URL = '/api';

// ==================== JWT TOKEN MANAGEMENT ====================

/**
 * Decodifica un token JWT (solo payload, senza verifica firma)
 */
function decodeJWT(token) {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(c =>
            '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
        ).join(''));
        return JSON.parse(jsonPayload);
    } catch (e) {
        console.error('Errore decodifica JWT:', e);
        return null;
    }
}

/**
 * Controlla se il token è scaduto
 * @param {number} bufferSeconds - Secondi di buffer prima della scadenza effettiva (default 60)
 */
function isTokenExpired(bufferSeconds = 60) {
    const token = localStorage.getItem('token');
    if (!token) return true;

    const payload = decodeJWT(token);
    if (!payload || !payload.exp) return true;

    // exp è in secondi, Date.now() in millisecondi
    const expirationTime = payload.exp * 1000;
    const currentTime = Date.now();
    const bufferTime = bufferSeconds * 1000;

    return currentTime >= (expirationTime - bufferTime);
}

/**
 * Ottiene il tempo rimanente prima della scadenza del token (in secondi)
 */
function getTokenTimeRemaining() {
    const token = localStorage.getItem('token');
    if (!token) return 0;

    const payload = decodeJWT(token);
    if (!payload || !payload.exp) return 0;

    const remaining = (payload.exp * 1000) - Date.now();
    return Math.max(0, Math.floor(remaining / 1000));
}

/**
 * Pulisce i dati di autenticazione e reindirizza al login
 */
function clearAuthAndRedirect(message = null) {
    localStorage.removeItem('token');
    localStorage.removeItem('user');

    // Salva messaggio per mostrarlo nella pagina login
    if (message) {
        sessionStorage.setItem('authMessage', message);
    }

    // Evita loop di redirect se siamo già sulla pagina login
    const currentPage = window.location.pathname.split('/').pop();
    if (currentPage !== 'login.html' && currentPage !== 'register.html') {
        window.location.href = 'login.html';
    }
}

/**
 * Verifica autenticazione all'avvio della pagina
 */
function checkAuthOnLoad() {
    const currentPage = window.location.pathname.split('/').pop();
    const publicPages = ['login.html', 'register.html', ''];

    // Salta controllo per pagine pubbliche
    if (publicPages.includes(currentPage)) {
        return true;
    }

    const token = localStorage.getItem('token');

    // Nessun token presente
    if (!token) {
        clearAuthAndRedirect('Effettua il login per continuare');
        return false;
    }

    // Token scaduto
    if (isTokenExpired()) {
        clearAuthAndRedirect('La sessione è scaduta. Effettua nuovamente il login.');
        return false;
    }

    // Imposta timer per controllo periodico
    setupTokenExpirationCheck();

    return true;
}

/**
 * Imposta un controllo periodico della scadenza del token
 */
let tokenCheckInterval = null;

function setupTokenExpirationCheck() {
    // Pulisci eventuale interval precedente
    if (tokenCheckInterval) {
        clearInterval(tokenCheckInterval);
    }

    // Controlla ogni 30 secondi
    tokenCheckInterval = setInterval(() => {
        if (isTokenExpired()) {
            clearInterval(tokenCheckInterval);
            showSessionExpiredModal();
        }
    }, 30000);

    // Controlla anche quando la finestra torna in focus
    window.addEventListener('focus', handleWindowFocus);
}

function handleWindowFocus() {
    if (isTokenExpired()) {
        showSessionExpiredModal();
    }
}

/**
 * Mostra un modal quando la sessione scade (invece di redirect immediato)
 */
function showSessionExpiredModal() {
    // Rimuovi modal esistente se presente
    const existingModal = document.getElementById('sessionExpiredModal');
    if (existingModal) {
        existingModal.remove();
    }

    const modalHtml = `
        <div class="modal fade show" id="sessionExpiredModal" tabindex="-1"
             style="display: block; background: rgba(0,0,0,0.5);" data-bs-backdrop="static">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-body text-center p-4">
                        <i class="bi bi-clock-history text-warning" style="font-size: 3rem;"></i>
                        <h4 class="mt-3">Sessione Scaduta</h4>
                        <p class="text-muted">La tua sessione è scaduta per motivi di sicurezza.</p>
                        <button class="btn btn-primary px-4" onclick="clearAuthAndRedirect()">
                            <i class="bi bi-box-arrow-in-right me-2"></i>Accedi di nuovo
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHtml);

    // Blocca lo scroll della pagina
    document.body.style.overflow = 'hidden';
}

// ==================== UTILITY FUNCTIONS ====================

/**
 * Headers per le richieste autenticate
 */
function getAuthHeaders() {
    const token = localStorage.getItem('token');
    return {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
    };
}

/**
 * Fetch wrapper con gestione errori e token scaduto
 */
async function apiFetch(endpoint, options = {}) {
    // Controllo preventivo token
    if (isTokenExpired()) {
        showSessionExpiredModal();
        throw new Error('TOKEN_EXPIRED');
    }

    const defaultOptions = {
        headers: getAuthHeaders()
    };

    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            ...defaultOptions,
            ...options,
            headers: {
                ...defaultOptions.headers,
                ...options.headers
            }
        });

        // Token scaduto o non valido (risposta dal server)
        if (response.status === 401) {
            showSessionExpiredModal();
            throw new Error('UNAUTHORIZED');
        }

        // Forbidden (permessi insufficienti)
        if (response.status === 403) {
            console.warn('Accesso negato:', endpoint);
            throw new Error('FORBIDDEN');
        }

        return response;
    } catch (error) {
        // Gestione errori di rete
        if (error.name === 'TypeError' && error.message.includes('fetch')) {
            console.error('Errore di rete:', error);
            throw new Error('NETWORK_ERROR');
        }
        throw error;
    }
}

/**
 * Traduzione codici errore
 */
const ERROR_MESSAGES = {
    'EMAIL_EXISTS': 'Questa email è già registrata',
    'REGISTRATION_SUCCESS': 'Registrazione completata!',
    'LOGIN_SUCCESS': 'Login riuscito!',
    'INVALID_CREDENTIALS': 'Email o password errati',
    'TOKEN_VALID': 'Token valido',
    'TOKEN_EXPIRED': 'Sessione scaduta',
    'USER_NOT_FOUND': 'Utente non trovato',
    'UNAUTHORIZED': 'Non autorizzato',
    'FORBIDDEN': 'Accesso negato',
    'NETWORK_ERROR': 'Errore di connessione. Verifica la tua rete.',
    'AI_SERVICE_UNAVAILABLE': 'Servizio AI non disponibile. Riprova.',
    'AI_TIMEOUT': 'Timeout del servizio AI. Riprova.',
    'DECK_NOT_FOUND': 'Deck non trovato',
    'FLASHCARD_NOT_FOUND': 'Flashcard non trovata',
    'DECK_ACCESS_DENIED': 'Accesso al deck negato'
};

function getErrorMessage(code) {
    return ERROR_MESSAGES[code] || code;
}

// ==================== USER INFO ====================

function getCurrentUser() {
    const userJson = localStorage.getItem('user');
    return userJson ? JSON.parse(userJson) : null;
}

function getUserInitials() {
    const user = getCurrentUser();
    if (!user) return '?';
    return (user.firstName?.charAt(0) || '') + (user.lastName?.charAt(0) || '');
}

function updateUserInfo() {
    const user = getCurrentUser();
    if (!user) return;

    const userNameEl = document.getElementById('userName');
    const userAvatarEl = document.getElementById('userAvatar');

    if (userNameEl) {
        userNameEl.textContent = `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email;
    }
    if (userAvatarEl) {
        userAvatarEl.textContent = getUserInitials();
    }
}

// ==================== SIDEBAR ====================

function initSidebar() {
    const mobileMenuBtn = document.getElementById('mobileMenuBtn');
    const sidebar = document.getElementById('sidebar');
    const sidebarOverlay = document.getElementById('sidebarOverlay');

    if (mobileMenuBtn) {
        mobileMenuBtn.addEventListener('click', () => {
            sidebar?.classList.toggle('open');
            sidebar?.classList.toggle('show');
            sidebarOverlay?.classList.toggle('active');
            sidebarOverlay?.classList.toggle('show');
        });
    }

    if (sidebarOverlay) {
        sidebarOverlay.addEventListener('click', () => {
            sidebar?.classList.remove('open', 'show');
            sidebarOverlay?.classList.remove('active', 'show');
        });
    }

    // Evidenzia pagina corrente
    const currentPage = window.location.pathname.split('/').pop() || 'index.html';
    document.querySelectorAll('.nav-item, .nav-link').forEach(item => {
        const href = item.getAttribute('href');
        if (href === currentPage) {
            item.classList.add('active');
        }
    });
}

// ==================== ALERTS ====================

function showAlert(message, type = 'error', containerId = 'alertContainer') {
    const container = document.getElementById(containerId);
    if (!container) {
        console.warn('Alert container not found:', containerId);
        return;
    }

    const alertClass = type === 'error' ? 'alert-danger' :
                       type === 'success' ? 'alert-success' :
                       type === 'warning' ? 'alert-warning' : 'alert-info';

    const icon = type === 'error' ? 'bi-exclamation-circle' :
                 type === 'success' ? 'bi-check-circle' :
                 type === 'warning' ? 'bi-exclamation-triangle' : 'bi-info-circle';

    container.innerHTML = `
        <div class="alert ${alertClass} alert-dismissible fade show" role="alert">
            <i class="bi ${icon} me-2"></i>
            <span>${message}</span>
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;

    // Auto-dismiss after 5 seconds
    setTimeout(() => {
        const alert = container.querySelector('.alert');
        if (alert) {
            alert.classList.remove('show');
            setTimeout(() => container.innerHTML = '', 150);
        }
    }, 5000);
}

/**
 * Toast notification (alternativa agli alert)
 */
function showToast(message, type = 'info') {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.style.cssText = 'position: fixed; bottom: 20px; right: 20px; z-index: 9999;';
        document.body.appendChild(container);
    }

    const icons = {
        success: 'bi-check-circle-fill text-success',
        error: 'bi-exclamation-circle-fill text-danger',
        warning: 'bi-exclamation-triangle-fill text-warning',
        info: 'bi-info-circle-fill text-primary'
    };

    const toast = document.createElement('div');
    toast.className = 'toast-notification';
    toast.style.cssText = `
        background: white; border-radius: 12px; padding: 1rem 1.5rem;
        box-shadow: 0 4px 20px rgba(0,0,0,0.15); display: flex; align-items: center;
        gap: 0.75rem; margin-bottom: 0.5rem; animation: slideIn 0.3s ease;
    `;
    toast.innerHTML = `
        <i class="bi ${icons[type] || icons.info} fs-5"></i>
        <span>${message}</span>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideIn 0.3s ease reverse';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// ==================== LOGOUT ====================

function logout() {
    clearAuthAndRedirect();
}

// ==================== INIT ====================

document.addEventListener('DOMContentLoaded', () => {
    // Verifica autenticazione (solo per pagine protette)
    if (!checkAuthOnLoad()) {
        return; // Stop se non autenticato
    }

    // Inizializza UI
    initSidebar();
    updateUserInfo();

    // Mostra eventuali messaggi dalla sessione precedente
    const authMessage = sessionStorage.getItem('authMessage');
    if (authMessage) {
        sessionStorage.removeItem('authMessage');
        // Mostra il messaggio solo se siamo sulla pagina login
        const currentPage = window.location.pathname.split('/').pop();
        if (currentPage === 'login.html') {
            setTimeout(() => showAlert(authMessage, 'warning'), 100);
        }
    }
});

// Cleanup quando si lascia la pagina
window.addEventListener('beforeunload', () => {
    if (tokenCheckInterval) {
        clearInterval(tokenCheckInterval);
    }
    window.removeEventListener('focus', handleWindowFocus);
});