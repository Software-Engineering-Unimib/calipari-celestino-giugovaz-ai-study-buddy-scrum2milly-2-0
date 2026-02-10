/**
 * AI Study Buddy - Layout Components
 * Sidebar e Topbar unificati per tutte le pagine
 * Compatibile con style.css esistente
 */

// ==================== HELPER FUNCTIONS ====================
function getUserInitials(user) {
    if (!user) return 'U';
    const first = user.firstName?.charAt(0) || '';
    const last = user.lastName?.charAt(0) || '';
    return (first + last).toUpperCase() || 'U';
}

function getUserDisplayName(user) {
    if (!user) return 'Utente';
    if (user.firstName || user.lastName) {
        return `${user.firstName || ''} ${user.lastName || ''}`.trim();
    }
    if (user.email) return user.email.split('@')[0];
    return 'Utente';
}

// ==================== SIDEBAR ====================
function renderSidebar(activePage = '') {
    const navItems = [
        { href: 'dashboard.html', icon: 'bi-house-door', label: 'Dashboard', id: 'dashboard' },
        { href: 'explanation.html', icon: 'bi-chat-dots', label: 'Spiegazioni AI', id: 'explanation' },
        { href: 'quiz.html', icon: 'bi-patch-question', label: 'Quiz', id: 'quiz' },
        { href: 'flashcards.html', icon: 'bi-stack', label: 'Flashcards', id: 'flashcards' },
        { href: 'focus.html', icon: 'bi-bullseye', label: 'Focus Mode', id: 'focus' },
        { href: 'profile.html', icon: 'bi-person', label: 'Profilo', id: 'profile' },
        { href: 'leaderboard.html', icon: 'bi-trophy', label: 'Classifica', id: 'leaderboard' }
    ];

    // Determina la pagina corrente
    const currentPage = activePage || window.location.pathname.split('/').pop().replace('.html', '') || 'dashboard';
    
    // Ottieni dati utente da localStorage
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    const initials = getUserInitials(user);
    const displayName = getUserDisplayName(user);

    const sidebarHTML = `
        <button class="mobile-menu-btn" id="mobileMenuBtn">
            <i class="bi bi-list"></i>
        </button>

        <div class="sidebar-overlay" id="sidebarOverlay"></div>

        <aside class="sidebar" id="sidebar">
            <a href="dashboard.html" class="sidebar-logo">
                <i class="bi bi-mortarboard-fill"></i>
                <h1>AI Study Buddy</h1>
            </a>

            <nav class="sidebar-nav">
                <div class="nav-section-title">Studio</div>
                ${navItems.slice(0, 5).map(item => `
                    <a href="${item.href}" class="nav-item ${item.id === currentPage ? 'active' : ''}" data-page="${item.id}">
                        <i class="bi ${item.icon}"></i>
                        <span>${item.label}</span>
                    </a>
                `).join('')}
                
                <div class="nav-section-title">Account</div>
                ${navItems.slice(5).map(item => `
                    <a href="${item.href}" class="nav-item ${item.id === currentPage ? 'active' : ''}" data-page="${item.id}">
                        <i class="bi ${item.icon}"></i>
                        <span>${item.label}</span>
                    </a>
                `).join('')}
            </nav>

            <div class="sidebar-footer">
                <div class="user-info">
                    <div class="user-avatar" id="sidebarAvatar">${initials}</div>
                    <div class="user-details">
                        <div class="user-name" id="sidebarUserName">${displayName}</div>
                        <div class="user-level">Livello <span id="sidebarLevel">1</span></div>
                    </div>
                </div>
                <button class="btn btn-outline" style="width: 100%; margin-top: 1rem;" onclick="logout()">
                    <i class="bi bi-box-arrow-right"></i> Logout
                </button>
            </div>
        </aside>
    `;

    document.body.insertAdjacentHTML('afterbegin', sidebarHTML);
    setupSidebarEvents();
    loadSidebarStats();
    
    // Intercetta i click sui link per gestire il Focus Mode
    setupNavigationInterception();
}

function setupSidebarEvents() {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebarOverlay');
    const menuBtn = document.getElementById('mobileMenuBtn');

    menuBtn?.addEventListener('click', () => {
        sidebar?.classList.toggle('open');
        overlay?.classList.toggle('active');
    });

    overlay?.addEventListener('click', () => {
        sidebar?.classList.remove('open');
        overlay?.classList.remove('active');
    });
}

