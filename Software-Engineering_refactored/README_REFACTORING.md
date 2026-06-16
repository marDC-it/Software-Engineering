# Refactoring Catan Java Swing

Questo documento riassume le modifiche applicate al progetto Catan desktop in Java/Swing.

Nota: `Main.java` non e stato modificato. Il flusso di avvio dell'applicazione resta quello esistente.

## Obiettivo del refactoring

Il refactoring e stato fatto per migliorare la struttura object-oriented del progetto, ridurre i controlli basati su stringhe o tipi, e applicare in modo piu coerente principi GRASP e pattern GoF.

Gli obiettivi principali erano:

- separare giocatori umani e bot;
- spostare il comportamento dei bot in strategie intercambiabili;
- rendere le carte sviluppo polimorfiche;
- sostituire gli stati di gioco string-based con un enum;
- pulire la gerarchia degli edifici;
- mantenere il `GameController` leggero;
- preservare il comportamento esterno dell'applicazione.

## File non modificati

Il file seguente non e stato toccato:

- `CatanProject/src/it/univaq/caffeine/catan/Main.java`

Anche le classi di configurazione sono rimaste separate dalla logica di dominio:

- `ConfigLoader`
- `GameConfig`

## Player hierarchy

### Prima

La classe `Player` rappresentava sia giocatori umani sia bot.

Conteneva:

- dati comuni del giocatore;
- campo booleano per distinguere i bot;
- riferimento a `BotStrategy`;
- costruttore per giocatore umano;
- costruttore per bot.

Questo creava accoppiamento e campi inutili per i giocatori umani.

### Dopo

`Player` e ora una classe astratta.

Sono state aggiunte:

- `HumanPlayer extends Player`
- `BotPlayer extends Player`

`Player` ora contiene solo dati e comportamenti comuni:

- nome;
- colore;
- risorse;
- carte sviluppo;
- edifici;
- punti vittoria;
- cavalieri giocati;
- pezzi disponibili;
- metodi per aggiungere/rimuovere risorse;
- metodi per aggiungere edifici e carte;
- metodi comuni di scoring e inventario.

### Nuove classi

- `CatanProject/src/it/univaq/caffeine/catan/model/player/HumanPlayer.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/player/BotPlayer.java`

### Campi rimossi da `Player`

- campo booleano `isBot`;
- campo `BotStrategy`;
- costruttore bot dentro `Player`.

### Comportamento polimorfico aggiunto

`Player` espone un hook:

```java
handleRobberDiscard(Bank bank, int cardsToDiscard, Map<Player, Integer> humanDiscards)
```

`HumanPlayer` aggiunge la richiesta di scarto alla mappa gestita dalla UI.

`BotPlayer` scarta automaticamente risorse casuali.

Questo elimina il controllo diretto del tipo giocatore nella logica del robber.

## Bot Strategy Pattern

### Prima

Esisteva una singola classe:

- `it.univaq.caffeine.catan.model.BotStrategy`

Questa classe conteneva sia la logica random sia quella strategica, scegliendo il comportamento tramite stringhe come `"Strategic"`.

### Dopo

La vecchia classe `model/BotStrategy.java` e stata rimossa.

E stato creato il package:

- `it.univaq.caffeine.catan.model.bot`

Con queste classi:

- `BotStrategy`
- `AbstractBotStrategy`
- `RandomBotStrategy`
- `StrategicBotStrategy`

### Responsabilita

`BotStrategy` definisce il contratto per le decisioni del bot:

- scelta della colonia iniziale;
- scelta della strada iniziale;
- scelta dell'azione nel turno;
- scelta dello scambio;
- scelta della tessera per il robber;
- reset del turno;
- recupero delle informazioni sull'ultimo trade.

`RandomBotStrategy` contiene la logica casuale.

`StrategicBotStrategy` contiene la logica con priorita:

1. City
2. Colony
3. Development card
4. Street

`BotPlayer` possiede la strategia e delega le decisioni.

## Game phases

### Prima

Gli stati di gioco erano gestiti con stringhe:

- `"Setup"`
- `"InitialPlacement"`
- `"ActiveGame"`
- `"GameOver"`

Queste stringhe venivano confrontate in piu punti del model e della view.

### Dopo

E stato aggiunto l'enum:

- `CatanProject/src/it/univaq/caffeine/catan/model/enums/GamePhase.java`

Valori:

- `SETUP`
- `INITIAL_PLACEMENT`
- `ACTIVE`
- `ROBBER_PHASE`
- `TRADE_PHASE`
- `BUILD_PHASE`
- `GAME_OVER`

`Game` ora mantiene:

```java
private GamePhase phase;
```

La logica usa:

```java
game.getPhase()
```

e non confronti string-based.

`getState()` e stato mantenuto solo come metodo di compatibilita/display per la UI.

## Building hierarchy

### Prima

La gerarchia esistente era:

- `Building`
- `Street`
- `Colony`
- `City`

`Colony` e `City` duplicavano il riferimento a `Intersection`.

### Dopo

