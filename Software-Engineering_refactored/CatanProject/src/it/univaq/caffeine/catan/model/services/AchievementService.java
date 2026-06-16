package it.univaq.caffeine.catan.model.services;

import it.univaq.caffeine.catan.model.board.Edge;
import it.univaq.caffeine.catan.model.board.GameMap;
import it.univaq.caffeine.catan.model.board.Intersection;
import it.univaq.caffeine.catan.model.player.Building;
import it.univaq.caffeine.catan.model.player.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * GRASP Information Expert for Longest Road and Largest Army achievements.
 *
 * All threshold and bonus values are injected as primitives by Game,
 * which is the sole reader of GameConfig in the model layer.
 * AchievementService has no dependency on GameConfig or ConfigLoader.
 */
public class AchievementService {

    // ── 1. Instance Variables ─────────────────────────────────────

    private final GameMap map;

    // Injected rule values (from Game, which reads GameConfig)
    private final int longestRoadMinimum;
    private final int longestRoadBonus;
    private final int largestArmyMinimum;
    private final int largestArmyBonus;

    // Mutable state: current holders
    private Player longestRoadOwner;
    private int    longestRoadLength;
    private Player largestArmyOwner;
    private int    largestArmySize;


    // ── 2. Constructor ────────────────────────────────────────────

    /**
     * @param map                The game map (for road graph traversal).
     * @param longestRoadMinimum Minimum road length to claim Longest Road.
     * @param longestRoadBonus   VP awarded for Longest Road.
     * @param largestArmyMinimum Minimum knights played to claim Largest Army.
     * @param largestArmyBonus   VP awarded for Largest Army.
     */
    public AchievementService(GameMap map,
                              int longestRoadMinimum, int longestRoadBonus,
                              int largestArmyMinimum, int largestArmyBonus) {
        this.map                 = map;
        this.longestRoadMinimum  = longestRoadMinimum;
        this.longestRoadBonus    = longestRoadBonus;
        this.largestArmyMinimum  = largestArmyMinimum;
        this.largestArmyBonus    = largestArmyBonus;
    }


    // ── 3. Achievement Checks ─────────────────────────────────────

    public void checkLargestArmy(Player player) {
        player.incrementKnightsPlayed();
        int knights = player.getKnightsPlayed();

        if (knights >= largestArmyMinimum && knights > largestArmySize) {
            if (largestArmyOwner != null && !largestArmyOwner.equals(player)) {
                largestArmyOwner.addVictoryPoints(-largestArmyBonus);
                player.addVictoryPoints(largestArmyBonus);
            } else if (largestArmyOwner == null) {
                player.addVictoryPoints(largestArmyBonus);
            }
            largestArmyOwner = player;
            largestArmySize  = knights;
        }
    }

    public void checkLongestRoad(Player player) {
        int length = calculateLongestRoad(player);

        if (length >= longestRoadMinimum && length > longestRoadLength) {
            if (longestRoadOwner != null && !longestRoadOwner.equals(player)) {
                longestRoadOwner.addVictoryPoints(-longestRoadBonus);
                player.addVictoryPoints(longestRoadBonus);
                longestRoadOwner = player;
            } else if (longestRoadOwner == null) {
                player.addVictoryPoints(longestRoadBonus);
                longestRoadOwner = player;
            }
            longestRoadLength = length;
        }
    }


    // ── 4. Getters ────────────────────────────────────────────────

    public Player getLongestRoadOwner() { return longestRoadOwner; }
    public Player getLargestArmyOwner() { return largestArmyOwner; }


    // ── 5. DFS: Longest Road Algorithm ───────────────────────────

    private int calculateLongestRoad(Player player) {
        int maxLength = 0;
        Set<Edge> visited = new HashSet<>();

        for (Edge edge : map.getEdges()) {
            if (edge.getStreet() == null || !edge.getStreet().getOwner().equals(player)) continue;

            for (Intersection start : edge.getEndpoints()) {
                Building building = start.getBuilding();
                if (building != null && !building.getOwner().equals(player)) continue;

                visited.clear();
                visited.add(edge);
                Intersection next = otherEndpoint(edge, start);
                int length = 1 + dfsLongestRoad(next, visited, player);
                maxLength = Math.max(maxLength, length);
            }
        }
        return maxLength;
    }

    private int dfsLongestRoad(Intersection intersection, Set<Edge> visited, Player player) {
        Building building = intersection.getBuilding();
        if (building != null && !building.getOwner().equals(player)) return 0;

        int best = 0;
        for (Edge edge : intersection.getAdjacentEdges()) {
            if (edge.getStreet() != null
                    && edge.getStreet().getOwner().equals(player)
                    && !visited.contains(edge)) {

                visited.add(edge);
                Intersection next = otherEndpoint(edge, intersection);
                int length = 1 + dfsLongestRoad(next, visited, player);
                best = Math.max(best, length);
                visited.remove(edge);
            }
        }
        return best;
    }

    private Intersection otherEndpoint(Edge edge, Intersection from) {
        return edge.getEndpoints()[0] == from
                ? edge.getEndpoints()[1]
                : edge.getEndpoints()[0];
    }
}
