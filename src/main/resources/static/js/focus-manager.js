/**
 * Focus Mode Manager
 * Gestisce lo stato della modalità focus su tutte le pagine
 * - Timer sincronizzato che scorre anche fuori dalla pagina focus
 * - Modale personalizzata per conferma uscita (non usa beforeunload di Safari)
 * - Permette navigazione libera verso pagine consentite
 */

const FocusManager = {
    // ==================== STATO ====================
    state: null,
    timerInterval: null,

    // Pagine permesse durante il focus mode (senza conferma)
    allowedPages: ['focus.html', 'quiz.html', 'flashcards.html', 'spiegazioni.html', 'explanation.html'],

    // ==================== INIZIALIZZAZIONE ====================
    init() {
        this.loadState();
        this.injectStyles();
        this.injectModal();

        if (this.isInSession()) {
            this.startTimerSync();
            this.renderFocusBanner();
        }

        // Ascolta cambiamenti localStorage da altre tab
        window.addEventListener('storage', (e) => {
            if (e.key === 'focusState') {
                this.loadState();
                if (this.isInSession()) {
                    this.renderFocusBanner();
                    this.startTimerSync();
                } else {
                    this.removeFocusBanner();
                    this.stopTimerSync();
                }
            }
        });
    },

    // ==================== STYLES INJECTION ====================
    injectStyles() {
        if (document.getElementById('focusManagerStyles')) return;

        const styles = document.createElement('style');
        styles.id = 'focusManagerStyles';
        styles.textContent = `
            /* FOCUS BANNER */
            #focusModeBanner {
                position: fixed;
                top: 0;
                left: 0;
                right: 0;
                background: linear-gradient(135deg, rgba(16, 185, 129, 0.95) 0%, rgba(5, 150, 105, 0.95) 100%);
                backdrop-filter: blur(10px);
                padding: 0.75rem 1.5rem;
                z-index: 9999;
                box-shadow: 0 4px 20px rgba(16, 185, 129, 0.3);
            }

            .focus-banner-content {
                max-width: 1200px;
                margin: 0 auto;
                display: flex;
                justify-content: space-between;
                align-items: center;
                flex-wrap: wrap;
                gap: 1rem;
            }

            .focus-banner-left {
                display: flex;
                align-items: center;
                gap: 0.75rem;
            }

            .focus-banner-pulse {
                width: 10px;
                height: 10px;
                background: #ffffff;
                border-radius: 50%;
                animation: focusPulse 2s infinite;
            }

            @keyframes focusPulse {
                0%, 100% { opacity: 1; transform: scale(1); }
                50% { opacity: 0.5; transform: scale(1.3); }
            }

            .focus-banner-left i {
                font-size: 1.25rem;
                color: #ffffff;
            }

            .focus-banner-text {
                color: #ffffff;
                display: flex;
                align-items: center;
                gap: 1rem;
            }

            .focus-banner-text strong {
                font-weight: 600;
            }

            .focus-banner-time {
                font-family: 'SF Mono', 'Consolas', monospace;
                font-size: 1.25rem;
                font-weight: 700;
                background: rgba(255, 255, 255, 0.2);
                padding: 0.25rem 0.75rem;
                border-radius: 0.5rem;
            }

            .focus-banner-right {
                display: flex;
                align-items: center;
                gap: 1rem;
            }

            .focus-banner-xp {
                color: #ffffff;
                font-weight: 700;
                background: rgba(255, 255, 255, 0.2);
                padding: 0.25rem 0.75rem;
                border-radius: 0.5rem;
            }

            .focus-banner-btn {
                display: flex;
                align-items: center;
                gap: 0.5rem;
                background: #ffffff;
                color: #059669;
                padding: 0.5rem 1rem;
                border-radius: 0.5rem;
                text-decoration: none;
                font-weight: 600;
                font-size: 0.9rem;
                transition: all 0.2s ease;
                border: none;
                cursor: pointer;
            }

            .focus-banner-btn:hover {
                transform: translateY(-1px);
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
                color: #059669;
            }

            /* Offset per il contenuto della pagina quando banner attivo */
            body.focus-mode-active {
                padding-top: 60px !important;
            }

            body.focus-mode-active .sidebar {
                top: 60px;
                height: calc(100vh - 60px);
            }

            body.focus-mode-active .mobile-menu-btn {
                top: calc(1rem + 60px);
            }

            /* MODALE CONFERMA USCITA */
            #focusExitModal {
                display: none;
                position: fixed;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: rgba(0, 0, 0, 0.7);
                backdrop-filter: blur(5px);
                z-index: 10000;
                justify-content: center;
                align-items: center;
            }

            #focusExitModal.show {
                display: flex;
            }

            .focus-exit-dialog {
                background: linear-gradient(145deg, #374151 0%, #1f2937 100%);
                border-radius: 1rem;
                padding: 2rem;
                max-width: 400px;
                width: 90%;
                box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5);
                border: 2px solid #4b5563;
                text-align: center;
            }

            .focus-exit-icon {
                width: 60px;
                height: 60px;
                background: rgba(239, 68, 68, 0.2);
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                margin: 0 auto 1.5rem;
            }

            .focus-exit-icon i {
                font-size: 1.75rem;
                color: #ef4444;
            }

            .focus-exit-title {
                font-size: 1.25rem;
                font-weight: 700;
                color: #ffffff;
                margin-bottom: 0.75rem;
            }

            .focus-exit-text {
                color: #9ca3af;
                margin-bottom: 1.5rem;
                line-height: 1.5;
            }

            .focus-exit-stats {
                background: rgba(139, 92, 246, 0.1);
                border-radius: 0.75rem;
                padding: 1rem;
                margin-bottom: 1.5rem;
                display: flex;
                justify-content: space-around;
                border: 1px solid rgba(139, 92, 246, 0.3);
            }

            .focus-exit-stat {
                text-align: center;
            }

            .focus-exit-stat-value {
                font-size: 1.5rem;
                font-weight: 700;
                color: #a78bfa;
            }

            .focus-exit-stat-label {
                font-size: 0.75rem;
                color: #9ca3af;
                text-transform: uppercase;
            }

            .focus-exit-buttons {
                display: flex;
                gap: 1rem;
            }

            .focus-exit-btn {
                flex: 1;
                padding: 0.75rem 1rem;
                border-radius: 0.75rem;
                font-weight: 600;
                cursor: pointer;
                transition: all 0.2s ease;
                border: none;
                font-size: 0.9rem;
            }

            .focus-exit-btn.cancel {
                background: #4b5563;
                color: #ffffff;
            }

            .focus-exit-btn.cancel:hover {
                background: #6b7280;
            }

            .focus-exit-btn.confirm {
                background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
                color: #ffffff;
            }

            .focus-exit-btn.confirm:hover {
                transform: translateY(-2px);
                box-shadow: 0 4px 12px rgba(239, 68, 68, 0.4);
            }

            /* Mobile */
            @media (max-width: 768px) {
                #focusModeBanner {
                    padding: 0.5rem 1rem;
                }

                .focus-banner-content {
                    justify-content: center;
                    text-align: center;
                }

                .focus-banner-text strong {
                    display: none;
                }

                .focus-exit-buttons {
                    flex-direction: column;
                }
            }
        `;
        document.head.appendChild(styles);
    },

    // ==================== MODAL INJECTION ====================
    injectModal() {
        if (document.getElementById('focusExitModal')) return;

        const modal = document.createElement('div');
        modal.id = 'focusExitModal';
        modal.innerHTML = `
            <div class="focus-exit-dialog">
                <div class="focus-exit-icon">
                    <i class="bi bi-exclamation-triangle"></i>
                </div>
                <div class="focus-exit-title">Interrompere la sessione Focus?</div>
                <div class="focus-exit-text">
                    Stai per lasciare la sessione di studio. I tuoi progressi verranno salvati.
                </div>
                <div class="focus-exit-stats">
                    <div class="focus-exit-stat">
                        <div class="focus-exit-stat-value" id="exitModalTime">00:00</div>
                        <div class="focus-exit-stat-label">Tempo</div>
                    </div>
                    <div class="focus-exit-stat">
                        <div class="focus-exit-stat-value" id="exitModalXp">+0</div>
                        <div class="focus-exit-stat-label">XP Guadagnati</div>
                    </div>
                </div>
                <div class="focus-exit-buttons">
                    <button class="focus-exit-btn cancel" id="focusExitCancel">
                        Continua sessione
                    </button>
                    <button class="focus-exit-btn confirm" id="focusExitConfirm">
                        Termina e esci
                    </button>
                </div>
            </div>
        `;
        document.body.appendChild(modal);

        // Event listeners
        document.getElementById('focusExitCancel').addEventListener('click', () => {
            this.hideNavigationConfirm();
        });

        document.getElementById('focusExitConfirm').addEventListener('click', () => {
            this.confirmExit();
        });

        // Chiudi con click fuori
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                this.hideNavigationConfirm();
            }
        });

        // Chiudi con ESC
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && modal.classList.contains('show')) {
                this.hideNavigationConfirm();
            }
        });
    },

    // ==================== NAVIGATION HANDLING ====================
    pendingNavigation: null,
    isLogout: false,

    showNavigationConfirm(targetUrl, isLogout = false) {
        this.pendingNavigation = targetUrl;
        this.isLogout = isLogout;

        // Aggiorna le stats nella modale
        if (this.state) {
            const elapsed = this.state.elapsedSeconds || 0;
            const minutes = Math.floor(elapsed / 60);
            const seconds = elapsed % 60;

            document.getElementById('exitModalTime').textContent =
                `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
            document.getElementById('exitModalXp').textContent = `+${this.state.earnedXp || 0}`;
        }

        document.getElementById('focusExitModal').classList.add('show');
    },

    hideNavigationConfirm() {
        document.getElementById('focusExitModal').classList.remove('show');
        this.pendingNavigation = null;
        this.isLogout = false;
    },

    confirmExit() {
        const targetUrl = this.pendingNavigation;
        const isLogout = this.isLogout;

        // Salva la sessione e termina
        this.saveSessionToBackend();
        this.endSession();

        this.hideNavigationConfirm();

        // Naviga
        if (isLogout) {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
        }

        if (targetUrl) {
            window.location.href = targetUrl;
        }
    },

    getCurrentPageName() {
        const path = window.location.pathname;
        return path.split('/').pop() || 'index.html';
    },

    isAllowedPage(url) {
        try {
            const pathname = new URL(url, window.location.origin).pathname;
            const filename = pathname.split('/').pop() || 'index.html';
            return this.allowedPages.some(page =>
                filename === page || filename.includes(page.replace('.html', ''))
            );
        } catch {
            return false;
        }
    },

    // ==================== STATE MANAGEMENT ====================
    loadState() {
        const saved = localStorage.getItem('focusState');
        if (saved) {
            this.state = JSON.parse(saved);

            // Ricalcola il tempo rimanente basandosi sul timestamp
            if (this.state && this.state.isRunning && !this.state.isPaused && this.state.lastUpdate) {
                const now = Date.now();
                const elapsed = Math.floor((now - this.state.lastUpdate) / 1000);

                if (elapsed > 0) {
                    this.state.remainingSeconds = Math.max(0, this.state.remainingSeconds - elapsed);
                    this.state.elapsedSeconds = (this.state.elapsedSeconds || 0) + elapsed;
                    this.state.lastUpdate = now;

                    // Calcola XP guadagnati (5 XP ogni 2 minuti)
                    this.state.earnedXp = Math.floor(this.state.elapsedSeconds / 120) * 5;

                    this.saveState();

                    // Se il tempo è scaduto
                    if (this.state.remainingSeconds <= 0) {
                        this.handleTimerComplete();
                    }
                }
            }
        } else {
            this.state = null;
        }
    },

    saveState() {
        if (this.state) {
            this.state.lastUpdate = Date.now();
            localStorage.setItem('focusState', JSON.stringify(this.state));
        }
    },

    isActive() {
        return this.state && this.state.isRunning && !this.state.isPaused;
    },

    isPaused() {
        return this.state && this.state.isRunning && this.state.isPaused;
    },

    isInSession() {
        return this.state && this.state.isRunning;
    },

    // ==================== TIMER SYNC ====================
    startTimerSync() {
        this.stopTimerSync();

        // Aggiorna ogni secondo
        this.timerInterval = setInterval(() => {
            if (!this.isActive()) {
                this.loadState(); // Ricarica per vedere se è stato messo in pausa
                this.updateBannerTime();
                return;
            }

            // Aggiorna il tempo rimanente
            if (this.state.remainingSeconds > 0) {
                this.state.remainingSeconds--;
                this.state.elapsedSeconds = (this.state.elapsedSeconds || 0) + 1;

                // Calcola XP (5 XP ogni 2 minuti)
                this.state.earnedXp = Math.floor(this.state.elapsedSeconds / 120) * 5;

                this.saveState();
                this.updateBannerTime();
            } else {
                this.handleTimerComplete();
            }
        }, 1000);
    },

    stopTimerSync() {
        if (this.timerInterval) {
            clearInterval(this.timerInterval);
            this.timerInterval = null;
        }
    },

    handleTimerComplete() {
        // Timer completato
        this.saveSessionToBackend();
        this.endSession();

        // Mostra notifica se non siamo sulla pagina focus
        if (this.getCurrentPageName() !== 'focus.html') {
            this.showCompletionNotification();
        }
    },

    showCompletionNotification() {
        const toast = document.createElement('div');
        toast.style.cssText = `
            position: fixed;
            bottom: 2rem;
            right: 2rem;
            background: linear-gradient(135deg, #10b981 0%, #059669 100%);
            color: white;
            padding: 1rem 1.5rem;
            border-radius: 0.75rem;
            box-shadow: 0 10px 30px rgba(16, 185, 129, 0.3);
            z-index: 10000;
            display: flex;
            align-items: center;
            gap: 0.75rem;
            animation: slideInRight 0.3s ease;
        `;
        toast.innerHTML = `
            <i class="bi bi-check-circle" style="font-size: 1.5rem;"></i>
            <div>
                <div style="font-weight: 600;">Sessione Focus completata!</div>
                <div style="font-size: 0.875rem; opacity: 0.9;">Ottimo lavoro!</div>
            </div>
        `;

        // Aggiungi animazione
        const styleSheet = document.createElement('style');
        styleSheet.textContent = `
            @keyframes slideInRight {
                from { transform: translateX(100%); opacity: 0; }
                to { transform: translateX(0); opacity: 1; }
            }
            @keyframes slideOutRight {
                from { transform: translateX(0); opacity: 1; }
                to { transform: translateX(100%); opacity: 0; }
            }
        `;
        document.head.appendChild(styleSheet);

        document.body.appendChild(toast);

        setTimeout(() => {
            toast.style.animation = 'slideOutRight 0.3s ease forwards';
            setTimeout(() => toast.remove(), 300);
        }, 5000);
    },

    // ==================== UI: FOCUS BANNER ====================
    renderFocusBanner() {
        const currentPage = this.getCurrentPageName();

        // Non mostrare sulla pagina focus stessa
        if (currentPage === 'focus.html') {
            return;
        }

        // Rimuovi banner esistente
        this.removeFocusBanner();

        // Crea banner
        const banner = document.createElement('div');
        banner.id = 'focusModeBanner';
        banner.innerHTML = `
            <div class="focus-banner-content">
                <div class="focus-banner-left">
                    <div class="focus-banner-pulse"></div>
                    <i class="bi bi-bullseye"></i>
                    <span class="focus-banner-text">
                        <strong>Focus Mode Attivo</strong>
                        <span class="focus-banner-time" id="focusBannerTime">00:00</span>
                    </span>
                </div>
                <div class="focus-banner-right">
                    <span class="focus-banner-xp" id="focusBannerXp">+0 XP</span>
                    <a href="focus.html" class="focus-banner-btn">
                        <i class="bi bi-arrow-return-left"></i>
                        Torna al Timer
                    </a>
                </div>
            </div>
        `;

        document.body.insertBefore(banner, document.body.firstChild);
        document.body.classList.add('focus-mode-active');

        this.updateBannerTime();
    },

    updateBannerTime() {
        const timeEl = document.getElementById('focusBannerTime');
        const xpEl = document.getElementById('focusBannerXp');

        if (!timeEl || !this.state) return;

        const remaining = this.state.remainingSeconds || 0;
        const minutes = Math.floor(remaining / 60);
        const seconds = remaining % 60;
        timeEl.textContent = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;

        if (xpEl) {
            xpEl.textContent = `+${this.state.earnedXp || 0} XP`;
        }
    },

    removeFocusBanner() {
        const banner = document.getElementById('focusModeBanner');
        if (banner) {
            banner.remove();
        }
        document.body.classList.remove('focus-mode-active');
    },

    // ==================== SESSION CONTROL ====================
    endSession() {
        localStorage.removeItem('focusState');
        this.state = null;
        this.removeFocusBanner();
        this.stopTimerSync();
    },

    async saveSessionToBackend() {
        if (!this.state || !this.state.elapsedSeconds) return;

        try {
            const token = localStorage.getItem('token');
            if (!token) return;

            await fetch('/api/gamification/focus-session', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    durationMinutes: Math.floor(this.state.elapsedSeconds / 60),
                    xpEarned: this.state.earnedXp || 0
                })
            });
        } catch (error) {
            console.error('Errore salvataggio sessione focus:', error);
        }
    }
};

// Inizializza quando il DOM è pronto
document.addEventListener('DOMContentLoaded', () => {
    FocusManager.init();
});

// Esporta per uso globale
window.FocusManager = FocusManager;