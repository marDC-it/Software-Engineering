package it.univaq.caffeine.catan.model;

import it.univaq.caffeine.catan.config.GameConfig;
import it.univaq.caffeine.catan.model.board.*;
import it.univaq.caffeine.catan.model.cards.CardPlayContext;
import it.univaq.caffeine.catan.model.cards.Deck;
import it.univaq.caffeine.catan.model.cards.DevelopmentCard;
import it.univaq.caffeine.catan.model.cards.KnightCard;
import it.univaq.caffeine.catan.model.cards.MonopolyCard;
import it.univaq.caffeine.catan.model.cards.ResourceCard;
import it.univaq.caffeine.catan.model.cards.RoadBuildingCard;
import it.univaq.caffeine.catan.model.cards.YearOfPlentyCard;
import it.univaq.caffeine.catan.model.enums.BuildingType;
import it.univaq.caffeine.catan.model.enums.GamePhase;
import it.univaq.caffeine.catan.model.enums.ResourceType;
import it.univaq.caffeine.catan.model.player.*;
import it.univaq.caffeine.catan.model.rules.CostRegistry;
import it.univaq.caffeine.catan.model.rules.GameRules;
import it.univaq.caffeine.catan.model.services.AchievementService;
import it.univaq.caffeine.catan.model.services.BuildService;
import it.univaq.caffeine.catan.model.state.GameState;
import it.univaq.caffeine.catan.model.state.SetupState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * Central domain class — root of the domain model.
 *
 * SINGLE POINT OF CONTACT with GameConfig in the model layer.
 * Game receives GameConfig via constructor (Dependency Injection from
 * GameController) and is the ONLY class in the model that reads from it.
 * All other model classes receive only the specific primitive values or
 * data structures they need — never the GameConfig object itself.
 *
 * GRASP: Creator (creates Map, Bank, Deck, Players, Robber, Services),
 *        Information Expert (owns cfg and exposes derived values to the rest
 *        of the model through direct constructor injection).
 */
public class Game {

    // ── 1. Configuration & Instance Variables ─────────────────────

    /** Single GameConfig reference in the entire model layer. */
    private final GameConfig cfg;

    // Stable game attributes
    private String    mode;
    private int       maxPlayers;
    private GameState phase;
    private int       currentShift;

    // Turn state management (UC 3.0)
    private boolean      diceRolled         = false;
    private int          lastDiceResult     = 0;
    private int          freeRoadsRemaining = 0;
    private BuildingType pendingBuildType;

    // Domain objects (created in confirmGameSetup)
    private List<Player>       players;
    private GameMap            map;
    private Deck               deck;
    private Bank               bank;
    private GameRules          rules;
    private CostRegistry       costRegistry;
    private BuildService       buildService;
    private AchievementService achievementService;
    private Robber             robber;
    private Colony             lastPlacedColony;

    // Transient setup data (valid only during UC 1.0)
    private List<String> pendingNames;
    private int          numHumans;
    private String       botDifficulty;


    // ── 2. Constructor (SD1) ──────────────────────────────────────

    /**
     * Receives GameConfig via Dependency Injection from GameController.
     * Immediately extracts values needed for early-phase objects (GameRules,
     * CostRegistry) so those classes remain free of any GameConfig reference.
     */
    public Game(GameConfig cfg) {
        this.cfg  = cfg;

        // GameRules and CostRegistry are needed before confirmGameSetup(),
        // so we build them here by extracting values from cfg directly.
        this.rules = new GameRules(
                cfg.getVictoryPointsGoal(),
                cfg.getRobberHandLimit(),
                cfg.getMaxColonyForPlayer(),
                cfg.getMaxStreetsForPlayer(),
                cfg.getMaxCitiesForPlayer()
        );
        this.costRegistry = new CostRegistry(cfg.getBuildingCosts());

        this.players      = new ArrayList<>();
        this.pendingNames = new ArrayList<>();
        this.phase        = SetupState.INSTANCE;
        this.currentShift = 0;
    }


    // ── 3. Setup & Configuration (UC 1.0) ─────────────────────────

    public List<String> getAvailableGameModes() { return cfg.getGameModes(); }

    public int[] getPlayerRange() {
        return new int[]{ cfg.getMinPlayers(), cfg.getMaxPlayers() };
    }