E stata introdotta:

- `VertexBuilding extends Building`

La gerarchia ora e:

- `Building`
  - `Street`
  - `VertexBuilding`
    - `Colony`
    - `City`

### Modifiche principali

`Building` ora espone:

- owner;
- color;
- `getBuildingType()`;
- `getCost(CostRegistry)`;
- `getVictoryPoints()`.

`Street` resta associata a `Edge`.

`VertexBuilding` gestisce:

- associazione con `Intersection`;
- produzione risorse.

`Colony` produce 1 risorsa.

`City` produce 2 risorse.

La distribuzione risorse in `Game` usa ora `VertexBuilding.getResourceProductionAmount()` invece di controllare direttamente `Colony` o `City`.

## Development cards

### Prima

`DevelopmentCard` era una classe concreta con un enum interno `CardType`.

La logica di gioco selezionava il comportamento con controlli sul tipo carta, per esempio:

- Knight;
- Monopoly;
- Discovery;
- Road Building;
- Victory Point.

La view usava uno switch sul tipo della carta selezionata.

### Dopo

`DevelopmentCard` e ora una classe astratta.

Sono state aggiunte carte concrete:

- `KnightCard`
- `MonopolyCard`
- `YearOfPlentyCard`
- `RoadBuildingCard`
- `VictoryPointCard`

Sono state aggiunte anche:

- `CardPlayContext`
- `DevelopmentCardFactory`

### Nuovo flusso

La UI raccoglie gli input necessari e crea un `CardPlayContext`.

Poi chiama:

```java
gc.playDevelopmentCard(selectedCard, context);
```

Il controller delega a `Game`.

`Game` chiama:

```java
selectedCard.play(game, currentPlayer, context);
```

Ogni carta applica il proprio effetto.

### Effetti delle carte

`KnightCard`:

- incrementa il conteggio dei cavalieri;
- aggiorna Largest Army;
- richiede lo spostamento del robber.

`MonopolyCard`:

- usa una risorsa scelta;
- prende tutte le risorse di quel tipo dagli altri giocatori.

`YearOfPlentyCard`:

- usa due risorse scelte;
- trasferisce due risorse dalla banca al giocatore.

`RoadBuildingCard`:

- imposta due strade gratuite da piazzare.

`VictoryPointCard`:

- non ha effetto attivo;
- vale 1 punto vittoria nascosto.

## Deck

`Deck` non crea piu carte generiche con un tipo.

Ora usa:

```java
DevelopmentCardFactory.create(typeName)
```

Questo permette di avere oggetti carta concreti e polimorfici.

## Game

`Game.java` e stato aggiornato in piu punti.

### Setup giocatori

La creazione giocatori ora usa:

- `HumanPlayer`
- `BotPlayer`

### Stato partita

Lo stato e stato sostituito da `GamePhase`.

### Carte sviluppo

La logica delle carte e stata delegata alle classi concrete.

Sono stati aggiunti metodi applicativi usati dalle carte:

- `applyKnightCard`
- `applyMonopolyCard`
- `applyYearOfPlentyCard`
- `applyRoadBuildingCard`

### Robber discard

Lo scarto risorse da robber usa polimorfismo su `Player`.

Non c'e piu un controllo diretto nel model del tipo:

```java
if (p.isBot()) ...
```

## GameController

`GameController` resta un controller leggero.

E stato aggiunto:

```java
playDevelopmentCard(DevelopmentCard card, CardPlayContext context)
```

I metodi preesistenti per le carte sono stati mantenuti come wrapper di compatibilita.

Il controller non contiene:

- algoritmi bot;
- switch sulle carte;
- logica dettagliata di costruzione;
- caricamento diretto dentro `Game`.

## Swing view

Sono stati aggiornati:

- `GameWindow`
- `CatanBoardPanel`
- `SetupWizard`

### GameWindow

La selezione carta non usa piu switch sul tipo.

Ora:

- filtra le carte giocabili tramite `canBePlayed`;
- chiede alla carta il tipo di input necessario;
- costruisce un `CardPlayContext`;
- chiama `playDevelopmentCard`.

### CatanBoardPanel

Il bot auto-play ora lavora con `BotPlayer`.

Le decisioni bot vengono delegate al bot:

- `chooseInitialPlacement`
- `chooseInitialRoad`
- `chooseAction`
- `chooseRobberTile`

### SetupWizard

Il riepilogo setup usa il display polimorfico del giocatore, invece di ricostruire direttamente la distinzione umano/bot.

## File modificati

File modificati:

- `CatanProject/src/it/univaq/caffeine/catan/controller/GameController.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/Game.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/board/Intersection.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/cards/Deck.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/cards/DevelopmentCard.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/player/Building.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/player/City.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/player/Colony.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/player/Player.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/player/Street.java`
- `CatanProject/src/it/univaq/caffeine/catan/view/CatanBoardPanel.java`
- `CatanProject/src/it/univaq/caffeine/catan/view/GameWindow.java`
- `CatanProject/src/it/univaq/caffeine/catan/view/SetupWizard.java`

