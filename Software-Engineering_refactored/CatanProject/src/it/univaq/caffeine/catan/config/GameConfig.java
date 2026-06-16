package it.univaq.caffeine.catan.config;

import java.util.List;
import java.util.Map;

/**
 * Pure data class (POJO) that mirrors the game_config.json file.
 * This class contains NO logic - it only holds fields populated by the ConfigLoader.
 * To modify any game rule, cost, or value, edit the game_config.json file directly.
 */
public class GameConfig {

    // ── General Setup & Metadata ──────────────────────────────────

    /** Minimum number of players required to start the game */
    private int minPlayers;

    /** Maximum number of players allowed in a game */
    private int maxPlayers;

    /** Available game modes (e.g., "CLASSIC", "EXTENDED") */
    private List<String> gameModes;

    /** Available AI difficulty levels (e.g., "EASY", "STRATEGIC") */
    private List<String> botDifficulties;

    /** List of available colors for the players */
    private List<String> playerColors;


    // ── Board & Map Structure ─────────────────────────────────────

    /** Distribution of terrain types (ResourceType names, e.g., "WOOD", "SHEEP") */
    private String[] terrainDist;

    /** Number tokens to be placed on the hexes */
    private int[] tokenNumbers;

    /** Axial coordinates for tile placement on the board */
    private int[][] tileAxial;

    /** List of ports available on the map boundaries */
    private List<PortDefinition> ports;

    /**
     * Nested POJO for port configuration.
     */
    public static class PortDefinition {
        /** The specific resource type required by the port. If null, it represents a generic 3:1 port */
        private String resourceType;
        /** Exchange rate (e.g., 2 for a 2:1 port, 3 for a 3:1 port) */
        private int rate;
        /** X coordinate for port placement */
        private double x;
        /** Y coordinate for port placement */
        private double y;

        public String getResourceType() { return resourceType; }
        public int getRate() { return rate; }
        public double getX() { return x; }
        public double getY() { return y; }
    }


    // ── Player Inventory Limits ───────────────────────────────────

    /** Maximum number of settlements (colonies) a single player can build */
    private int maxColonyForPlayer;

    /** Maximum number of cities a single player can build */
    private int maxCitiesForPlayer;

    /** Maximum number of roads (streets) a single player can build */
    private int maxStreetsForPlayer;


    // ── Economy, Trading & Costs ──────────────────────────────────

    /** Initial amount of resources available in the bank per resource type */
    private int bankResourcesPerType;

    /** Default maritime trade rate with the bank (usually 4:1) */
    private int bankDefaultRate = 4;

    /**
     * Costs to build game pieces.
     * Outer key: BuildingType name (e.g., "ROAD", "CITY")
     * Inner key: ResourceType name and its required quantity
     */
    private Map<String, Map<String, Integer>> buildingCosts;

    /**
     * Composition of the development card deck.
     * Key: Card type (e.g., "KNIGHT", "MONOPOLY") -> Value: Number of copies in the deck
     */
    private Map<String, Integer> deckComposition;


    // ── Scoring & Game Rules ──────────────────────────────────────

    /** Number of Victory Points (VP) required to win the game */
    private int victoryPointsGoal;

    /** Victory Points awarded for each settlement (colony) */
    private int victoryPointsColony;

    /** Victory Points awarded for each city */
    private int victoryPointsCity;

    /** Maximum number of resource cards a player can hold safely when a 7 is rolled */
    private int robberHandLimit;

    /** Minimum continuous roads required to claim the "Longest Road" achievement */
    private int longestRoadMinimum;

    /** Victory Points awarded to the player holding the "Longest Road" */
    private int longestRoadBonus;

    /** Minimum number of played knight cards required to claim the "Largest Army" achievement */
    private int largestArmyMinimum;

    /** Victory Points awarded to the player holding the "Largest Army" */
    private int largestArmyBonus;

    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public List<String> getGameModes() { return gameModes; }
    public List<String> getBotDifficulties() { return botDifficulties; }
    public List<String> getPlayerColors() { return playerColors; }
    public String[] getTerrainDist() { return terrainDist; }
    public int[] getTokenNumbers() { return tokenNumbers; }
    public int[][] getTileAxial() { return tileAxial; }
    public List<PortDefinition> getPorts() { return ports; }
    public int getMaxColonyForPlayer() { return maxColonyForPlayer; }
    public int getMaxCitiesForPlayer() { return maxCitiesForPlayer; }
    public int getMaxStreetsForPlayer() { return maxStreetsForPlayer; }
    public int getBankResourcesPerType() { return bankResourcesPerType; }
    public int getBankDefaultRate() { return bankDefaultRate; }
    public Map<String, Map<String, Integer>> getBuildingCosts() { return buildingCosts; }
    public Map<String, Integer> getDeckComposition() { return deckComposition; }
    public int getVictoryPointsGoal() { return victoryPointsGoal; }
    public int getVictoryPointsColony() { return victoryPointsColony; }
    public int getVictoryPointsCity() { return victoryPointsCity; }
    public int getRobberHandLimit() { return robberHandLimit; }
    public int getLongestRoadMinimum() { return longestRoadMinimum; }
    public int getLongestRoadBonus() { return longestRoadBonus; }
    public int getLargestArmyMinimum() { return largestArmyMinimum; }
    public int getLargestArmyBonus() { return largestArmyBonus; }
}
