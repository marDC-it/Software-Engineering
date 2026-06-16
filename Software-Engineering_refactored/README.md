# Catan Project

## Project Description

This project is a Java/Swing implementation of **The Settlers of Catan**, developed for the Software Engineering course at the Universita degli Studi dell'Aquila (Prof.ssa Alessia Di Fonso).

The code follows an object-oriented design based on the course UML artefacts and is organized with a layered MVC structure:

- `config`: technical configuration loading.
- `controller`: thin application controller.
- `model`: domain logic and game rules.
- `view`: Swing user interface.

The latest refactoring keeps the external gameplay unchanged, but improves the internal design by applying GRASP principles and GoF patterns: Strategy, State, polymorphic cards, extracted services, and clearer domain hierarchies.

---

## Architecture

| Layer | Responsibility |
|---|---|
| **config** | Loads `game_config.json` through Gson and exposes a read-only `GameConfig` object |
| **controller** | Delegates UI/system operations to `Game` without domain logic |
| **model** | Contains game state, rules, board, players, cards, trading, building, achievements, and bot decisions |
| **view** | Swing GUI for setup, board rendering, dialogs, and player actions |

`ConfigLoader` loads the configuration once at game startup. The resulting `GameConfig` is injected into the model, avoiding hardcoded game values in the domain logic.

`Game` remains the main domain coordinator, but specialized responsibilities are delegated to cohesive collaborators such as `BuildService`, `AchievementService`, concrete `GameState` classes, concrete development cards, and bot strategies.

---

## Package Structure

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

## Main Design Choices

### Configuration

`GameConfig` is populated by Gson from `game_config.json`. Its fields are private and exposed through getters only, so configuration values are read-only after loading.

### Controller

`GameController` is intentionally thin. It receives requests from the Swing UI and forwards them to `Game`, preserving the GRASP Controller role and keeping domain rules outside the UI/controller layer.

### Player Hierarchy

`Player` is now abstract. Concrete player types are represented by:

- `HumanPlayer`
- `BotPlayer`

This removes the old design where one `Player` class contained both human and bot-specific data. Bot behavior now belongs to `BotPlayer` and its strategy.

### Bot Strategy Pattern

Bot logic is implemented through the `BotStrategy` interface and concrete strategies:

- `RandomBotStrategy`: random/easy behavior.
- `StrategicBotStrategy`: priority-based decisions for placement, building, trading, and robber movement.

This replaces the previous single bot class with string-based difficulty checks.

### Development Cards

`DevelopmentCard` is abstract. Each card type implements its own behavior:

- `KnightCard`
- `MonopolyCard`
- `YearOfPlentyCard`
- `RoadBuildingCard`
- `VictoryPointCard`

`Deck` creates cards through `DevelopmentCardFactory`, and `Game` plays cards polymorphically through `card.play(...)`.

### Game State Pattern

The game phases are modeled with the GoF State pattern.

`Game` keeps a `GameState` reference, while concrete states decide which transitions are valid:

- setup
- initial placement
- active turn
- build phase
- trade phase
- robber phase
- game over

`GamePhase` remains as a type-safe identifier/display value, but state transitions are delegated to `GameState` objects.

### Building Hierarchy

The building hierarchy separates board-edge structures from vertex structures:

- `Street` is placed on an `Edge` and produces no resources.
- `VertexBuilding` is placed on an `Intersection`.
- `Colony` produces 1 resource.
- `City` produces 2 resources.

`Game` can ask a generic `Building` for its intersection and production amount without checking concrete types.

### Services

Two model services reduce the size and responsibility of `Game`:

- `BuildService`: placement, construction, validation, and resource payment.
- `AchievementService`: Longest Road, Largest Army, and related victory point bonuses.

### Robber Abstraction

`Robber` is abstract and the current game uses `TerrainRobber`. The model depends on the abstraction, so future robber-like variants can be added without rewriting the main game flow.

---

## Implemented Features

### UC 1.0 - Game Setup

Setup wizard for game mode, player count, human/bot split, bot difficulty, and player names.

### UC 2.0 - Initial Placement

Snake-pattern placement of 2 colonies and 2 roads per player, including initial resource distribution and bot automation.

### UC 3.0 - Execute Game Turn

Full active turn management with dice rolling, building, trading, development cards, robber handling, and turn progression.

### UC 3.1 - Resource Production

Dice rolls distribute resources to players with buildings on matching tiles. The robber blocks production on its tile.

### UC 3.1 EX.1 - Robber

When a 7 is rolled, eligible players discard half their resources. Humans are handled through the UI, while bots discard automatically. The active player moves the robber and may steal from an adjacent opponent.

### UC 3.2 - Bank and Port Trading

Trading supports the default 4:1 bank rate, generic 3:1 ports, and resource-specific 2:1 ports.

### UC 3.3 - Build Structures

Players can build colonies, cities, and streets with resource cost checks and placement validation.

### UC 3.4 - Buy Development Card

Players can buy cards from the development deck using configured resource costs.

### UC 3.5 - Play Development Card

Supported development cards:

- Knight
- Monopoly
- Discovery / Year of Plenty
- Road Building
- hidden Victory Point cards

### Victory Conditions and Bonus Cards

The game checks victory after victory-point-changing actions. It supports hidden VP cards, Longest Road, and Largest Army.

---

## How to Run the Project

The project is configured for **Java 21** and can be run directly from IntelliJ IDEA.

1. Open IntelliJ IDEA.
2. Select **Open** and choose the project folder.
3. Mark `CatanProject/src` as **Sources Root**.
4. Mark `CatanProject/Resources` as **Resources Root**.
5. Configure a Java 21 SDK if IntelliJ does not detect it automatically.
6. Add the Gson JAR to the project libraries if it is not already configured.
7. Open and run `CatanProject/src/it/univaq/caffeine/catan/Main.java`.

---

## Dependencies

| Library | Purpose |
|---|---|
| **Gson** | JSON parsing for `ConfigLoader` |
| **Java Swing** | Desktop GUI rendering |

---

## Additional Documentation

- `README_IT.md`: Italian version of the general project documentation.
- `README_REFACTORING.md`: chronological technical notes about the refactoring work.
- `README_STUDIO.md`: study guide explaining what changed, why it changed, and which principles/patterns were applied.

---

## Authors

Developed by the **Caffeine Coders** team:

- Lorenzo Rodorigo
- Mario Del Corvo
- Gennaro Ranalli

---

## License

This project is intended for educational purposes only.