File rimosso:

- `CatanProject/src/it/univaq/caffeine/catan/model/BotStrategy.java`

File aggiunti:

- `CatanProject/src/it/univaq/caffeine/catan/model/bot/BotStrategy.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/bot/AbstractBotStrategy.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/bot/RandomBotStrategy.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/bot/StrategicBotStrategy.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/cards/CardPlayContext.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/cards/DevelopmentCardFactory.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/cards/KnightCard.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/cards/MonopolyCard.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/cards/RoadBuildingCard.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/cards/VictoryPointCard.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/cards/YearOfPlentyCard.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/enums/GamePhase.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/player/BotPlayer.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/player/HumanPlayer.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/player/VertexBuilding.java`

## Pattern e principi applicati

Pattern applicati:

- Strategy Pattern per i bot;
- Replace Conditional with Polymorphism per le carte sviluppo;
- uso di enum type-safe per le fasi di gioco.

Principi GRASP applicati:

- Polymorphism;
- Protected Variations;
- Low Coupling;
- High Cohesion;
- Information Expert.

## Verifiche eseguite

E stata eseguita una compilazione completa con `javac`.

Risultato:

```text
Compilazione riuscita
```

E stato eseguito anche uno smoke test headless con JShell per verificare:

- caricamento configurazione;
- creazione `Game`;
- setup giocatori;
- creazione di player umani e bot;
- passaggio alla fase `InitialPlacement`.

## Rischi residui

Non era presente una test suite automatica.

La UI Swing non e stata avviata manualmente durante la verifica. Il codice e stato compilato e il model e stato controllato con uno smoke test, ma resta utile fare un test manuale completo dei flussi:

- setup partita;
- piazzamento iniziale;
- turno umano;
- turno bot;
- acquisto carta sviluppo;
- gioco di ogni tipo di carta sviluppo;
- robber;
- trade;
- costruzione strada, colonia e citta.

## REFACTORING 2

Questa sezione documenta il secondo gruppo di correzioni applicate al progetto.

Gli obiettivi erano:

- eliminare gli accessi diretti ai campi di `GameConfig`;
- rendere la configurazione read-only dopo il caricamento via Gson;
- completare il polimorfismo della gerarchia `Building`;
- rimuovere gli `instanceof VertexBuilding` da `Game`;
- ridurre la responsabilita diretta di `Game` tramite servizi dedicati;
- rendere esplicita la natura dei giocatori nella gerarchia `Player`.

### Incapsulamento di GameConfig

#### Prima

`GameConfig` esponeva molti campi pubblici, per esempio:

```java
public int minPlayers;
public int maxPlayers;
public List<String> gameModes;
public Map<String, Integer> deckComposition;
```

Anche la classe interna `PortDefinition` esponeva campi pubblici:

```java
public String resourceType;
public int rate;
public double x;
public double y;
```

Questo permetteva a model, controller e view di accedere direttamente alla struttura interna della configurazione.

#### Dopo

Tutti i campi di `GameConfig` sono stati resi `private`.

Sono stati aggiunti solo getter pubblici, senza setter, per mantenere la configurazione read-only dopo il caricamento da `game_config.json` tramite Gson.

Esempi:

```java
public int getVictoryPointsGoal() { return victoryPointsGoal; }
public List<String> getPlayerColors() { return playerColors; }
public Map<String, Integer> getDeckComposition() { return deckComposition; }
public int getBankDefaultRate() { return bankDefaultRate; }
```

La stessa modifica e stata applicata a `GameConfig.PortDefinition`:

```java
public String getResourceType() { return resourceType; }
public int getRate() { return rate; }
public double getX() { return x; }
public double getY() { return y; }
```

#### Accessi aggiornati

Gli accessi diretti sono stati sostituiti con getter.

Esempi:

```java
cfg.victoryPointsGoal
```

e diventato:

```java
cfg.getVictoryPointsGoal()
```

```java
game.getConfig().deckComposition
```

e diventato:

```java
game.getConfig().getDeckComposition()
```

```java
def.resourceType
```

e diventato:

```java
def.getResourceType()
```

#### File aggiornati per GameConfig

- `CatanProject/src/it/univaq/caffeine/catan/config/GameConfig.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/Game.java`
- `CatanProject/src/it/univaq/caffeine/catan/controller/GameController.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/board/GameMap.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/cards/Deck.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/Bank.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/rules/GameRules.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/rules/CostRegistry.java`
- `CatanProject/src/it/univaq/caffeine/catan/view/SetupWizard.java`
- `CatanProject/src/it/univaq/caffeine/catan/view/GameWindow.java`

### Polimorfismo completo della gerarchia Building

#### Problema

`Game.java` conteneva controlli espliciti sul tipo `VertexBuilding` in piu punti.

Le occorrenze principali erano:

```java
if (b instanceof VertexBuilding vertexBuilding) {
    Intersection inter = vertexBuilding.getIntersection();
}
```

