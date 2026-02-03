/**
 * AI Study Buddy - Layout Components
 *
 * Questo file genera dinamicamente sidebar, topbar e footer
 * così da avere un layout consistente su tutte le pagine
 */

// ==================== CONFIGURAZIONE ====================
const APP_CONFIG = {
    name: 'AI Study Buddy',
    version: '1.0',
    university: 'Università Milano-Bicocca'
};

// ==================== SIDEBAR ====================
function renderSidebar(activePage = '') {
    const navItems = [
        { href: 'index.html', icon: 'bi-house-door', label: 'Dashboard', id: 'index' },
        { href: 'spiegazioni.html', icon: 'bi-lightbulb', label: 'Spiegazioni', id: 'spiegazioni' },
        { href: 'quiz.html', icon: 'bi-patch-question', label: 'Quiz', id: 'quiz' },
        { href: 'flashcards.html', icon: 'bi-stack', label: 'Flashcards', id: 'flashcards' },
        { href: 'profilo.html', icon: 'bi-person-circle', label: 'Profilo', id: 'profilo' },
        { href: 'leaderboard.html', icon: 'bi-trophy', label: 'Classifica', id: 'leaderboard' }
    ];

    // Determina pagina attiva
    const currentPage = activePage || window.location.pathname.split('/').pop().replace('.html', '') || 'index';

    const sidebarHTML = `
        <nav class="sidebar" id="sidebar">
            <div class="sidebar-header">
                <i class="bi bi-mortarboard-fill"></i>
                <span>${APP_CONFIG.name}</span>
            </div>

            <ul class="sidebar-nav">
                ${navItems.map(item => `
                    <li class="nav-item">
                        <a class="nav-link ${item.id === currentPage ? 'active' : ''}" href="${item.href}">
                            <i class="bi ${item.icon}"></i>
                            <span>${item.label}</span>
                        </a>
                    </li>
                `).join('')}
            </ul>

            <div class="sidebar-footer">
                <a href="#" onclick="logout(); return false;" class="btn btn-outline-light btn-sm w-100">
                    <i class="bi bi-box-arrow-right me-2"></i>Logout
                </a>
            </div>
        </nav>
        <div class="sidebar-overlay" id="sidebarOverlay"></div>
    `;

    // Inserisci all'inizio del body
    document.body.insertAdjacentHTML('afterbegin', sidebarHTML);

    // Setup event listeners
    setupSidebarEvents();
}

function setupSidebarEvents() {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebarOverlay');
    const menuBtn = document.getElementById('mobileMenuBtn');

    if (menuBtn) {
        menuBtn.addEventListener('click', () => {
            sidebar?.classList.toggle('show');
            overlay?.classList.toggle('show');
        });
    }

    if (overlay) {
        overlay.addEventListener('click', () => {
            sidebar?.classList.remove('show');
            overlay?.classList.remove('show');
        });
    }
}

// ==================== TOPBAR ====================
function renderTopbar(pageTitle = '') {
    const topbarHTML = `
        <header class="topbar">
            <div class="topbar-left">
                <button class="mobile-menu-btn" id="mobileMenuBtn">
                    <i class="bi bi-list"></i>
                </button>
                <h1 class="topbar-title">${pageTitle}</h1>
            </div>

            <div class="topbar-right">
                <!-- Streak -->
                <div class="streak-badge" title="Giorni consecutivi di studio">
                    <i class="bi bi-fire"></i>
                    <span id="topbarStreak">0</span>
                </div>

                <!-- XP -->
                <div class="xp-badge" title="Punti esperienza">
                    <i class="bi bi-lightning-charge-fill"></i>
                    <span id="topbarXp">0</span>
                </div>

                <!-- Level -->
                <div class="level-badge" title="Livello attuale">
                    <i class="bi bi-star-fill"></i>
                    <span>Lv.<span id="topbarLevel">1</span></span>
                </div>

                <!-- User Menu -->
                <div class="dropdown">
                    <div class="user-menu" data-bs-toggle="dropdown" aria-expanded="false">
                        <div class="user-avatar" id="topbarAvatar">?</div>
                        <span class="user-name" id="topbarUserName">Utente</span>
                        <i class="bi bi-chevron-down text-white-50"></i>
                    </div>
                    <ul class="dropdown-menu dropdown-menu-end">
                        <li>
                            <a class="dropdown-item" href="profilo.html">
                                <i class="bi bi-person"></i> Il mio profilo
                            </a>
                        </li>
                        <li>
                            <a class="dropdown-item" href="leaderboard.html">
                                <i class="bi bi-trophy"></i> Classifica
                            </a>
                        </li>
                        <li><hr class="dropdown-divider"></li>
                        <li>
                            <a class="dropdown-item text-danger" href="#" onclick="logout(); return false;">
                                <i class="bi bi-box-arrow-right"></i> Logout
                            </a>
                        </li>
                    </ul>
                </div>
            </div>
        </header>
    `;

    document.body.insertAdjacentHTML('afterbegin', topbarHTML);

    // Carica dati utente
    loadTopbarUserData();
}

