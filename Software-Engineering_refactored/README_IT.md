# Progetto Catan

## Descrizione del Progetto

Questo progetto e un'implementazione Java/Swing del gioco da tavolo **I Coloni di Catan**, sviluppata per il corso di Ingegneria del Software presso l'Universita degli Studi dell'Aquila (Prof.ssa Alessia Di Fonso).

Il codice segue un design orientato agli oggetti basato sugli artefatti UML del corso ed e organizzato con una struttura MVC a livelli:

- `config`: caricamento tecnico della configurazione.
- `controller`: controller applicativo leggero.
- `model`: logica di dominio e regole di gioco.
- `view`: interfaccia grafica Swing.

L'ultimo refactoring mantiene invariato il comportamento esterno del gioco, ma migliora la struttura interna applicando principi GRASP e pattern GoF: Strategy, State, carte polimorfiche, servizi dedicati e gerarchie di dominio piu chiare.

---

## Architettura

| Livello | Responsabilita |
|---|---|
| **config** | Carica `game_config.json` tramite Gson ed espone un oggetto `GameConfig` read-only |
| **controller** | Delega le operazioni della UI a `Game` senza contenere logica di dominio |
| **model** | Contiene stato partita, regole, mappa, giocatori, carte, commercio, costruzioni, achievement e decisioni dei bot |
| **view** | GUI Swing per setup, rendering della board, dialog e azioni dei giocatori |

`ConfigLoader` carica la configurazione una sola volta all'avvio della partita. Il `GameConfig` risultante viene iniettato nel model, evitando valori hardcoded nella logica di dominio.

`Game` resta il coordinatore principale del dominio, ma delega responsabilita specialistiche a collaboratori piu coesi come `BuildService`, `AchievementService`, gli stati concreti di `GameState`, le carte sviluppo concrete e le strategie dei bot.

---

## Struttura dei Package

```text
it.univaq.caffeine.catan
|-- Main
|-- config
|   |-- ConfigLoader
|   `-- GameConfig
|-- controller
|   `-- GameController
|-- model
|   |-- Game
|   |-- Bank
|   |-- TradeOffer
|   |-- board
|   |   |-- Edge
|   |   |-- GameMap
|   |   |-- HexagonalTile
|   |   |-- Intersection
|   |   |-- Port
|   |   |-- Robber
|   |   |-- TerrainRobber
|   |   `-- Token
|   |-- bot
|   |   |-- BotStrategy
|   |   |-- AbstractBotStrategy
|   |   |-- RandomBotStrategy
|   |   `-- StrategicBotStrategy
|   |-- cards
|   |   |-- CardPlayContext
|   |   |-- Deck
|   |   |-- DevelopmentCard
|   |   |-- DevelopmentCardFactory
|   |   |-- KnightCard
|   |   |-- MonopolyCard
|   |   |-- ResourceCard
|   |   |-- RoadBuildingCard
|   |   |-- VictoryPointCard
|   |   `-- YearOfPlentyCard
|   |-- enums
|   |   |-- BuildingType
|   |   |-- GamePhase
|   |   `-- ResourceType
|   |-- player
|   |   |-- Player
|   |   |-- HumanPlayer
|   |   |-- BotPlayer
|   |   |-- Building
|   |   |-- VertexBuilding
|   |   |-- Colony
|   |   |-- City
|   |   `-- Street
|   |-- rules
|   |   |-- CostRegistry
|   |   `-- GameRules
|   |-- services
|   |   |-- AchievementService
|   |   `-- BuildService
|   `-- state
|       |-- GameState
|       |-- SetupState
|       |-- InitialPlacementState
|       |-- ActiveState
|       |-- BuildPhaseState
|       |-- TradePhaseState
|       |-- RobberPhaseState
|       `-- GameOverState
`-- view
    |-- CatanBoardPanel
    |-- GameWindow
    `-- SetupWizard

CatanProject/Resources
`-- game_config.json
```

---

## Scelte di Design Principali

### Configurazione

`GameConfig` viene popolato da Gson a partire da `game_config.json`. I campi sono privati e accessibili solo tramite getter, quindi la configurazione e read-only dopo il caricamento.

### Controller

`GameController` e volutamente leggero. Riceve le richieste dalla UI Swing e le inoltra a `Game`, rispettando il ruolo GRASP Controller e tenendo le regole di dominio fuori da UI e controller.

### Gerarchia dei Giocatori

`Player` e ora una classe astratta. I tipi concreti sono:

- `HumanPlayer`
- `BotPlayer`

Questo elimina il vecchio design in cui una sola classe `Player` conteneva sia dati/comportamenti dei giocatori umani sia dati/comportamenti dei bot. Il comportamento bot appartiene ora a `BotPlayer` e alla sua strategia.

### Strategy Pattern per i Bot

La logica dei bot e implementata tramite l'interfaccia `BotStrategy` e strategie concrete:

- `RandomBotStrategy`: comportamento casuale/easy.
- `StrategicBotStrategy`: decisioni basate su priorita per piazzamento, costruzione, trade e robber.

Questa soluzione sostituisce la vecchia classe unica con controlli sulla difficolta tramite stringhe.