```java
if (building instanceof VertexBuilding vb) {
    bank.transferResourcesTo(vb.getOwner(), resource, vb.getResourceProductionAmount());
}
```

```java
if (!(b instanceof VertexBuilding vb)) continue;
Intersection inter = vb.getIntersection();
```

La causa era che `Building` non esponeva i metodi comuni necessari per usare il polimorfismo.

#### Dopo

`Building` ora dichiara metodi polimorfici con valori neutri:

```java
public int getProductionAmount() { return 0; }
public Intersection getIntersection() { return null; }
```

Il significato e:

- `Street` produce `0` risorse;
- `Street` non ha intersezione, quindi restituisce `null`;
- `Colony` produce `1` risorsa;
- `City` produce `2` risorse;
- `Colony` e `City` ereditano la gestione dell'intersezione da `VertexBuilding`.

`VertexBuilding` ora ridefinisce `getIntersection()` e dichiara astratto:

```java
public abstract int getProductionAmount();
```

`Colony` implementa:

```java
public int getProductionAmount() { return 1; }
```

`City` implementa:

```java
public int getProductionAmount() { return 2; }
```

Il vecchio metodo `getResourceProductionAmount()` e stato rimosso.

#### Effetti su Game.java

`distributeInitialResources()` non usa piu `instanceof VertexBuilding`.

Ora usa:

```java
Intersection inter = b.getIntersection();
if (inter == null) continue;
```

`distributeResources()` non usa piu `VertexBuilding`.

Ora usa:

```java
Building building = inter.getBuilding();
if (building == null) continue;
int amount = building.getProductionAmount();
if (amount <= 0) continue;

bank.transferResourcesTo(
        building.getOwner(),
        resource,
        amount
);
```

`getEffectiveTradeRate()` non usa piu `instanceof VertexBuilding`.

Ora usa:

```java
Intersection inter = b.getIntersection();
if (inter == null) continue;
```

Questo applica GRASP Polymorphism in modo completo: `Game` non deve piu scoprire il tipo concreto dell'edificio per sapere se produce risorse o se si trova su una intersezione.

### Estrazione di BuildService

#### Problema

`Game.java` conteneva direttamente la logica di costruzione e piazzamento:

- piazzamento colonia iniziale;
- piazzamento strada iniziale;
- costruzione colonia durante il turno;
- upgrade colonia in citta;
- costruzione strada;
- costruzione strada gratuita tramite Road Building;
- validazioni e pagamento costi.

Questo aumentava le responsabilita di `Game` e rendeva la classe piu difficile da testare.

#### Dopo

E stata aggiunta la classe:

- `CatanProject/src/it/univaq/caffeine/catan/model/services/BuildService.java`

`BuildService` riceve:

- `GameConfig`;
- `GameMap`;
- `Bank`;
- `GameRules`;
- `CostRegistry`.

Responsabilita principali:

- `placeInitialColony(...)`;
- `placeInitialRoad(...)`;
- `build(...)`;
- `buildFreeStreet(...)`;
- validazione disponibilita pezzi;
- validazione connessione alla rete strade;
- gestione pagamento costi;
- creazione di `Colony`, `City`, `Street`.

`Game` mantiene la responsabilita di orchestrare il flusso del turno, ma delega la logica specialistica a `BuildService`.

Esempio:

```java
buildService.build(player, buildingType, locationId);
```

### Estrazione di AchievementService

#### Problema

`Game.java` conteneva direttamente la logica degli achievement:

- Largest Army;
- Longest Road;
- calcolo DFS della strada piu lunga;
- proprietari correnti degli achievement;
- bonus punti vittoria.

Questa logica era molto specifica e aumentava la dimensione e la complessita di `Game`.

#### Dopo

E stata aggiunta la classe:

- `CatanProject/src/it/univaq/caffeine/catan/model/services/AchievementService.java`

`AchievementService` gestisce:

- `checkLargestArmy(Player player)`;
- `checkLongestRoad(Player player)`;
- `getLongestRoadOwner()`;
- `getLargestArmyOwner()`;
- calcolo DFS della strada piu lunga.

`Game` ora delega:

```java
achievementService.checkLongestRoad(player);
achievementService.checkLargestArmy(player);
```

I getter pubblici di `Game` sono stati mantenuti per non cambiare l'API usata da controller e view:

```java
public Player getLongestRoadOwner()
public Player getLargestArmyOwner()
```

ma internamente delegano ad `AchievementService`.

Questa modifica applica GRASP Pure Fabrication e High Cohesion: la logica degli achievement e concentrata in una classe dedicata invece di restare dentro `Game`.

### Game.java dopo REFACTORING 2

`Game.java` e passato da una classe con molte responsabilita dirette a un orchestratore piu leggero.

Responsabilita rimaste in `Game`:

- setup generale della partita;
- turno corrente e fase di gioco;
- orchestrazione di build, trade, robber, carte sviluppo e vittoria;
- accesso agli oggetti principali del model.

Responsabilita delegate:

- costruzioni e piazzamenti: `BuildService`;
- achievement e Longest Road DFS: `AchievementService`.