function setupNavigationInterception() {
    // Intercetta tutti i link per gestire il Focus Mode
    document.addEventListener('click', (e) => {
        const link = e.target.closest('a[href]');
        if (!link) return;
        
        const href = link.getAttribute('href');
        if (!href || href.startsWith('#') || href.startsWith('javascript:')) return;
        
        // Verifica se il Focus Mode è attivo
        if (window.FocusManager && window.FocusManager.isInSession()) {
            const isAllowed = window.FocusManager.isAllowedPage(href);
            
            if (!isAllowed) {
                e.preventDefault();
                window.FocusManager.showNavigationConfirm(href);
            }
        }
    });
}

async function loadSidebarStats() {
    try {
        const token = localStorage.getItem('token');
        if (!token) return;

        const response = await fetch('/api/gamification/stats', {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.ok) {
            const stats = await response.json();
            const levelEl = document.getElementById('sidebarLevel');
            if (levelEl) levelEl.textContent = stats.level || 1;
        }
    } catch (e) { 
        console.error('Errore stats sidebar:', e); 
    }
}

// ==================== TOPBAR ====================
function renderTopbar(pageTitle = '', pageSubtitle = '') {
    const topbarHTML = `
        <div class="page-header" style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem;">
            <div>
                <h1 class="page-title">${pageTitle}</h1>
                ${pageSubtitle ? `<p class="page-subtitle">${pageSubtitle}</p>` : ''}
            </div>
            <div class="topbar-right">
                <div class="topbar-stat streak" title="Giorni consecutivi">
                    <i class="bi bi-fire"></i>
                    <span id="topbarStreak">0</span>
                </div>
                <div class="topbar-stat xp" title="XP totali">
                    <i class="bi bi-lightning-charge-fill"></i>
                    <span id="topbarXp">0</span>
                </div>
                <div class="topbar-stat level" title="Livello">
                    <i class="bi bi-star-fill"></i>
                    <span>Lv.<span id="topbarLevel">1</span></span>
                </div>
            </div>
        </div>
    `;

    const mainContent = document.querySelector('.main-content');
    if (mainContent) {
        mainContent.insertAdjacentHTML('afterbegin', topbarHTML);
    }

    loadTopbarStats();
}

async function loadTopbarStats() {
    try {
        const token = localStorage.getItem('token');
        if (!token) return;

        const response = await fetch('/api/gamification/stats', {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.ok) {
            const stats = await response.json();
            const streakEl = document.getElementById('topbarStreak');
            const xpEl = document.getElementById('topbarXp');
            const levelEl = document.getElementById('topbarLevel');

            if (streakEl) streakEl.textContent = stats.currentStreak || 0;
            if (xpEl) xpEl.textContent = formatNumber(stats.totalXp || 0);
            if (levelEl) levelEl.textContent = stats.level || 1;
        }
    } catch (e) { 
        console.error('Errore stats topbar:', e); 
    }
}

function formatNumber(num) {
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    return num.toString();
}

// ==================== LAYOUT INIT ====================
function initLayout(options = {}) {
    const { pageTitle = '', pageSubtitle = '', activePage = '', showTopbar = true } = options;

    if (!checkAuth()) return;

    renderSidebar(activePage);
    if (showTopbar && pageTitle) renderTopbar(pageTitle, pageSubtitle);
}

// ==================== AUTH ====================
function checkAuth() {
    const token = localStorage.getItem('token');
    const currentPage = window.location.pathname.split('/').pop();
    const publicPages = ['login.html', 'register.html', ''];

    if (publicPages.includes(currentPage)) return true;

    if (!token) {
        window.location.href = 'login.html';
        return false;
    }

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
        window.location.href = 'login.html';
        return false;
    }

    return true;
}

function logout() {
    // Se c'è una sessione focus attiva, chiedi conferma
    if (window.FocusManager && window.FocusManager.isInSession()) {
        window.FocusManager.showNavigationConfirm('login.html', true);
        return;
    }
    
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = 'login.html';
}

// Esporta per uso globale
window.initLayout = initLayout;
window.renderSidebar = renderSidebar;
window.renderTopbar = renderTopbar;
window.logout = logout;
window.checkAuth = checkAuth;