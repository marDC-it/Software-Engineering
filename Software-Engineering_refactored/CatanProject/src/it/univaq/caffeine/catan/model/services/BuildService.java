package it.univaq.caffeine.catan.model.services;

import it.univaq.caffeine.catan.model.Bank;
import it.univaq.caffeine.catan.model.board.Edge;
import it.univaq.caffeine.catan.model.board.GameMap;
import it.univaq.caffeine.catan.model.board.Intersection;
import it.univaq.caffeine.catan.model.enums.BuildingType;
import it.univaq.caffeine.catan.model.enums.ResourceType;
import it.univaq.caffeine.catan.model.player.Building;
import it.univaq.caffeine.catan.model.player.City;
import it.univaq.caffeine.catan.model.player.Colony;
import it.univaq.caffeine.catan.model.player.Player;
import it.univaq.caffeine.catan.model.player.Street;
import it.univaq.caffeine.catan.model.rules.CostRegistry;
import it.univaq.caffeine.catan.model.rules.GameRules;

import java.util.Map;

/**
 * GRASP Pure Fabrication — handles all building placement logic.
 *
 * VP values for Colony and City are injected as primitives by Game,
 * which is the sole reader of GameConfig in the model layer.
 * BuildService has no dependency on GameConfig or ConfigLoader.
 */
public class BuildService {

    // ── 1. Instance Variables ─────────────────────────────────────

    private final GameMap      map;
    private final Bank         bank;
    private final GameRules    rules;
    private final CostRegistry costRegistry;

    // Injected VP values (from Game, which reads GameConfig)
    private final int victoryPointsColony;
    private final int victoryPointsCity;


    // ── 2. Constructor ────────────────────────────────────────────

    /**
     * @param map                  The game board.
     * @param bank                 The resource bank.
     * @param rules                Game rules for placement validation.
     * @param costRegistry         Building cost table.
     * @param victoryPointsColony  VP awarded per colony (usually 1).
     * @param victoryPointsCity    VP awarded per city (usually 2).
     */
    public BuildService(GameMap map,
                        Bank bank,
                        GameRules rules,
                        CostRegistry costRegistry,
                        int victoryPointsColony,
                        int victoryPointsCity) {
        this.map                 = map;
        this.bank                = bank;
        this.rules               = rules;
        this.costRegistry        = costRegistry;
        this.victoryPointsColony = victoryPointsColony;
        this.victoryPointsCity   = victoryPointsCity;
    }


    // ── 3. Initial Placement ──────────────────────────────────────

    public Colony placeInitialColony(Player player, int intersectionId) {
        Intersection i = map.getIntersection(intersectionId);
        if (i == null) {
            throw new IllegalArgumentException("Intersection not found: " + intersectionId);
        }
        if (!i.isFree()) {
            throw new IllegalStateException("This intersection is already occupied.");
        }

        for (Intersection adj : i.getAdjacentIntersections()) {
            if (!adj.isFree()) {
                throw new IllegalStateException(
                        "<html>Too close to another settlement!<br><br>" +
                                "<b>Catan distance rule:</b> every settlement must be at<br>" +
                                "least <b>2 intersections away</b> from any other.</html>");
            }
        }

        if (!rules.validateColonyPlacement(i, player, map)) {
            throw new IllegalStateException("Invalid placement at intersection " + intersectionId);
        }

        Colony colony = new Colony(player.getColor(), player, victoryPointsColony);
        colony.setIntersection(i);
        i.addBuilding(colony);
        player.addBuilding(colony);
        player.decrementAvailableColonies();
        player.addVictoryPoints(colony.getVictoryPoints());
        return colony;
    }

    public void placeInitialRoad(Player player, Colony lastPlacedColony, int edgeId) {
        Edge e = map.getEdge(edgeId);
        if (e == null) {
            throw new IllegalArgumentException("Edge not found: " + edgeId);
        }
        if (!e.isFree()) {
            throw new IllegalStateException("This edge is already occupied.");
        }

        if (lastPlacedColony != null) {
            Intersection colonyIntersection = lastPlacedColony.getIntersection();
            if (colonyIntersection != null && !colonyIntersection.getAdjacentEdges().contains(e)) {
                throw new IllegalStateException(
                        "<html><b>Road not adjacent to this turn's settlement!</b></html>");
            }
        }

        if (!rules.validateRoadPlacement(e, lastPlacedColony, map)) {
            throw new IllegalStateException("Invalid road placement at edge " + edgeId);
        }

        Street street = new Street(player.getColor(), player);
        street.setEdge(e);
        e.setStreet(street);
        player.addBuilding(street);
        player.decrementAvailableStreets();
    }