Dimensioni dopo l'intervento:

```text
Game.java: 582 righe
BuildService.java: 219 righe
AchievementService.java: 114 righe
```

### Player.isBot astratto

#### Problema

`Player` aveva un default silenzioso:

```java
public boolean isBot() { return false; }
```

Questo era rischioso: una nuova sottoclasse di `Player` avrebbe potuto dimenticare di ridefinire `isBot()` e comportarsi come giocatore umano senza errore di compilazione.

#### Dopo

`Player` ora dichiara:

```java
public abstract boolean isBot();
```

`HumanPlayer` implementa esplicitamente:

```java
public boolean isBot() { return false; }
```

`BotPlayer` implementa esplicitamente:

```java
public boolean isBot() { return true; }
```

Il compilatore ora obbliga ogni futura sottoclasse di `Player` a dichiarare la propria natura.

Questo rafforza GRASP Polymorphism e riduce il rischio di bug silenziosi.

### Robber astratto e TerrainRobber

#### Problema

Nel progetto attuale il robber era una classe concreta:

```java
public class Robber
```

La classe rappresentava direttamente il robber terrestre, cioe quello che occupa una `HexagonalTile`.

Questo funzionava per il gioco attuale, ma rendeva il model meno pronto a varianti future. Nei Coloni di Catan esistono anche estensioni con una variante marina del robber/pirata. Il progetto non implementa questa regola, ma il design puo comunque essere preparato per l'estensione senza creare classi inutilizzate.

#### Dopo

`Robber` e ora una classe astratta.

Espone il contratto comune:

```java
public abstract boolean occupies(HexagonalTile tile);
public abstract void moveTo(HexagonalTile tile);
public abstract HexagonalTile getCurrentTile();
```

Il comportamento concreto attuale e stato spostato in:

- `CatanProject/src/it/univaq/caffeine/catan/model/board/TerrainRobber.java`

`TerrainRobber` e l'unica implementazione concreta usata dal gioco corrente.

Non e stata creata una classe `SeaRobber`.

Questa scelta e intenzionale: il gioco attuale non implementa regole marine, quindi creare una classe `SeaRobber` vuota avrebbe introdotto una falsa astrazione. Il sistema e pero pronto per una futura estensione, perche `Game` dipende dal tipo astratto `Robber` e non dalla classe concreta.

#### Effetti su Game.java

Il campo resta astratto:

```java
private Robber robber;
```

Durante il setup viene creata solo l'implementazione terrestre:

```java
robber = new TerrainRobber(desert);
```

I controlli diretti sulla tile corrente sono stati sostituiti da un metodo polimorfico:

```java
robber.occupies(tile)
```

Esempio nella distribuzione risorse:

```java
if (robber != null && robber.occupies(tile)) continue;
```

Esempio nel movimento:

```java
if (robber.occupies(tile))
    throw new IllegalStateException("Cannot place Robber on its current tile.");
robber.moveTo(tile);
```

#### Effetti sulla view

`CatanBoardPanel` non confronta piu direttamente:

```java
robber.getCurrentTile() == tile
```

ma usa:

```java
robber.occupies(tile)
```

Questo evita che la view dipenda dal dettaglio interno della posizione del robber.

#### Principio applicato

Questa modifica applica GRASP Polymorphism e Protected Variations.

Il codice attuale continua a usare solo il robber terrestre, ma una futura variante potra essere introdotta come nuova sottoclasse di `Robber` senza riscrivere la logica principale di `Game`.

### File aggiunti in REFACTORING 2

- `CatanProject/src/it/univaq/caffeine/catan/model/services/BuildService.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/services/AchievementService.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/board/TerrainRobber.java`

### File modificati in REFACTORING 2

- `CatanProject/src/it/univaq/caffeine/catan/config/GameConfig.java`
- `CatanProject/src/it/univaq/caffeine/catan/controller/GameController.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/Game.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/Bank.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/board/Robber.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/board/GameMap.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/cards/Deck.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/player/Building.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/player/VertexBuilding.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/player/Colony.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/player/City.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/player/Player.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/player/HumanPlayer.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/player/BotPlayer.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/rules/GameRules.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/rules/CostRegistry.java`
- `CatanProject/src/it/univaq/caffeine/catan/view/CatanBoardPanel.java`
- `CatanProject/src/it/univaq/caffeine/catan/view/SetupWizard.java`
- `CatanProject/src/it/univaq/caffeine/catan/view/GameWindow.java`

### Pattern e principi applicati in REFACTORING 2

Pattern e tecniche:

- Encapsulation per `GameConfig`;
- Replace Conditional with Polymorphism nella gerarchia `Building`;
- Extract Class per `BuildService`;
- Extract Class per `AchievementService`;
- astrazione del robber con implementazione concreta `TerrainRobber`;
- eliminazione di type checks non necessari su `VertexBuilding`.

Principi GRASP:

- Polymorphism;
- Pure Fabrication;
- High Cohesion;
- Low Coupling;
- Information Expert;
- Protected Variations.