    /** Exposes player colors for the View — avoids View touching cfg directly. */
    public List<String> getPlayerColors()    { return cfg.getPlayerColors(); }

    /** Exposes bot difficulties for the View — avoids View touching cfg directly. */
    public List<String> getBotDifficulties() { return cfg.getBotDifficulties(); }

    public void setGameMode(String mode)      { this.mode = mode; }
    public void setMaxPlayers(int n)          { this.maxPlayers = n; }
    public void addPlayerName(String name)    { pendingNames.add(name); }
    public void setNumHumans(int n)           { this.numHumans = n; }
    public void setBotDifficulty(String diff) { this.botDifficulty = diff; }
    private void setPhase(GameState phase)    { this.phase = phase; }

    private void requireActiveTurn(String action) {
        if (!phase.isActive())
            throw new IllegalStateException("Cannot " + action + " while game phase is " + phase.getDisplayName() + ".");
    }

    private void requireDiceRolled(String action) {
        if (!diceRolled)
            throw new IllegalStateException("Must roll dice before " + action + ".");
    }

    private void requireBuildPhase(String action) {
        if (!phase.isBuildPhase())
            throw new IllegalStateException("Cannot " + action + " while game phase is " + phase.getDisplayName() + ".");
    }

    private void requireTradePhase(String action) {
        if (!phase.isTradePhase())
            throw new IllegalStateException("Cannot " + action + " while game phase is " + phase.getDisplayName() + ".");
    }

    private void requireRobberPhase(String action) {
        if (!phase.isRobberPhase())
            throw new IllegalStateException("Cannot " + action + " while game phase is " + phase.getDisplayName() + ".");
    }

    public void beginBuildPhase(BuildingType buildingType) {
        if (buildingType == null || buildingType == BuildingType.DEVELOPMENT_CARD)
            throw new IllegalArgumentException("Invalid building type for build phase.");

        requireDiceRolled("building");
        GameState nextPhase = phase.beginBuildPhase(this, buildingType);
        pendingBuildType = buildingType;
        setPhase(nextPhase);
    }

    public void cancelBuildPhase() {
        GameState nextPhase = phase.cancelBuildPhase(this);
        pendingBuildType = null;
        setPhase(nextPhase);
    }

    private void finishBuildPhase() {
        GameState nextPhase = phase.finishBuildPhase(this);
        pendingBuildType = null;
        setPhase(nextPhase);
    }

    public void beginTradePhase() {
        requireActiveTurn("start trade phase");
        requireDiceRolled("trading");
        setPhase(phase.beginTradePhase(this));
    }

    public void finishTradePhase() {
        setPhase(phase.finishTradePhase(this));
    }

    public void finishRobberPhase() {
        setPhase(phase.finishRobberPhase(this));
    }

    /**
     * SD5 — Creates all domain objects for the game.
     *
     * This is the single place where GameConfig values are read and
     * forwarded to the other model classes as plain primitives or data
     * structures. After this method returns, no model class other than
     * Game holds any reference to cfg.
     */
    public void confirmGameSetup() {
        List<String> colors = cfg.getPlayerColors();

        // ── Create players ──────────────────────────────────────────
        for (int i = 0; i < pendingNames.size(); i++) {
            String color = (i < colors.size()) ? colors.get(i) : "Gray";
            Player p;
            if (i >= numHumans) {
                p = new BotPlayer(pendingNames.get(i), color,
                        botDifficulty != null ? botDifficulty : "Easy");
            } else {
                p = new HumanPlayer(pendingNames.get(i), color);
            }

            // Values extracted from cfg here — Player never sees cfg
            p.setAvailableColonies(cfg.getMaxColonyForPlayer());
            p.setAvailableCities(cfg.getMaxCitiesForPlayer());
            p.setAvailableStreets(cfg.getMaxStreetsForPlayer());

            players.add(p);
        }

        // ── Build Port objects from config data (Game is responsible) ──
        // Port is a domain object in the board package; GameMap receives
        // the ready-made list so it never needs to import GameConfig.
        List<Port> ports = new ArrayList<>();
        for (GameConfig.PortDefinition def : cfg.getPorts()) {
            ResourceType rt = (def.getResourceType() != null)
                    ? ResourceType.valueOf(def.getResourceType())
                    : null;
            ports.add(new Port(rt, def.getRate(), def.getX(), def.getY()));
        }

        // ── Create model objects — each receives only what it needs ────
        map  = new GameMap(
                cfg.getTerrainDist(),
                cfg.getTokenNumbers(),
                cfg.getTileAxial(),
                ports
        );
        map.generateMap();

        bank = new Bank(cfg.getBankResourcesPerType());

        deck = new Deck(cfg.getDeckComposition());
        deck.shuffle();

        buildService = new BuildService(
                map, bank, rules, costRegistry,
                cfg.getVictoryPointsColony(),
                cfg.getVictoryPointsCity()
        );

        achievementService = new AchievementService(
                map,
                cfg.getLongestRoadMinimum(), cfg.getLongestRoadBonus(),
                cfg.getLargestArmyMinimum(), cfg.getLargestArmyBonus()
        );

        // ── Place the robber on the desert tile ───────────────────────
        HexagonalTile desert = map.getHexagons().stream()
                .filter(t -> t.getTerrainType() == ResourceType.DESERT)
                .findFirst().orElse(map.getHexagons().get(0));
        robber = new TerrainRobber(desert);

        generateTurnOrder();
        setPhase(phase.confirmSetup(this));

        // Cleanup transient data
        pendingNames  = null;
        botDifficulty = null;
    }