async function loadTopbarUserData() {
    try {
        // Carica dati da localStorage (veloce)
        const user = JSON.parse(localStorage.getItem('user') || '{}');
        updateTopbarUser(user);

        // Poi aggiorna con dati dal server
        const token = localStorage.getItem('token');
        if (!token) return;

        // Carica stats gamification
        const statsResponse = await fetch('/api/gamification/stats', {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (statsResponse.ok) {
            const stats = await statsResponse.json();
            updateTopbarStats(stats);
        }
    } catch (error) {
        console.error('Errore caricamento dati topbar:', error);
    }
}

function updateTopbarUser(user) {
    const avatarEl = document.getElementById('topbarAvatar');
    const nameEl = document.getElementById('topbarUserName');

    if (avatarEl && user) {
        const initials = ((user.firstName?.charAt(0) || '') + (user.lastName?.charAt(0) || '')).toUpperCase() || '?';
        avatarEl.textContent = initials;
    }

    if (nameEl && user) {
        const displayName = `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.email || 'Utente';
        nameEl.textContent = displayName;
    }
}

function updateTopbarStats(stats) {
    const streakEl = document.getElementById('topbarStreak');
    const xpEl = document.getElementById('topbarXp');
    const levelEl = document.getElementById('topbarLevel');

    if (streakEl) streakEl.textContent = stats.currentStreak || 0;
    if (xpEl) xpEl.textContent = formatNumber(stats.totalXp || 0);
    if (levelEl) levelEl.textContent = stats.level || 1;
}

function formatNumber(num) {
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    return num.toString();
}

// ==================== FOOTER ====================
function renderFooter() {
    const year = new Date().getFullYear();

    const footerHTML = `
        <footer class="app-footer">
            <div class="footer-content">
                <span>© ${year} ${APP_CONFIG.name} - ${APP_CONFIG.university}</span>
                <span>
                    <a href="#" onclick="return false;">Privacy</a> ·
                    <a href="#" onclick="return false;">Termini</a> ·
                    <a href="#" onclick="return false;">Contatti</a>
                </span>
            </div>
        </footer>
    `;

    document.body.insertAdjacentHTML('beforeend', footerHTML);
}

// ==================== LAYOUT INIT ====================
/**
 * Inizializza il layout completo della pagina
 * @param {Object} options - Opzioni di configurazione
 * @param {string} options.pageTitle - Titolo della pagina (mostrato nella topbar)
 * @param {string} options.activePage - ID della pagina attiva nella sidebar
 * @param {boolean} options.showFooter - Se mostrare il footer (default: true)
 */
function initLayout(options = {}) {
    const { pageTitle = '', activePage = '', showFooter = true } = options;

    // Verifica autenticazione
    if (!checkAuth()) return;

    // Render componenti
    renderTopbar(pageTitle);
    renderSidebar(activePage);
    if (showFooter) renderFooter();

    // Setup mobile menu dopo che sidebar è renderizzata
    setTimeout(setupSidebarEvents, 0);
}

// ==================== AUTH HELPERS ====================
function checkAuth() {
    const token = localStorage.getItem('token');
    const currentPage = window.location.pathname.split('/').pop();
    const publicPages = ['login.html', 'register.html', ''];

    if (publicPages.includes(currentPage)) return true;

    if (!token) {
        window.location.href = 'login.html';
        return false;
    }

    // Verifica scadenza token
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        if (Date.now() >= payload.exp * 1000) {
            sessionStorage.setItem('authMessage', 'Sessione scaduta. Effettua nuovamente il login.');
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.href = 'login.html';
            return false;
        }
    } catch (e) {
        console.error('Token non valido');
        window.location.href = 'login.html';
        return false;
    }

    return true;
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = 'login.html';
}

// ==================== EXPORT ====================
// Se vuoi usarlo come modulo ES6
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { initLayout, renderSidebar, renderTopbar, renderFooter, logout };
}