### Verifiche eseguite in REFACTORING 2

E stata eseguita una compilazione completa con `javac` includendo Gson `2.10.1` nel classpath.

Risultato:

```text
Compilazione riuscita
```

Sono state eseguite ricerche mirate per verificare che non restassero accessi o pattern non voluti.

Verifiche completate:

- nessuna occorrenza residua di `instanceof VertexBuilding`;
- nessuna occorrenza residua di `getResourceProductionAmount()`;
- nessun default `isBot() { return false; }` in `Player`;
- nessun accesso diretto residuo ai campi principali di `GameConfig`;
- nessun accesso diretto residuo ai campi di `PortDefinition`;
- nessun campo pubblico rimasto in `GameConfig`;
- nessuna occorrenza residua di `new Robber(...)`;
- nessun confronto diretto residuo con `getCurrentTile() == ...`;
- nessuna classe `SeaRobber` creata.

### Note finali REFACTORING 2

`Main.java` non e stato modificato.

Il comportamento esterno del gioco e stato preservato: le API pubbliche usate da controller e view sono rimaste disponibili, mentre la logica interna e stata spostata verso classi piu coese.

## REFACTORING 3

Questa sezione documenta la correzione della separazione tra fase di gioco di dominio e stato locale della board Swing.

### Obiettivo

Nel codice precedente esistevano due concetti simili:

- `GamePhase` nel model;
- `CatanBoardPanel.Phase` nella view.

La distinzione era corretta a livello concettuale, ma il nome `Phase` nella view creava ambiguita e alcune sottofasi presenti in `GamePhase` non erano realmente governate dal model.

L'obiettivo di questo intervento e stato:

- rendere effettive le sottofasi `ROBBER_PHASE`, `TRADE_PHASE` e `BUILD_PHASE` nel model/controller;
- lasciare alla view solo una modalita locale di interazione con la board;
- evitare che la view sia l'unica fonte di verita per costruzione, trade o robber;
- mantenere invariato il comportamento visibile dell'applicazione Swing.

### GamePhase ora governa le sottofasi di dominio

`GamePhase` resta nel package model:

- `CatanProject/src/it/univaq/caffeine/catan/model/enums/GamePhase.java`

Le fasi ora rappresentano lo stato regolamentare della partita:

- `SETUP`
- `INITIAL_PLACEMENT`
- `ACTIVE`
- `ROBBER_PHASE`
- `TRADE_PHASE`
- `BUILD_PHASE`
- `GAME_OVER`

E stato aggiunto:

```java
public boolean isTurnPhase()
```

Questo metodo permette alla UI di riconoscere le sottofasi che fanno ancora parte del turno del giocatore senza trattarle come setup o game over.

### Nuove transizioni in Game

`Game` ora espone metodi applicativi per aprire e chiudere sottofasi:

```java
beginBuildPhase(BuildingType buildingType)
cancelBuildPhase()
beginTradePhase()
finishTradePhase()
finishRobberPhase()
```

La modifica rende il model responsabile della validita delle azioni:

- `build(...)` lavora in `BUILD_PHASE`;
- `bankTrade(...)` lavora in `TRADE_PHASE`;
- `activateRobber(...)`, `moveRobber(...)` e `stealFrom(...)` lavorano in `ROBBER_PHASE`;
- `endTurn()` e consentito solo quando il turno e tornato in `ACTIVE`.

Per compatibilita con bot e chiamate applicative gia esistenti, alcune operazioni possono aprire e chiudere internamente una sottofase temporanea quando partono da `ACTIVE`.

### GameController

`GameController` resta leggero e delega le nuove transizioni a `Game`.

Sono stati aggiunti wrapper espliciti:

```java
beginBuildPhase(...)
cancelBuildPhase()
beginTradePhase()
finishTradePhase()
finishRobberPhase()
```

In questo modo la view non modifica direttamente lo stato del model, ma passa sempre dal controller.

### CatanBoardPanel usa BoardInteractionMode

`CatanBoardPanel.Phase` e stato rinominato in:

```java
CatanBoardPanel.BoardInteractionMode
```

Valori attuali:

- `INITIAL_COLONY`
- `INITIAL_ROAD`
- `TURN_ACTIONS`
- `BUILD_COLONY`
- `BUILD_CITY`
- `BUILD_STREET`
- `MOVE_ROBBER`

Questo enum non rappresenta piu una fase della partita.
Rappresenta solo cosa deve fare il prossimo click sulla board o quale hover deve essere disegnato.

Sono stati rinominati anche i metodi pubblici:

```java
setInteractionMode(...)
getInteractionMode()
```

### Effetti sulla view

`GameWindow` ora:

- chiama `gc.beginBuildPhase(...)` prima di chiedere alla board di attendere un click di costruzione;
- chiama `gc.cancelBuildPhase()` se il giocatore termina il turno dopo aver solo selezionato una costruzione normale;
- apre `TRADE_PHASE` quando il dialog del trade e realmente usabile;
- chiude `TRADE_PHASE` quando il dialog viene chiuso o il trade viene completato;
- lascia `ROBBER_PHASE` attiva finche il robber non viene mosso e il furto non viene risolto;
- imposta `BoardInteractionMode.MOVE_ROBBER` o `BoardInteractionMode.BUILD_STREET` solo dopo che il model e entrato nella fase corretta.