    // ── 4. Turn Management & Victory Conditions ───────────────────

    public Player getCurrentPlayer() {
        if (players == null || players.isEmpty()) return null;
        int n = players.size();

        if (phase.isInitialPlacement()) {
            if (currentShift < n)      return players.get(currentShift);
            if (currentShift < 2 * n)  return players.get(2 * n - 1 - currentShift);
            return null;
        }

        return players.get(currentShift % n);
    }

    public void generateTurnOrder() {
        Collections.shuffle(players);
        currentShift = 0;
    }

    public void endTurn() {
        requireActiveTurn("end turn");
        if (!diceRolled)
            throw new IllegalStateException("Must roll dice before ending turn.");

        if (checkVictory()) {
            setPhase(phase.gameOver(this));
            return;
        }

        currentShift++;
        diceRolled     = false;
        lastDiceResult = 0;
    }

    public boolean checkVictory() {
        Player current = getCurrentPlayer();
        if (current == null) return false;
        int totalVP = current.getVictoryPoints() + countVPCardsInHand(current);
        return totalVP >= rules.getVictoryPointsGoal();
    }

    private int countVPCardsInHand(Player p) {
        return (int) p.getCards().stream()
                .filter(DevelopmentCard::isVictoryPoint)
                .count();
    }

    public Player getWinner() {
        return players.stream()
                .filter(p -> p.getVictoryPoints() + countVPCardsInHand(p)
                        >= rules.getVictoryPointsGoal())
                .findFirst()
                .orElse(null);
    }


    // ── 5. Initial Placement (UC 2.0) ─────────────────────────────

    public void placeColony(int intersectionId) {
        lastPlacedColony = buildService.placeInitialColony(getCurrentPlayer(), intersectionId);
    }

    public void placeRoad(int edgeId) {
        buildService.placeInitialRoad(getCurrentPlayer(), lastPlacedColony, edgeId);
        currentShift++;

        if (currentShift >= maxPlayers * 2) {
            distributeInitialResources();
            setPhase(phase.completeInitialPlacement(this));
        }
    }

    public void distributeInitialResources() {
        for (Player p : players) {
            for (Building b : p.getBuildings()) {
                Intersection inter = b.getIntersection();
                if (inter == null) continue;

                for (HexagonalTile tile : inter.getAdjacentTiles()) {
                    if (tile.getTerrainType() != ResourceType.DESERT)
                        bank.transferResourcesTo(p, tile.getTerrainType(), 1);
                }
            }
        }
    }


    // ── 6. Turn Actions (Building & Buying) ──────────────

    public void build(BuildingType buildingType, int locationId) {
        boolean startedHere = false;
        if (phase.isActive()) {
            beginBuildPhase(buildingType);
            startedHere = true;
        } else {
            requireBuildPhase("build");
            requireDiceRolled("building");
        }

        if (pendingBuildType != null && pendingBuildType != buildingType)
            throw new IllegalStateException("Current build phase is for " + pendingBuildType + ".");

        Player player = getCurrentPlayer();
        try {
            buildService.build(player, buildingType, locationId);
        } catch (RuntimeException ex) {
            if (startedHere && phase.isBuildPhase() && freeRoadsRemaining == 0) {
                finishBuildPhase();
            }
            throw ex;
        }

        if (buildingType == BuildingType.STREET) achievementService.checkLongestRoad(player);

        if (checkVictory()) {
            setPhase(phase.gameOver(this));
        } else if (startedHere || phase.isBuildPhase()) {
            finishBuildPhase();
        }
    }

