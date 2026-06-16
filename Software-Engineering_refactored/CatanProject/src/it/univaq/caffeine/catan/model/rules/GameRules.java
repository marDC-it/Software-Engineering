package it.univaq.caffeine.catan.model.rules;

import it.univaq.caffeine.catan.model.board.Edge;
import it.univaq.caffeine.catan.model.board.GameMap;
import it.univaq.caffeine.catan.model.board.Intersection;
import it.univaq.caffeine.catan.model.enums.BuildingType;
import it.univaq.caffeine.catan.model.player.Colony;
import it.univaq.caffeine.catan.model.player.Player;

/**
 * GRASP Information Expert for game rules.
 * Knows the game constraints and validates placements.
 *
 * All rule values are injected as primitives by Game, which is the
 * sole reader of GameConfig in the model layer. GameRules has no
 * dependency on GameConfig or ConfigLoader.
 */
public class GameRules {

    // ── 1. Instance Variables (injected by Game) ──────────────────

    private final int victoryPointsGoal;
    private final int robberHandLimit;
    private final int maxColonyForPlayer;
    private final int maxStreetsForPlayer;
    private final int maxCitiesForPlayer;


    // ── 2. Constructor ────────────────────────────────────────────

    /**
     * Receives every rule value as a primitive — no GameConfig reference needed.
     * Game extracts these values from GameConfig and injects them here.
     *
     * @param victoryPointsGoal  VP required to win (usually 10).
     * @param robberHandLimit    Max cards before robber discard (usually 7).
     * @param maxColony          Max colonies per player (usually 5).
     * @param maxStreets         Max streets per player (usually 15).
     * @param maxCities          Max cities per player (usually 4).
     */
    public GameRules(int victoryPointsGoal, int robberHandLimit,
                     int maxColony, int maxStreets, int maxCities) {
        this.victoryPointsGoal  = victoryPointsGoal;
        this.robberHandLimit    = robberHandLimit;
        this.maxColonyForPlayer = maxColony;
        this.maxStreetsForPlayer = maxStreets;
        this.maxCitiesForPlayer = maxCities;
    }


    // ── 3. Placement Validation ───────────────────────────────────

    /**
     * Validates colony placement: intersection must be free and satisfy the distance rule.
     */
    public boolean validateColonyPlacement(Intersection intersection,
                                           Player player, GameMap map) {
        if (intersection == null || !intersection.isFree()) return false;
        for (Intersection adj : intersection.getAdjacentIntersections()) {
            if (!adj.isFree()) return false;
        }
        return true;
    }

    /**
     * Validates road placement: edge must be free and adjacent to the last placed colony.
     */
    public boolean validateRoadPlacement(Edge edge, Colony lastColony, GameMap map) {
        if (edge == null || !edge.isFree()) return false;
        if (lastColony == null) return true;
        Intersection ci = lastColony.getIntersection();
        if (ci == null) return true;
        return ci.getAdjacentEdges().contains(edge);
    }

    public boolean checkResources(Player player, BuildingType type) {
        return true;
    }


    // ── 4. Getters (single source of truth for rule values) ───────

    public int getVictoryPointsGoal()   { return victoryPointsGoal; }
    public int getRobberHandLimit()     { return robberHandLimit; }
    public int getMaxColonyForPlayer()  { return maxColonyForPlayer; }
    public int getMaxStreetsForPlayer() { return maxStreetsForPlayer; }
    public int getMaxCitiesForPlayer()  { return maxCitiesForPlayer; }
}