    // ── 4. Main Build Dispatcher ──────────────────────────────────

    public void build(Player player, BuildingType buildingType, int locationId) {
        Map<ResourceType, Integer> cost = costRegistry.getCost(buildingType);

        for (var entry : cost.entrySet()) {
            if (!player.hasResource(entry.getKey(), entry.getValue())) {
                throw new IllegalStateException(
                        "Insufficient resources to build " + buildingType + ".");
            }
        }

        switch (buildingType) {
            case COLONY -> buildColony(player, locationId);
            case CITY   -> buildCity(player, locationId);
            case STREET -> buildStreet(player, locationId);
            default     -> throw new IllegalArgumentException(
                    "Unsupported building type: " + buildingType);
        }

        cost.forEach((res, qty) -> bank.receiveResourcesFrom(player, res, qty));
    }

    public void buildFreeStreet(Player player, int locationId) {
        buildStreet(player, locationId);
    }


    // ── 5. Private Build Methods ──────────────────────────────────

    private void buildColony(Player player, int locationId) {
        Intersection i = map.getIntersection(locationId);
        if (i == null) throw new IllegalArgumentException("Intersection not found: " + locationId);
        if (!i.isFree()) throw new IllegalStateException("Intersection already occupied.");

        for (Intersection adj : i.getAdjacentIntersections()) {
            if (!adj.isFree()) {
                throw new IllegalStateException(
                        "<html>Too close to another settlement!<br>Distance rule: 2 intersections minimum.</html>");
            }
        }

        boolean connected = i.getAdjacentEdges().stream()
                .anyMatch(e -> e.getStreet() != null
                        && e.getStreet().getOwner().equals(player));
        if (!connected) {
            throw new IllegalStateException("Colony must be connected to your road network.");
        }

        if (player.getAvailableColonies() <= 0) {
            throw new IllegalStateException("No colonies available.");
        }

        Colony colony = new Colony(player.getColor(), player, victoryPointsColony);
        colony.setIntersection(i);
        i.addBuilding(colony);
        player.addBuilding(colony);
        player.decrementAvailableColonies();
        player.addVictoryPoints(colony.getVictoryPoints());
    }

    private void buildCity(Player player, int locationId) {
        Intersection i = map.getIntersection(locationId);
        if (i == null) throw new IllegalArgumentException("Intersection not found: " + locationId);

        Building existing = i.getBuilding();
        if (!(existing instanceof Colony)) {
            throw new IllegalStateException("Can only upgrade a Colony to a City.");
        }
        if (!existing.getOwner().equals(player)) {
            throw new IllegalStateException("Can only upgrade your own Colony.");
        }

        if (player.getAvailableCities() <= 0) {
            throw new IllegalStateException("No cities available.");
        }

        int vpGain = victoryPointsCity - victoryPointsColony;

        City city = new City(player.getColor(), player, victoryPointsCity);
        city.setIntersection(i);
        i.addBuilding(city);
        player.getBuildings().remove(existing);
        player.addBuilding(city);
        player.decrementAvailableCities();
        player.setAvailableColonies(player.getAvailableColonies() + 1);
        player.addVictoryPoints(vpGain);
    }

    private void buildStreet(Player player, int locationId) {
        Edge e = map.getEdge(locationId);
        if (e == null) throw new IllegalArgumentException("Edge not found: " + locationId);
        if (!e.isFree()) throw new IllegalStateException("Edge already occupied.");

        if (player.getAvailableStreets() <= 0) {
            throw new IllegalStateException("No streets available.");
        }

        boolean connected = false;
        for (Intersection endpoint : e.getEndpoints()) {
            Building building = endpoint.getBuilding();
            if (building != null && building.getOwner().equals(player)) {
                connected = true;
                break;
            }
            for (Edge adjacent : endpoint.getAdjacentEdges()) {
                if (adjacent != e && adjacent.getStreet() != null
                        && adjacent.getStreet().getOwner().equals(player)) {
                    connected = true;
                    break;
                }
            }
            if (connected) break;
        }
        if (!connected) {
            throw new IllegalStateException(
                    "Street must be connected to your road network or settlement.");
        }

        Street street = new Street(player.getColor(), player);
        street.setEdge(e);
        e.setStreet(street);
        player.addBuilding(street);
        player.decrementAvailableStreets();
    }
}