### Carte Sviluppo Polimorfiche

`DevelopmentCard` e astratta. Ogni tipo di carta implementa il proprio comportamento:

- `KnightCard`
- `MonopolyCard`
- `YearOfPlentyCard`
- `RoadBuildingCard`
- `VictoryPointCard`

`Deck` crea le carte tramite `DevelopmentCardFactory`, mentre `Game` le gioca polimorficamente tramite `card.play(...)`.

### State Pattern per le Fasi di Gioco

Le fasi della partita sono modellate con il pattern GoF State.

`Game` mantiene un riferimento a `GameState`, mentre gli stati concreti decidono quali transizioni sono valide:

- setup
- piazzamento iniziale
- turno attivo
- fase di costruzione
- fase di trade
- fase del robber
- game over

`GamePhase` resta come identificatore type-safe e valore di display, ma le transizioni sono delegate agli oggetti `GameState`.

### Gerarchia degli Edifici

La gerarchia separa gli elementi sugli edge da quelli sulle intersezioni:

- `Street` si trova su un `Edge` e non produce risorse.
- `VertexBuilding` si trova su una `Intersection`.
- `Colony` produce 1 risorsa.
- `City` produce 2 risorse.

`Game` puo chiedere a un generico `Building` la sua intersezione e la quantita prodotta senza controllare il tipo concreto.

### Servizi del Model

Due servizi riducono dimensione e responsabilita di `Game`:

- `BuildService`: piazzamento, costruzione, validazione e pagamento dei costi.
- `AchievementService`: Strada piu Lunga, Esercito piu Grande e relativi bonus VP.

### Astrazione del Robber

`Robber` e astratto e il gioco attuale usa `TerrainRobber`. Il model dipende dall'astrazione, quindi eventuali varianti future del robber possono essere aggiunte senza riscrivere il flusso principale.

---

## Funzionalita Implementate

### UC 1.0 - Setup di Gioco

Wizard di setup per modalita di gioco, numero giocatori, divisione umani/bot, difficolta bot e nomi dei giocatori.

### UC 2.0 - Piazzamento Iniziale

Piazzamento a serpentina di 2 colonie e 2 strade per giocatore, con distribuzione iniziale delle risorse e automazione dei bot.

### UC 3.0 - Esecuzione del Turno

Gestione completa del turno attivo con lancio dadi, costruzione, trade, carte sviluppo, robber e avanzamento turno.

### UC 3.1 - Produzione Risorse

Il lancio dei dadi distribuisce risorse ai giocatori con edifici sulle tile corrispondenti. Il robber blocca la produzione sulla propria tile.

### UC 3.1 EX.1 - Robber

Quando esce 7, i giocatori che superano il limite scartano meta risorse. Gli umani vengono gestiti tramite UI, mentre i bot scartano automaticamente. Il giocatore attivo sposta il robber e puo rubare da un avversario adiacente.

### UC 3.2 - Commercio Banca e Porto

Il trade supporta il cambio 4:1 della banca, i porti generici 3:1 e i porti specifici 2:1.

### UC 3.3 - Costruzione Strutture

I giocatori possono costruire colonie, citta e strade con controllo dei costi e validazione del piazzamento.

### UC 3.4 - Acquisto Carta Sviluppo

I giocatori possono acquistare carte dal mazzo sviluppo usando i costi configurati.

### UC 3.5 - Gioco Carta Sviluppo

Carte sviluppo supportate:

- Knight
- Monopoly
- Discovery / Year of Plenty
- Road Building
- carte Victory Point nascoste

### Vittoria e Carte Bonus

La vittoria viene controllata dopo le azioni che modificano i punti vittoria. Il gioco supporta VP nascosti, Strada piu Lunga ed Esercito piu Grande.

---

## Come Eseguire il Progetto

Il progetto e configurato per **Java 21** e puo essere eseguito direttamente da IntelliJ IDEA.

1. Aprire IntelliJ IDEA.
2. Selezionare **Open** e scegliere la cartella del progetto.
3. Contrassegnare `CatanProject/src` come **Sources Root**.
4. Contrassegnare `CatanProject/Resources` come **Resources Root**.
5. Configurare un SDK Java 21 se IntelliJ non lo rileva automaticamente.
6. Aggiungere il JAR di Gson alle librerie del progetto se non e gia configurato.
7. Aprire ed eseguire `CatanProject/src/it/univaq/caffeine/catan/Main.java`.

---

## Dipendenze

| Libreria | Scopo |
|---|---|
| **Gson** | Parsing JSON per `ConfigLoader` |
| **Java Swing** | Rendering della GUI desktop |

---

## Documentazione Aggiuntiva

- `README.md`: versione inglese della documentazione generale.
- `README_REFACTORING.md`: note tecniche cronologiche sul refactoring.
- `README_STUDIO.md`: guida di studio che spiega cosa e cambiato, perche e quali principi/pattern sono stati applicati.

---

## Autori

Sviluppato dal team **Caffeine Coders**:

- Lorenzo Rodorigo
- Mario Del Corvo
- Gennaro Ranalli

---

## Licenza

Questo progetto e destinato esclusivamente a scopi educativi.
