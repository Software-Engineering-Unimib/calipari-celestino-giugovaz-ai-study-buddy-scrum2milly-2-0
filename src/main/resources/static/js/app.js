// ==================== CONFIGURAZIONE API ====================

const API_BASE_URL = '/api';

// ==================== UTILITY FUNCTIONS ====================

// Headers per le richieste autenticate
function getAuthHeaders() {
    const token = localStorage.getItem('token');
    return {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
    };
}

// Fetch wrapper con gestione errori
async function apiFetch(endpoint, options = {}) {
    const defaultOptions = {
        headers: getAuthHeaders()
    };

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...defaultOptions,
        ...options,
        headers: {
            ...defaultOptions.headers,
            ...options.headers
        }
    });

    if (response.status === 401) {
        // Token scaduto o non valido
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.location.href = 'login.html';
        throw new Error('Sessione scaduta');
    }

    return response;
}

// Traduzione codici errore
const ERROR_MESSAGES = {
    'EMAIL_EXISTS': 'Questa email è già registrata',
    'REGISTRATION_SUCCESS': 'Registrazione completata!',
    'LOGIN_SUCCESS': 'Login riuscito!',
    'INVALID_CREDENTIALS': 'Email o password errati',
    'TOKEN_VALID': 'Token valido',
    'USER_NOT_FOUND': 'Utente non trovato',
    'UNAUTHORIZED': 'Non autorizzato',
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
        userNameEl.textContent = `${user.firstName} ${user.lastName}`;
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
            sidebar.classList.toggle('open');
            sidebarOverlay.classList.toggle('active');
        });
    }

    if (sidebarOverlay) {
        sidebarOverlay.addEventListener('click', () => {
            sidebar.classList.remove('open');
            sidebarOverlay.classList.remove('active');
        });
    }

    // Evidenzia pagina corrente
    const currentPage = window.location.pathname.split('/').pop() || 'index.html';
    document.querySelectorAll('.nav-item').forEach(item => {
        if (item.getAttribute('href') === currentPage) {
            item.classList.add('active');
        }
    });
}

// ==================== ALERTS ====================

function showAlert(message, type = 'error', containerId = 'alertContainer') {
    const container = document.getElementById(containerId);
    if (!container) return;

    const alertClass = type === 'error' ? 'alert-error' : 'alert-success';
    const icon = type === 'error' ? 'bi-exclamation-circle' : 'bi-check-circle';

    container.innerHTML = `
        <div class="alert ${alertClass}">
            <i class="bi ${icon}"></i>
            <span>${message}</span>
        </div>
    `;

    setTimeout(() => {
        container.innerHTML = '';
    }, 5000);
}

// ==================== INIT ====================

document.addEventListener('DOMContentLoaded', () => {
    initSidebar();
    updateUserInfo();
});