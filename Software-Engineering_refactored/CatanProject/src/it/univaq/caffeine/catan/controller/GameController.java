package it.univaq.caffeine.catan.controller;

import it.univaq.caffeine.catan.config.ConfigLoader;
import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.board.HexagonalTile;
import it.univaq.caffeine.catan.model.cards.CardPlayContext;
import it.univaq.caffeine.catan.model.cards.DevelopmentCard;
import it.univaq.caffeine.catan.model.cards.ResourceCard;
import it.univaq.caffeine.catan.model.player.Player;
import it.univaq.caffeine.catan.model.enums.BuildingType;
import it.univaq.caffeine.catan.model.enums.ResourceType;

import java.util.Map;
import java.util.List;

/**
 * GRASP Controller — receives system operations from the View
 * and delegates immediately to Game. Contains zero domain logic.
 *
 * ConfigLoader is called here (once, in startNewGame) and the
 * resulting GameConfig is injected into Game via the constructor.
 * Game itself has no dependency on ConfigLoader.
 */
public class GameController {

    // ── 1. Instance Variables ─────────────────────────────────────

    private Game game;


    // ── 2. System Setup (UC 1.0) ──────────────────────────────────

    /** SD1 — Loads configuration, creates the Game instance, and returns available game modes. */
    public List<String> startNewGame() {
        game = new Game(ConfigLoader.load());
        return game.getAvailableGameModes();
    }

    /** SD2 — Stores the selected game mode in Game. */
    public void selectGameMode(String type) {
        game.setGameMode(type);
    }

    /** SD3 — Stores the player count in Game. Validates against the config range. */
    public void setNumberOfPlayers(int n) {
        int[] range = game.getPlayerRange();
        if (n < range[0] || n > range[1]) {
            throw new IllegalArgumentException(
                    "Player count must be between " + range[0] + " and " + range[1] + ".");
        }
        game.setMaxPlayers(n);
    }

    public void setNumHumans(int n) {
        game.setNumHumans(n);
    }

    public void setBotDifficulty(String diff) {
        game.setBotDifficulty(diff);
    }

    /** SD4 — Accumulates player names in Game. */
    public void addPlayerName(String name) {
        game.addPlayerName(name);
    }

    /** SD5 — Triggers the creation of domain objects to finalize setup. */
    public void confirmGameSetup() {
        game.confirmGameSetup();
    }


    // ── 3. Initial Placement (UC 2.0) ─────────────────────────────

    /** PlaceColony — Places a starting colony at the specified intersection. */
    public void placeColony(int intersectionId) {
        game.placeColony(intersectionId);
    }

    /** PlaceRoad — Places a starting road at the specified edge. */
    public void placeRoad(int edgeId) {
        game.placeRoad(edgeId);
    }


    // ── 4. Turn Management & Basic Actions (UC 3.0) ───────────────

    /** UC 3.1 — Rolls dice, distributes resources, or triggers the robber if a 7 is rolled. */
    public int rollDice() {
        return game.rollDice();
    }

    /** UC 3.3 — Builds a structure (e.g., Road, Colony, City) on the board. */
    public void build(BuildingType buildingType, int locationId) {
        game.build(buildingType, locationId);
    }

    public void beginBuildPhase(BuildingType buildingType) {
        game.beginBuildPhase(buildingType);
    }

    public void cancelBuildPhase() {
        game.cancelBuildPhase();
    }

    /** UC 3.4 — Buys a development card from the deck. */
    public DevelopmentCard buyDevelopmentCard() {
        return game.buyDevelopmentCard();
    }

    /** UC 3.0 — Ends the current player's turn, checks for victory, and advances to the next player. */
    public void endTurn() {
        game.endTurn();
    }


    // ── 5. Robber Logic (UC 3.1 EX 1) ─────────────────────────────

    public Map<Player, Integer> activateRobber() {
        return game.activateRobber();
    }

    public void discardResources(Player p, Map<ResourceType, Integer> d) {
        game.discardResources(p, d);
    }

    public void moveRobber(int tileId) {
        game.moveRobber(tileId);
    }

    public void moveRobberDirect(HexagonalTile tile) {
        game.moveRobberDirect(tile);
    }

    public void finishRobberPhase() {
        game.finishRobberPhase();
    }

    public List<Player> getStealablePlayers() {
        return game.getStealablePlayers();
    }

    public ResourceCard stealFrom(Player victim) {
        return game.stealFrom(victim);
    }


    // ── 6. Playing Development Cards (UC 3.5) ─────────────────────

    public void playKnight() {
        game.playKnight();
    }

    public void playDevelopmentCard(DevelopmentCard card, CardPlayContext context) {
        game.playDevelopmentCard(card, context);
    }

    public int playMonopoly(ResourceType resource) {
        return game.playMonopoly(resource);
    }

    public void playDiscovery(ResourceType r1, ResourceType r2) {
        game.playDiscovery(r1, r2);
    }

    public void playRoadBuilding() {
        game.playRoadBuilding();
    }

    public void buildFreeStreet(int locationId) {
        game.buildFreeStreet(locationId);
    }

    public int getFreeRoadsRemaining() {
        return game.getFreeRoadsRemaining();
    }


    // ── 7. Trade Management ───────────────────────────────────────

    /** Gets the current effective trade rate for a specific resource based on player's ports. */
    public int getEffectiveTradeRate(ResourceType offer) {
        return game.getEffectiveTradeRate(game.getCurrentPlayer(), offer);
    }

    /** Executes a maritime or bank trade. */
    public void bankTrade(ResourceType give, ResourceType receive) {
        game.bankTrade(give, receive);
    }

    public void beginTradePhase() {
        game.beginTradePhase();
    }

    public void finishTradePhase() {
        game.finishTradePhase();
    }

    /** Retrieves all currently available maritime trade rates for the active player. */
    public Map<ResourceType, Integer> getAllTradeRates() {
        return game.getAllTradeRates(game.getCurrentPlayer());
    }


    // ── 8. Game State & Getters ───────────────────────────────────

    public Game getGame() {
        return game;
    }

    public boolean checkVictory() {
        return game.checkVictory();
    }

    public Player getWinner() {
        return game.getWinner();
    }

    /**
     * Returns the valid player count range {min, max} from the config.
     * The View uses this to build the player-count selector without hardcoding values.
     */
    public int[] getPlayerRange() {
        return game.getPlayerRange();
    }

    /** Returns the available player colors from GameConfig — used by the SetupWizard. */
    public List<String> getPlayerColors() {
        return game.getPlayerColors();
    }

    public Player getLongestRoadOwner() {
        return game.getLongestRoadOwner();
    }

    public Player getLargestArmyOwner() {
        return game.getLargestArmyOwner();
    }

    /** Returns the available bot difficulty levels — avoids View touching cfg directly. */
    public java.util.List<String> getBotDifficulties() {
        return game.getBotDifficulties();
    }

}