### Effetti sui bot

Le strategie bot ora usano le transizioni di dominio prima delle azioni:

- prima di costruire chiamano `beginBuildPhase(...)`;
- per i trade chiamano `beginTradePhase()` e `finishTradePhase()`;
- in caso di errore chiudono la sottofase aperta quando possibile.

Anche il movimento del robber da parte dei bot chiude `ROBBER_PHASE` tramite controller dopo aver mosso il robber ed eventualmente rubato una risorsa.

### File modificati in REFACTORING 3

- `CatanProject/src/it/univaq/caffeine/catan/model/enums/GamePhase.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/Game.java`
- `CatanProject/src/it/univaq/caffeine/catan/controller/GameController.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/bot/AbstractBotStrategy.java`
- `CatanProject/src/it/univaq/caffeine/catan/view/CatanBoardPanel.java`
- `CatanProject/src/it/univaq/caffeine/catan/view/GameWindow.java`

### Verifiche eseguite in REFACTORING 3

E stata eseguita una compilazione completa con `javac` includendo Gson `2.10.1` nel classpath.

Comando usato:

```text
javac -cp ~/.m2/repository/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar -d /tmp/catan_compile @/tmp/catan_sources.txt
```

Risultato:

```text
Compilazione riuscita
```

Sono state eseguite ricerche mirate per verificare che non restassero riferimenti alla vecchia view phase:

- nessuna occorrenza residua di `CatanBoardPanel.Phase` nel codice sorgente;
- nessuna occorrenza residua di `enum Phase` nella view sorgente;
- nessuna modalita `DONE` nella board;
- la view usa `BoardInteractionMode` solo per input grafico e click;
- le sottofasi `BUILD_PHASE`, `TRADE_PHASE` e `ROBBER_PHASE` sono ora usate da model/controller.

## REFACTORING 4 - State Pattern puro per le fasi di Game

Questa sezione documenta la modifica richiesta per risolvere in modo completo il punto 4 indicato dalla professoressa: gli stati di gioco non devono essere governati solo da stringhe o da confronti diretti su enum, ma tramite il pattern GoF State.

### Collegamento con le modifiche precedenti

Il punto 4 era gia stato affrontato in modo parziale nelle sezioni precedenti del documento:

- nella sezione `Game phases`, a partire dalla riga 144, era stato documentato il passaggio dagli stati string-based (`"Setup"`, `"InitialPlacement"`, `"ActiveGame"`, `"GameOver"`) all'enum `GamePhase`;
- nella sezione `Game`, alla riga 342 circa, era stato riassunto che lo stato partita era stato sostituito da `GamePhase`;
- nel `REFACTORING 3`, a partire dalla riga 1068, era stato documentato che `GamePhase` governava anche le sottofasi di dominio `BUILD_PHASE`, `TRADE_PHASE` e `ROBBER_PHASE`.

Queste modifiche avevano risolto il problema delle stringhe, ma non applicavano ancora il pattern State puro: `Game` continuava infatti a mantenere direttamente un `GamePhase` e a decidere alcune transizioni tramite confronti con l'enum.

La soluzione completa e stata quindi ripresa in questa sezione, `REFACTORING 4`, che inizia alla riga 1211 del documento. Qui sono state documentate le nuove classi `GameState`, `SetupState`, `InitialPlacementState`, `ActiveState`, `BuildPhaseState`, `TradePhaseState`, `RobberPhaseState` e `GameOverState`, e il fatto che `Game` ora delega le transizioni allo stato corrente.

### Problema residuo

Dopo i refactoring precedenti il problema delle stringhe era gia stato risolto usando `GamePhase`.

Il codice non confrontava piu valori come:

```java
"ActiveGame"
"GameOver"
"RobberPhase"
```

Tuttavia `Game` continuava a contenere un campo:

```java
private GamePhase phase;
```

e diverse transizioni erano ancora decise con controlli del tipo:

```java
if (phase == GamePhase.ACTIVE) { ... }
if (phase == GamePhase.BUILD_PHASE) { ... }
setPhase(GamePhase.ROBBER_PHASE);
```

Questa soluzione era type-safe, ma non era ancora il pattern State puro.

### Dopo

E stato introdotto il package:

- `it.univaq.caffeine.catan.model.state`

Con una interfaccia comune:

- `GameState`

E gli stati concreti:

- `SetupState`
- `InitialPlacementState`
- `ActiveState`
- `BuildPhaseState`
- `TradePhaseState`
- `RobberPhaseState`
- `GameOverState`

`Game` ora mantiene un riferimento allo stato astratto:

```java
private GameState phase;
```

Le transizioni non vengono piu fatte assegnando direttamente un enum, ma delegando allo stato corrente.