    public DevelopmentCard buyDevelopmentCard() {
        requireActiveTurn("buy cards");
        requireDiceRolled("buying a card");
        if (deck.isEmpty())
            throw new IllegalStateException("Development card deck is empty.");

        Player p = getCurrentPlayer();
        Map<ResourceType, Integer> cost = costRegistry.getCost(BuildingType.DEVELOPMENT_CARD);

        for (var entry : cost.entrySet())
            if (!p.hasResource(entry.getKey(), entry.getValue()))
                throw new IllegalStateException("Insufficient resources to buy a development card.");

        DevelopmentCard card = deck.drawCard();
        p.addDevelopmentCard(card);

        cost.forEach((res, qty) -> bank.receiveResourcesFrom(p, res, qty));

        if (checkVictory()) setPhase(phase.gameOver(this));

        return card;
    }


    // ── 7. Dice Rolling & Resources ───────────────────────────────

    public int rollDice() {
        requireActiveTurn("roll dice");
        if (diceRolled)
            throw new IllegalStateException("Dice already rolled this turn.");

        int die1 = (int)(Math.random() * 6) + 1;
        int die2 = (int)(Math.random() * 6) + 1;
        lastDiceResult = die1 + die2;
        diceRolled = true;

        if (lastDiceResult != 7) {
            distributeResources(lastDiceResult);
        }
        setPhase(phase.afterDiceRoll(this, lastDiceResult));
        return lastDiceResult;
    }

    private void distributeResources(int diceResult) {
        for (HexagonalTile tile : map.getHexagons()) {
            if (tile.getToken() == null) continue;
            if (tile.getToken().getNumber() != diceResult) continue;
            if (tile.getTerrainType() == ResourceType.DESERT) continue;
            if (robber != null && robber.occupies(tile)) continue;

            ResourceType resource = tile.getTerrainType();

            for (Intersection inter : tile.getIntersections()) {
                Building building = inter.getBuilding();
                if (building == null) continue;
                int amount = building.getProductionAmount();
                if (amount <= 0) continue;

                bank.transferResourcesTo(building.getOwner(), resource, amount);
            }
        }
    }


    // ── 8. Robber Logic (UC 3.1) ──────────────────────────────────

    public Map<Player, Integer> activateRobber() {
        requireRobberPhase("activate robber");
        if (lastDiceResult != 7)
            throw new IllegalStateException("Robber discard is only triggered by rolling 7.");

        int limit    = rules.getRobberHandLimit();
        Player roller = getCurrentPlayer();
        Map<Player, Integer> humanDiscards = new LinkedHashMap<>();

        for (Player p : players) {
            if (p == roller) continue;
            if (p.getResources().size() > limit) {
                int toDiscard = p.getResources().size() / 2;
                p.handleRobberDiscard(bank, toDiscard, humanDiscards);
            }
        }
        return humanDiscards;
    }

    public void discardResources(Player p, Map<ResourceType, Integer> toDiscard) {
        requireRobberPhase("discard robber resources");
        toDiscard.forEach((type, qty) -> {
            if (qty > 0) bank.receiveResourcesFrom(p, type, qty);
        });
    }

