/**
 * AI Study Buddy - Layout Components
 * Compatibile con style.css esistente
 */

// ==================== SIDEBAR ====================
function renderSidebar(activePage = '') {
    const navItems = [
        { href: 'index.html', icon: 'bi-house-door', label: 'Dashboard', id: 'index' },
        { href: 'spiegazioni.html', icon: 'bi-lightbulb', label: 'Spiegazioni', id: 'spiegazioni' },
        { href: 'quiz.html', icon: 'bi-patch-question', label: 'Quiz', id: 'quiz' },
        { href: 'flashcards.html', icon: 'bi-stack', label: 'Flashcards', id: 'flashcards' },
        { href: 'profile.html', icon: 'bi-person', label: 'Profilo', id: 'profilo' },
        { href: 'leaderboard.html', icon: 'bi-trophy', label: 'Classifica', id: 'leaderboard' }
    ];

    const currentPage = activePage || window.location.pathname.split('/').pop().replace('.html', '') || 'index';
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    const initials = ((user.firstName?.charAt(0) || '') + (user.lastName?.charAt(0) || '')).toUpperCase() || '?';
    const displayName = `${user.firstName || ''} ${user.lastName || ''}`.trim() || 'Utente';

    const sidebarHTML = `
        <button class="mobile-menu-btn" id="mobileMenuBtn">
            <i class="bi bi-list"></i>
        </button>

        <aside class="sidebar" id="sidebar">
            <a href="index.html" class="sidebar-logo">
                <i class="bi bi-mortarboard-fill"></i>
                <h1>AI Study Buddy</h1>
            </a>

            <nav class="sidebar-nav">
                ${navItems.map(item => `
                    <a href="${item.href}" class="nav-item ${item.id === currentPage ? 'active' : ''}">
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
                <button class="btn btn-outline w-100 mt-3" onclick="logout()">
                    <i class="bi bi-box-arrow-right"></i> Logout
                </button>
            </div>
        </aside>

        <div class="sidebar-overlay" id="sidebarOverlay"></div>
    `;

    document.body.insertAdjacentHTML('afterbegin', sidebarHTML);
    setupSidebarEvents();
    loadSidebarStats();
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
    } catch (e) { console.error('Errore stats sidebar:', e); }
}

// ==================== TOPBAR ====================
function renderTopbar(pageTitle = '') {
    const topbarHTML = `
        <header class="topbar" id="topbar">
            <div class="topbar-left">
                <h1 class="topbar-title">${pageTitle}</h1>
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
        </header>
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
    } catch (e) { console.error('Errore stats topbar:', e); }
}

function formatNumber(num) {
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    return num.toString();
}

// ==================== FOOTER ====================
function renderFooter() {
    const footerHTML = `
        <footer class="app-footer">
            <span>© ${new Date().getFullYear()} AI Study Buddy - Università Milano-Bicocca</span>
        </footer>
    `;
    document.body.insertAdjacentHTML('beforeend', footerHTML);
}

// ==================== LAYOUT INIT ====================
function initLayout(options = {}) {
    const { pageTitle = '', activePage = '', showTopbar = true, showFooter = true } = options;

    if (!checkAuth()) return;

    renderSidebar(activePage);
    if (showTopbar) renderTopbar(pageTitle);
    if (showFooter) renderFooter();
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
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = 'login.html';
}