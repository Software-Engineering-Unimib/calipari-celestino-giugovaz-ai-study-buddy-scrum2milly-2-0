# 🎓 AI Study Buddy

<div align="center">

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Groq AI](https://img.shields.io/badge/Groq-LLaMA%203.3-F55036?style=for-the-badge&logo=meta&logoColor=white)
![License](https://img.shields.io/badge/License-Academic-blue?style=for-the-badge)

**Piattaforma web intelligente che sfrutta l'AI generativa per supportare gli studenti nello studio, fornendo spiegazioni personalizzate, quiz interattivi, flashcards e monitoraggio dei progressi.**

[Funzionalità](#-funzionalità) · [Architettura](#-architettura) · [Setup](#-setup-e-installazione) · [API](#-api-endpoints) · [Team](#-team)

</div>

---

## 📋 Panoramica

AI Study Buddy è un tutor digitale intelligente che si adatta al livello e alle esigenze dello studente. Attraverso l'integrazione con modelli di AI generativa (Groq LLaMA 3.3 70B), la piattaforma offre un'esperienza di apprendimento personalizzata e interattiva.

### Problema

Gli studenti universitari affrontano sfide significative nel loro percorso di apprendimento: difficoltà nella comprensione di concetti complessi senza supporto immediato, mancanza di feedback personalizzato e strumenti di autovalutazione efficaci.

### Soluzione

AI Study Buddy fornisce un supporto didattico personalizzato disponibile 24/7, con spiegazioni adattate al livello dello studente, strumenti di verifica automatici e un sistema di gamification che mantiene alta la motivazione.

---

## ✨ Funzionalità

### 🧠 Spiegazioni Personalizzate
- Generazione di spiegazioni AI adattate al livello di istruzione dello studente (Scuola Media, Superiore, Università)
- Supporto multilingua (Italiano, Inglese, Spagnolo, Francese, Tedesco, Portoghese, Russo)
- Storico delle spiegazioni con possibilità di rigenerazione

### 📝 Generazione Quiz
- Quiz a scelta multipla generati dall'AI su qualsiasi argomento
- Tre livelli di difficoltà: Principiante, Intermedio, Avanzato
- Da 5 a 20 domande per quiz
- Valutazione automatica con evidenziazione risposte corrette/sbagliate
- Possibilità di ripetere i quiz e tracciamento dello storico

### 🃏 Flashcards Intelligenti
- Generazione automatica di flashcards tramite AI
- Organizzazione in deck personalizzabili con colori e materie
- Modalità studio con flip card interattive
- Tracciamento del tasso di successo per ogni carta
- Algoritmo di studio che priorizza le carte meno conosciute

### 🏆 Sistema di Gamification
- Punti esperienza (XP) per ogni attività completata:
  - Spiegazioni richieste: +10 XP
  - Quiz completati: +20 XP base, +10 bonus se superato
  - Flashcards studiate: +2 XP per carta
- Sistema di livelli basato sui punti totali
- Badge e achievement sbloccabili
- Streak di studio giornaliero

### 📊 Dashboard Progressi
- Statistiche globali su deck, flashcards e sessioni di studio
- Monitoraggio del tasso di padronanza per ogni deck
- Conteggio sessioni e giorni consecutivi di studio

### 🎯 Sistema di Raccomandazioni
- Suggerimenti personalizzati basati sulle lacune rilevate
- Prioritizzazione intelligente degli argomenti da ripassare
- Raccomandazioni AI adattate al livello e alle performance dello studente

---

## 🏗 Architettura

Il progetto segue un'architettura a layer con separazione delle responsabilità:

```
┌─────────────────────────────────────────────────────┐
│                    Frontend                          │
│             HTML5 / CSS3 / JavaScript                │
│                   Bootstrap                          │
├─────────────────────────────────────────────────────┤
│               Presentation Layer                     │
│  ExplanationController · QuizController              │
│  FlashcardController · GamificationController        │
│  RecommendationController · LoginController          │
├─────────────────────────────────────────────────────┤
│                 Mapper Layer                         │
│         Entity ↔ DTO mapping e conversioni           │
├─────────────────────────────────────────────────────┤
│                Business Layer                        │
│  AIServiceImpl · QuizServiceImpl                     │
│  FlashcardServiceImpl · FlashcardDeckServiceImpl     │
│  UserServiceImpl · GamificationServiceImpl           │
│  ExplanationServiceImpl · RecommendationServiceImpl  │
├─────────────────────────────────────────────────────┤
│              Integration Layer                       │
│  GroqPrimaryClient · GroqFallbackClient              │
│  ResponseParser · AIClient (interface)               │
│  WebClientConfig                                     │
├─────────────────────────────────────────────────────┤
│               Data Access Layer                      │
│  UserRepository · QuizRepository                     │
│  FlashcardRepository · FlashcardDeckRepository       │
│  BadgeRepository · XpEventRepository                 │
├─────────────────────────────────────────────────────┤
│              Security Layer                          │
│  Spring Security · JWT Filter · CORS Config          │
├─────────────────────────────────────────────────────┤
│                   Database                           │
│            PostgreSQL (Supabase)                      │
└─────────────────────────────────────────────────────┘
```

### Design Pattern Utilizzati

| Pattern | Applicazione |
|---------|-------------|
| **Strategy** | `AIClient` interface con `GroqPrimaryClient` e `GroqFallbackClient` |
| **Chain of Responsibility** | `callAIWithFallback` per gestione fallback tra modelli AI |
| **Template Method** | Struttura base dei client Groq |
| **Adapter** | Controller che convertono JSON in oggetti Java |
| **Mapper** | Conversione bidirezionale tra entità JPA e DTO |
| **DTO** | Data Transfer Objects per separazione tra layer |
| **Repository** | Spring Data JPA repositories per accesso dati |
| **Dependency Injection** | Constructor Injection con IoC container di Spring |

---

## 🛠 Tech Stack

### Backend
| Tecnologia | Versione | Utilizzo |
|-----------|---------|---------|
| Java | 21 | Linguaggio principale |
| Spring Boot | 4.0.2 | Framework applicativo |
| Spring Security | - | Autenticazione e autorizzazione (JWT) |
| Spring Data JPA | - | Persistenza dati |
| Spring WebFlux | - | Client HTTP reattivo per API AI |
| Maven | - | Build system e dependency management |
| Gson | - | Parsing JSON risposte AI |

### Frontend
| Tecnologia | Utilizzo |
|-----------|---------|
| HTML5 / CSS3 | Struttura e stile |
| JavaScript (Vanilla) | Logica client-side |
| Bootstrap | Framework UI responsive |
| Bootstrap Icons | Iconografia |

### Database & Infrastruttura
| Tecnologia | Utilizzo |
|-----------|---------|
| PostgreSQL 14+ | Database relazionale |
| Supabase | Hosting database remoto |

### AI & Integrazioni
| Tecnologia | Utilizzo |
|-----------|---------|
| Groq API | Provider AI (OpenAI-compatible) |
| LLaMA 3.3 70B | Modello primario |
| LLaMA 3.1 8B | Modello fallback |

### Testing & Quality
| Strumento | Utilizzo |
|----------|---------|
| JUnit 5 | Unit testing |
| Mockito | Mocking framework |
| AssertJ | Assertion library |
| SonarQube / SonarCloud | Analisi qualità codice |
| Understand | Analisi antipattern |

---

## 🚀 Setup e Installazione

### Prerequisiti

- **Java 21** o superiore
- **Maven 3.8+**
- **PostgreSQL 14+** (o account Supabase)
- **API Key Groq** ([ottienila qui](https://console.groq.com))

### 1. Clona il repository

```bash
git clone https://github.com/your-username/studybuddy.git
cd studybuddy
```

### 2. Configura le variabili d'ambiente

Crea il file `src/main/resources/application.properties` oppure usa variabili d'ambiente:

```properties
# Database
spring.datasource.url=jdbc:postgresql://YOUR_HOST:5432/YOUR_DB
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
spring.jpa.hibernate.ddl-auto=update

# AI - Groq
ai.groq.api-key=YOUR_GROQ_API_KEY
ai.groq.model=llama-3.3-70b-versatile
ai.groq.fallback-model=llama-3.1-8b-instant
ai.groq.test-fallback=false

# JWT
jwt.secret=YOUR_JWT_SECRET
jwt.expiration=86400000
```

### 3. Build e avvio

```bash
# Build del progetto
mvn clean install

# Avvio dell'applicazione
mvn spring-boot:run
```

L'applicazione sarà disponibile su `http://localhost:8080`

### 4. Verifica

```bash
# Health check
curl http://localhost:8080/api/ai/health
# Output atteso: OK
```

---

## 📡 API Endpoints

### Autenticazione

| Metodo | Endpoint | Descrizione |
|--------|---------|-------------|
| `POST` | `/api/auth/register` | Registrazione nuovo utente |
| `POST` | `/api/auth/login` | Login e ottenimento JWT |

### Spiegazioni AI

| Metodo | Endpoint | Descrizione |
|--------|---------|-------------|
| `GET` | `/api/ai/explain` | Genera spiegazione personalizzata |
| `GET` | `/api/ai/health` | Health check servizio AI |

### Quiz

| Metodo | Endpoint | Descrizione |
|--------|---------|-------------|
| `POST` | `/api/ai/quiz/generate` | Genera un nuovo quiz |
| `POST` | `/api/ai/quiz/{quizId}/start` | Inizia un quiz |
| `POST` | `/api/ai/quiz/submit` | Invia risposte del quiz |
| `GET` | `/api/ai/quiz/{quizId}` | Ottieni quiz specifico |
| `GET` | `/api/ai/quiz/my` | Ottieni tutti i quiz dell'utente |
| `GET` | `/api/ai/quiz/completed` | Ottieni quiz completati |
| `GET` | `/api/ai/quiz/stats` | Statistiche quiz |
| `POST` | `/api/ai/quiz/{quizId}/retry` | Ripeti un quiz |
| `DELETE` | `/api/ai/quiz/{quizId}` | Elimina un quiz |

### Flashcards

| Metodo | Endpoint | Descrizione |
|--------|---------|-------------|
| `POST` | `/api/flashcards/generate` | Genera flashcards con AI |
| `GET` | `/api/flashcards/decks` | Ottieni tutti i deck |
| `POST` | `/api/flashcards/decks` | Crea nuovo deck |
| `GET` | `/api/flashcards/decks/{deckId}` | Ottieni deck specifico |
| `PUT` | `/api/flashcards/decks/{deckId}` | Aggiorna deck |
| `DELETE` | `/api/flashcards/decks/{deckId}` | Elimina deck |
| `GET` | `/api/flashcards/decks/{deckId}/cards` | Ottieni carte del deck |
| `POST` | `/api/flashcards/decks/{deckId}/cards` | Crea flashcard manuale |
| `POST` | `/api/flashcards/cards/{cardId}/review` | Registra revisione |
| `POST` | `/api/flashcards/decks/{deckId}/study` | Registra sessione studio |
| `GET` | `/api/flashcards/decks/{deckId}/study-session` | Ottieni carte per studio |
| `GET` | `/api/flashcards/decks/stats` | Statistiche globali deck |

### Gamification

| Metodo | Endpoint | Descrizione |
|--------|---------|-------------|
| `GET` | `/api/gamification/stats` | Statistiche gamification utente |
| `GET` | `/api/gamification/badges` | Badge dell'utente |

> **Nota:** Tutti gli endpoint (eccetto auth) richiedono il token JWT nell'header `Authorization: Bearer <token>`

---

## 📁 Struttura del Progetto

```
studybuddy/
├── src/
│   ├── main/
│   │   ├── java/com/ai/studybuddy/
│   │   │   ├── config/                  # Configurazione generale
│   │   │   │   ├── integration/         # AIClient interface e implementazioni Groq
│   │   │   │   ├── security/            # Spring Security, JWT, filtri autenticazione
│   │   │   │   └── WebClientConfig.java # Configurazione WebClient per API esterne
│   │   │   ├── controller/              # REST Controllers
│   │   │   │   ├── login/               # Controller autenticazione e registrazione
│   │   │   │   ├── ExplanationController.java
│   │   │   │   ├── FlashcardController.java
│   │   │   │   ├── GamificationController.java
│   │   │   │   ├── QuizController.java
│   │   │   │   └── RecommendationController.java
│   │   │   ├── dto/                     # Data Transfer Objects (request/response)
│   │   │   ├── exception/               # Eccezioni custom (AIServiceException, etc.)
│   │   │   ├── mapper/                  # Mapper entità ↔ DTO
│   │   │   ├── model/                   # Entità JPA
│   │   │   │   ├── flashcard/           # Flashcard, FlashcardDeck
│   │   │   │   ├── gamification/        # Badge, XpEvent, livelli
│   │   │   │   ├── quiz/                # Quiz, Question
│   │   │   │   ├── recommendation/      # Raccomandazioni di studio
│   │   │   │   └── user/                # User, profilo studente
│   │   │   ├── repository/              # Spring Data JPA Repositories
│   │   │   ├── service/
│   │   │   │   ├── impl/                # Implementazioni servizi
│   │   │   │   └── inter/               # Interfacce servizi (contratti)
│   │   │   └── util/                    # Utility e costanti
│   │   │       ├── enums/               # Enum (DifficultyLevel, EducationLevel, etc.)
│   │   │       └── Const.java           # Costanti globali dell'applicazione
│   │   └── resources/
│   │       ├── static/                  # Frontend (HTML, CSS, JS)
│   │       ├── messages.properties      # i18n - Italiano (default)
│   │       ├── messages_en.properties   # i18n - Inglese
│   │       ├── messages_es.properties   # i18n - Spagnolo
│   │       └── application.properties
│   └── test/                            # Test unitari (JUnit 5 + Mockito)
├── pom.xml
└── README.md
```

---

## 🔒 Sicurezza

- **Autenticazione JWT** con token firmato e scadenza configurabile
- **Spring Security** per protezione degli endpoint
- **Password hashing** con BCrypt
- **Validazione input** con Jakarta Bean Validation
- **Controllo di ownership** su tutte le risorse (deck, flashcards, quiz)
- **Gestione sessione** lato client con controllo scadenza token e refresh automatico

---

## 🧪 Testing

```bash
# Esegui tutti i test
mvn test

# Esegui test con report di copertura
mvn test jacoco:report
```

I test sono implementati con JUnit 5 e Mockito, con copertura su:
- Service layer (UserServiceImpl, FlashcardServiceImpl, etc.)
- Business logic (calcolo XP, livelli, streak)
- Gestione errori e edge cases

---

## 👥 Team

**Scrum2Milly 2.0** — Università degli Studi di Milano-Bicocca

| Nome | Matricola | Email |
|------|----------|-------|
| **Mario Calipari** | 916010 | m.calipari@campus.unimib.it |
| **Andrea Celestino** | 914472 | a.celestino2@campus.unimib.it |
| **Giovanni Giugovaz** | 909392 | g.giugovaz@campus.unimib.it |

---

## 📄 Documentazione

Il progetto include documentazione completa secondo la metodologia Scrum:

- **Casi d'Uso** testuali e diagrammatici (UC1: RegistraStudente, UC2: RichiediSpiegazione, UC3: GeneraQuiz)
- **Diagrammi di Sequenza** di Sistema (SSD) e di progetto (SD)
- **Modello delle Classi Concettuali**
- **Diagrammi delle Classi Software** di Progetto (controller, service, integration, model, dto, repository)
- **Diagrammi delle Attività** e **Macchine a Stati**
- **Architettura Logica** del sistema
- **Specifica Supplementare**, **Visione** e **Glossario**
- **Contratti** delle operazioni di sistema

---

## 📜 Licenza

Progetto accademico sviluppato per il corso di Ingegneria del Software — Università degli Studi di Milano-Bicocca, A.A. 2025/2026.

---

<div align="center">

Sviluppato con ❤️ da **Scrum2Milly 2.0**

</div>