    public void moveRobber(int tileId) {
        requireRobberPhase("move robber");
        HexagonalTile tile = map.getHexagons().stream()
                .filter(t -> t.getId() == tileId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tile not found: " + tileId));
        if (robber.occupies(tile))
            throw new IllegalStateException("Cannot place Robber on its current tile.");
        robber.moveTo(tile);
    }

    public void moveRobberDirect(HexagonalTile tile) {
        requireRobberPhase("move robber");
        if (tile == null) return;
        if (robber.occupies(tile))
            throw new IllegalStateException("Cannot place Robber on its current tile.");
        robber.moveTo(tile);
    }

    public List<Player> getStealablePlayers() {
        HexagonalTile tile = robber.getCurrentTile();
        if (tile == null) return Collections.emptyList();

        Player active = getCurrentPlayer();
        return tile.getIntersections().stream()
                .map(Intersection::getBuilding)
                .filter(b -> b != null && !b.getOwner().equals(active)
                        && !b.getOwner().getResources().isEmpty())
                .map(Building::getOwner)
                .distinct()
                .collect(Collectors.toList());
    }

    public ResourceCard stealFrom(Player victim) {
        requireRobberPhase("steal from another player");
        if (victim == null || victim.getResources().isEmpty()) return null;

        List<ResourceCard> res = new ArrayList<>(victim.getResources());
        Collections.shuffle(res);
        ResourceCard stolen = res.get(0);

        victim.removeResource(stolen.getType(), 1);
        getCurrentPlayer().addResource(new ResourceCard(stolen.getType()));

        return stolen;
    }


    // ── 9. Development Card Logic (UC 3.5) ────────────────────────

    private <T extends DevelopmentCard> T findPlayableCard(Player p, Class<T> cardClass) {
        return p.getCards().stream()
                .filter(cardClass::isInstance)
                .map(cardClass::cast)
                .filter(c -> c.canBePlayed(this, p))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No playable " + cardClass.getSimpleName() + " in hand."));
    }

    public void playDevelopmentCard(DevelopmentCard card, CardPlayContext context) {
        requireActiveTurn("play development card");

        Player p = getCurrentPlayer();
        if (p == null || !p.getCards().contains(card)) {
            throw new IllegalStateException("The selected card is not in the current player's hand.");
        }

        CardPlayContext effectiveContext = context != null ? context : new CardPlayContext();
        card.play(this, p, effectiveContext);

        if (checkVictory()) {
            setPhase(phase.gameOver(this));
        } else {
            if (effectiveContext.isRoadPlacementRequired()) {
                pendingBuildType = BuildingType.STREET;
            }
            setPhase(phase.afterDevelopmentCard(this, effectiveContext));
        }
    }

    public void playKnight() {
        playDevelopmentCard(
                findPlayableCard(getCurrentPlayer(), KnightCard.class),
                new CardPlayContext());
    }

    public void applyKnightCard(Player p) {
        achievementService.checkLargestArmy(p);
    }

    public int playMonopoly(ResourceType resource) {
        CardPlayContext context = new CardPlayContext();
        context.setSelectedResourceType(resource);
        playDevelopmentCard(
                findPlayableCard(getCurrentPlayer(), MonopolyCard.class),
                context);
        return context.getCollectedResourceCount();
    }

    public int applyMonopolyCard(Player p, ResourceType resource) {
        int totalCollected = 0;
        for (Player other : players) {
            if (other == p) continue;
            int count = other.countResources(resource);
            for (int i = 0; i < count; i++) {
                other.removeResource(resource, 1);
                p.addResource(new ResourceCard(resource));
            }
            totalCollected += count;
        }
        return totalCollected;
    }

    public void playDiscovery(ResourceType r1, ResourceType r2) {
        CardPlayContext context = new CardPlayContext();
        context.setSelectedResourceTypes(List.of(r1, r2));
        playDevelopmentCard(
                findPlayableCard(getCurrentPlayer(), YearOfPlentyCard.class),
                context);
    }

    public void applyYearOfPlentyCard(Player p, ResourceType r1, ResourceType r2) {
        bank.transferResourcesTo(p, r1, 1);
        bank.transferResourcesTo(p, r2, 1);
    }

    public void playRoadBuilding() {
        playDevelopmentCard(
                findPlayableCard(getCurrentPlayer(), RoadBuildingCard.class),
                new CardPlayContext());
    }

    public void applyRoadBuildingCard(Player p) {
        freeRoadsRemaining = 2;
    }

    public void buildFreeStreet(int locationId) {
        if (freeRoadsRemaining <= 0)
            throw new IllegalStateException("No free roads remaining.");
        if (phase.isActive()) {
            beginBuildPhase(BuildingType.STREET);
        } else {
            requireBuildPhase("build free street");
            requireDiceRolled("building a free street");
        }
        if (pendingBuildType != BuildingType.STREET)
            throw new IllegalStateException("Current build phase is for " + pendingBuildType + ".");

        Player player = getCurrentPlayer();
        buildService.buildFreeStreet(player, locationId);
        freeRoadsRemaining--;

        achievementService.checkLongestRoad(player);
        if (checkVictory()) {
            setPhase(phase.gameOver(this));
        } else if (freeRoadsRemaining == 0) {
            finishBuildPhase();
        }
    }


    // ── 10. Bank and Port Trade Logic (UC 3.2) ────────────────────

    public int getEffectiveTradeRate(Player player, ResourceType offerType) {
        int bestRate = cfg.getBankDefaultRate();  // only cfg read during active game

        for (Building b : player.getBuildings()) {
            Intersection inter = b.getIntersection();
            if (inter == null) continue;

            for (Port port : map.getPorts()) {
                if (!port.getAdjacentIntersections().contains(inter)) continue;

                if (port.isGeneric()) {
                    bestRate = Math.min(bestRate, port.getExchangeRate());
                } else if (port.getResourceType() == offerType) {
                    bestRate = Math.min(bestRate, port.getExchangeRate());
                }
            }
        }
        return bestRate;
    }

    public void bankTrade(ResourceType give, ResourceType receive) {
        if (give == receive)
            throw new IllegalStateException("Cannot trade a resource for itself.");

        boolean startedHere = false;
        if (phase.isActive()) {
            beginTradePhase();
            startedHere = true;
        } else {
            requireTradePhase("trade");
            requireDiceRolled("trading");
        }

        try {
            Player p    = getCurrentPlayer();
            int    rate = getEffectiveTradeRate(p, give);

            if (!p.hasResource(give, rate))
                throw new IllegalStateException("Need " + rate + "x " + give.name() + " to trade.");
            if (!bank.hasResources(receive, 1))
                throw new IllegalStateException("Bank has no " + receive.name() + ".");

            bank.receiveResourcesFrom(p, give, rate);
            bank.transferResourcesTo(p, receive, 1);
        } finally {
            if (startedHere && phase.isTradePhase()) {
                finishTradePhase();
            }
        }
    }

    public Map<ResourceType, Integer> getAllTradeRates(Player player) {
        Map<ResourceType, Integer> rates = new LinkedHashMap<>();
        for (ResourceType rt : ResourceType.values()) {
            if (rt == ResourceType.DESERT) continue;
            rates.put(rt, getEffectiveTradeRate(player, rt));
        }
        return rates;
    }


    // ── 11. Getters ───────────────────────────────────────────────

    public Player        getLongestRoadOwner()  { return achievementService == null ? null : achievementService.getLongestRoadOwner(); }
    public Player        getLargestArmyOwner()  { return achievementService == null ? null : achievementService.getLargestArmyOwner(); }
    public int           getFreeRoadsRemaining(){ return freeRoadsRemaining; }

    public String        getMode()              { return mode; }
    public int           getMaxPlayers()        { return maxPlayers; }
    public GamePhase     getPhase()             { return phase.getPhase(); }
    public GameState     getGameState()         { return phase; }
    public String        getState()             { return phase.getDisplayName(); }
    public int           getCurrentShift()      { return currentShift; }
    public List<Player>  getPlayers()           { return players; }
    public GameMap       getMap()               { return map; }
    public Deck          getDeck()              { return deck; }
    public Bank          getBank()              { return bank; }
    public GameRules     getRules()             { return rules; }
    public CostRegistry  getCostRegistry()      { return costRegistry; }
    public Robber        getRobber()            { return robber; }
    public Colony        getLastPlacedColony()  { return lastPlacedColony; }
    public GameConfig    getConfig()            { return cfg; }
    public boolean       canPlayDevelopmentCards()     { return phase.canTakeTurnActions(); }
    public boolean       isSetupPhase()         { return phase.isSetup(); }
    public boolean       isInitialPlacementPhase()    { return phase.isInitialPlacement(); }
    public boolean       isActivePhase()        { return phase.isActive(); }
    public boolean       isBuildPhase()         { return phase.isBuildPhase(); }
    public boolean       isTradePhase()         { return phase.isTradePhase(); }
    public boolean       isRobberPhase()        { return phase.isRobberPhase(); }
    public boolean       isTurnPhase()          { return phase.isTurnPhase(); }
    public boolean       isGameOver()           { return phase.isGameOver(); }
    public boolean       isDiceRolled()         { return diceRolled; }
    public int           getLastDiceResult()    { return lastDiceResult; }
}