Esempi:

```java
setPhase(phase.confirmSetup(this));
setPhase(phase.beginBuildPhase(this, buildingType));
setPhase(phase.beginTradePhase(this));
setPhase(phase.finishRobberPhase(this));
setPhase(phase.afterDiceRoll(this, lastDiceResult));
setPhase(phase.afterDevelopmentCard(this, effectiveContext));
```

### Ruolo di GamePhase dopo State

`GamePhase` non governa piu il comportamento.

Resta come identificatore type-safe e come valore di compatibilita/display:

```java
public GamePhase getPhase() { return phase.getPhase(); }
public String getState() { return phase.getDisplayName(); }
```

Questo permette alla UI esistente di continuare a leggere il nome della fase, ma la logica di transizione e responsabilita degli oggetti `GameState`.

### Come si riflette nel class diagram

Nel class diagram ora va rappresentato cosi:

- `Game` ha una associazione verso `GameState`;
- `GameState` e una interfaccia o classe astratta;
- `SetupState`, `InitialPlacementState`, `ActiveState`, `BuildPhaseState`, `TradePhaseState`, `RobberPhaseState` e `GameOverState` implementano `GameState`;
- `GamePhase` puo essere rappresentato come enum usato dagli stati per display/compatibilita, non come oggetto che decide il comportamento.

Relazione principale:

```text
Game 1 ----> 1 GameState
GameState <|.. SetupState
GameState <|.. InitialPlacementState
GameState <|.. ActiveState
GameState <|.. BuildPhaseState
GameState <|.. TradePhaseState
GameState <|.. RobberPhaseState
GameState <|.. GameOverState
```

### Effetti su Game.java

`Game` e ora il Context del pattern State.

Responsabilita di `Game`:

- conserva il riferimento allo stato corrente;
- invoca le operazioni sullo stato corrente;
- aggiorna il riferimento quando lo stato restituisce il prossimo stato;
- mantiene la logica di dominio non specifica dello stato, come giocatori, mappa, banca, deck, robber e servizi.

Responsabilita degli stati concreti:

- decidere se una transizione e consentita;
- restituire il prossimo stato valido;
- bloccare operazioni non ammesse in quella fase tramite eccezioni coerenti.

Esempi:

- `SetupState` permette `confirmSetup(...)` e passa a `InitialPlacementState`;
- `InitialPlacementState` permette `completeInitialPlacement(...)` e passa ad `ActiveState`;
- `ActiveState` permette `beginBuildPhase(...)`, `beginTradePhase(...)`, `afterDiceRoll(...)` e `afterDevelopmentCard(...)`;
- `BuildPhaseState` permette `finishBuildPhase(...)` e `cancelBuildPhase(...)`;
- `TradePhaseState` permette `finishTradePhase(...)`;
- `RobberPhaseState` permette `finishRobberPhase(...)`;
- `GameOverState` non permette nuove azioni di gioco.

### Effetti su bot e view

I confronti diretti nella view e nelle strategie bot sono stati sostituiti con query esposte da `Game`.

Esempi:

```java
game.isActivePhase()
game.isBuildPhase()
game.isTradePhase()
game.isRobberPhase()
game.isTurnPhase()
game.isGameOver()
```

In questo modo `GameWindow`, `CatanBoardPanel` e le strategie bot non devono conoscere i dettagli concreti delle classi di stato.

### File aggiunti in REFACTORING 4

- `CatanProject/src/it/univaq/caffeine/catan/model/state/GameState.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/state/SetupState.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/state/InitialPlacementState.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/state/ActiveState.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/state/BuildPhaseState.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/state/TradePhaseState.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/state/RobberPhaseState.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/state/GameOverState.java`

### File modificati in REFACTORING 4

- `CatanProject/src/it/univaq/caffeine/catan/model/Game.java`
- `CatanProject/src/it/univaq/caffeine/catan/model/bot/AbstractBotStrategy.java`
- `CatanProject/src/it/univaq/caffeine/catan/view/CatanBoardPanel.java`
- `CatanProject/src/it/univaq/caffeine/catan/view/GameWindow.java`
- `README_REFACTORING.md`

### Pattern e principi applicati in REFACTORING 4

Pattern applicato:

- State Pattern GoF.

Principi GRASP rafforzati:

- Polymorphism;
- Protected Variations;
- Low Coupling;
- High Cohesion.

### Verifiche eseguite in REFACTORING 4

E stata eseguita una compilazione completa con `javac` includendo Gson `2.10.1` nel classpath.

Risultato:

```text
Compilazione riuscita
```

Sono state eseguite ricerche mirate per verificare che i confronti diretti con `GamePhase` non fossero piu sparsi in bot e view.

Risultato:

- `GamePhase` resta nell'enum;
- `GamePhase` resta negli stati concreti come identificatore/display;
- `Game.getPhase()` resta come getter di compatibilita;
- `GameWindow`, `CatanBoardPanel` e `AbstractBotStrategy` usano le query dello stato esposte da `Game`.
