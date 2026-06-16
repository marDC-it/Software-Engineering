package it.univaq.caffeine.catan.model.board;

import it.univaq.caffeine.catan.model.enums.ResourceType;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single hexagonal tile on the game board.
 * Stores layout coordinates, terrain type, associated graph elements
 * (intersections and edges), and the number token assigned to it.
 */
public class HexagonalTile {

    // ── 1. Instance Variables ─────────────────────────────────────

    /** Unique identifier for the tile */
    private final int id;

    /** The type of resource this tile produces (e.g., WOOD, BRICK, DESERT) */
    private final ResourceType terrainType;

    /** Screen X coordinate for the center of the hex (used for rendering) */
    private final double cx;

    /** Screen Y coordinate for the center of the hex (used for rendering) */
    private final double cy;

    /** List of the 6 vertices (intersections) surrounding this hex */
    private final List<Intersection> intersections;

    /** List of the 6 borders (edges) surrounding this hex */
    private final List<Edge> edges;

    /** The number token placed on this tile (determines when it produces resources) */
    private Token token;


    // ── 2. Constructor ────────────────────────────────────────────

    /**
     * Initializes a new hexagonal tile with its core properties.
     * * @param id          Unique ID of the tile.
     * @param terrainType The terrain/resource type.
     * @param cx          Center X pixel coordinate.
     * @param cy          Center Y pixel coordinate.
     */
    public HexagonalTile(int id, ResourceType terrainType, double cx, double cy) {
        this.id            = id;
        this.terrainType   = terrainType;
        this.cx            = cx;
        this.cy            = cy;
        this.intersections = new ArrayList<>();
        this.edges         = new ArrayList<>();
    }


    // ── 3. Core Logic ─────────────────────────────────────────────

    /**
     * Determines the resource produced by this tile.
     * * @return The ResourceType produced, or null if it's a DESERT.
     */
    public ResourceType produceResource() {
        return (terrainType == ResourceType.DESERT) ? null : terrainType;
    }


    // ── 4. Graph Building (Setters & Adders) ──────────────────────

    public void addIntersection(Intersection i) {
        intersections.add(i);
    }

    public void addEdge(Edge e) {
        edges.add(e);
    }

    public void setToken(Token t) {
        this.token = t;
    }


    // ── 5. Getters ────────────────────────────────────────────────

    public int getId() {
        return id;
    }

    public ResourceType getTerrainType() {
        return terrainType;
    }

    public double getCenterX() {
        return cx;
    }

    public double getCenterY() {
        return cy;
    }

    public List<Intersection> getIntersections() {
        return intersections;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public Token getToken() {
        return token;
    }


    // ── 6. Overrides ──────────────────────────────────────────────

    /**
     * Returns a short string representation of the tile for debugging.
     * Example: "WO[6]" for Wood with a 6 token, or "DE[D]" for Desert.
     */
    @Override
    public String toString() {
        String tok = (token != null) ? "[" + token.getNumber() + "]" : "[D]";
        return terrainType.name().substring(0, 2) + tok;
    }

}