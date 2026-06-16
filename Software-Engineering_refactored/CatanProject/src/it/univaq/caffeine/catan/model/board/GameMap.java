package it.univaq.caffeine.catan.model.board;

import it.univaq.caffeine.catan.model.enums.ResourceType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the hexagonal game board graph.
 *
 * Rendering constants (HEX_SIZE, CENTER_X, CENTER_Y) remain here —
 * they are layout parameters, not core game rules.
 *
 * All game-data (terrain distribution, tokens, tile coordinates, ports)
 * are injected by Game as plain Java types. GameMap has no dependency
 * on GameConfig or ConfigLoader.
 */
public class GameMap {

    // ── 1. Constants (Layout Parameters) ──────────────────────────

    public static final double HEX_SIZE = 60.0;
    public static final double CENTER_X = 380.0;
    public static final double CENTER_Y = 415.0;


    // ── 2. Instance Variables ─────────────────────────────────────

    // Board-data injected by Game (extracted from GameConfig there)
    private final String[] terrainDist;
    private final int[]    tokenNumbers;
    private final int[][]  tileAxial;
    private final List<Port> configuredPorts;

    // Graph collections
    private final List<HexagonalTile> hexagons      = new ArrayList<>();
    private final List<Intersection>  intersections = new ArrayList<>();
    private final List<Edge>          edges         = new ArrayList<>();
    private final List<Token>         tokens        = new ArrayList<>();
    private final List<Port>          ports         = new ArrayList<>();

    // Indexing and tracking for graph creation
    private final Map<String, Intersection> intersectionIndex = new HashMap<>();
    private final Map<String, Edge>         edgeIndex         = new HashMap<>();
    private int nextIntersectionId = 0;
    private int nextEdgeId         = 0;


    // ── 3. Constructor ────────────────────────────────────────────

    /**
     * Receives all board data as plain Java types.
     * Game extracts these values from GameConfig and passes them here —
     * GameMap itself never touches GameConfig or ConfigLoader.
     *
     * @param terrainDist     ResourceType names for each tile (e.g. "WOOD", "DESERT").
     * @param tokenNumbers    Number tokens to assign to non-desert tiles.
     * @param tileAxial       Axial [q, r] coordinates for each tile.
     * @param configuredPorts Pre-built Port objects (created by Game from GameConfig).
     */
    public GameMap(String[] terrainDist,
                   int[]    tokenNumbers,
                   int[][]  tileAxial,
                   List<Port> configuredPorts) {
        this.terrainDist     = terrainDist;
        this.tokenNumbers    = tokenNumbers;
        this.tileAxial       = tileAxial;
        this.configuredPorts = configuredPorts;
    }


    // ── 4. Core Logic: Map Generation ─────────────────────────────

    /**
     * Main execution sequence to build the entire board graph.
     */
    public void generateMap() {
        buildTiles();
        linkAdjacentIntersections();
        assignTokens();
        generatePorts();
        linkPortsToIntersections();
    }

    /**
     * Builds the hexagonal tiles, computing their coordinates and linking
     * them to their corresponding intersections and edges.
     */
    private void buildTiles() {
        List<ResourceType> terrains = new ArrayList<>();
        for (String name : terrainDist) {
            terrains.add(ResourceType.valueOf(name));
        }
        Collections.shuffle(terrains);

        for (int t = 0; t < tileAxial.length; t++) {
            int    q  = tileAxial[t][0];
            int    r  = tileAxial[t][1];
            double cx = axialToPixelX(q, r);
            double cy = axialToPixelY(q, r);

            HexagonalTile tile = new HexagonalTile(t, terrains.get(t), cx, cy);
            hexagons.add(tile);

            Intersection[] verts = new Intersection[6];
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(-90.0 + 60.0 * i);
                verts[i] = findOrCreateIntersection(
                        cx + HEX_SIZE * Math.cos(angle),
                        cy + HEX_SIZE * Math.sin(angle));
                tile.addIntersection(verts[i]);
                verts[i].addTile(tile);
            }

            for (int i = 0; i < 6; i++) {
                Edge e = findOrCreateEdge(verts[i], verts[(i + 1) % 6]);
                tile.addEdge(e);
                e.addTile(tile);
            }
        }
    }

    /**
     * Establishes direct neighbor relationships between adjacent intersections
     * by traversing the generated edges.
     */
    private void linkAdjacentIntersections() {
        for (Edge e : edges) {
            Intersection a = e.getEndpoints()[0];
            Intersection b = e.getEndpoints()[1];
            a.addAdjacentIntersection(b);
            b.addAdjacentIntersection(a);
        }
    }

    /**
     * Assigns number tokens to non-desert hexagonal tiles.
     */
    private void assignTokens() {
        List<Integer> tokenNums = new ArrayList<>();
        for (int n : tokenNumbers) {
            tokenNums.add(n);
        }
        Collections.shuffle(tokenNums);

        int ti = 0;
        for (HexagonalTile tile : hexagons) {
            if (tile.getTerrainType() != ResourceType.DESERT) {
                Token tok = new Token(tokenNums.get(ti++));
                tokens.add(tok);
                tile.setToken(tok);
            }
        }
    }

    /**
     * Copies the pre-built Port objects (created by Game from GameConfig)
     * into the live ports list. Each Port already carries its type, rate,
     * and display coordinates — no further conversion needed here.
     */
    private void generatePorts() {
        ports.addAll(configuredPorts);
    }

    /**
     * Links each port to its 2 closest intersections.
     * The 2 closest intersections represent the physical coastal edge
     * adjacent to the port on the map.
     */
    private void linkPortsToIntersections() {
        for (Port port : ports) {
            List<Intersection> adj = intersections.stream()
                    .sorted(Comparator.comparingDouble(i ->
                            Math.hypot(i.getX() - port.getDisplayX(),
                                    i.getY() - port.getDisplayY())))
                    .limit(2)
                    .collect(Collectors.toList());
            port.setAdjacentIntersections(adj);
        }
    }


    // ── 5. Helpers: Graph Creation ────────────────────────────────

    private Intersection findOrCreateIntersection(double x, double y) {
        String key = Math.round(x) + "_" + Math.round(y);
        if (intersectionIndex.containsKey(key)) {
            return intersectionIndex.get(key);
        }

        Intersection i = new Intersection(nextIntersectionId++, x, y);
        intersectionIndex.put(key, i);
        intersections.add(i);
        return i;
    }

    private Edge findOrCreateEdge(Intersection a, Intersection b) {
        int lo = Math.min(a.getId(), b.getId());
        int hi = Math.max(a.getId(), b.getId());
        String key = lo + "_" + hi;

        if (edgeIndex.containsKey(key)) {
            return edgeIndex.get(key);
        }

        Edge e = new Edge(nextEdgeId++, a, b);
        edgeIndex.put(key, e);
        edges.add(e);
        a.addEdge(e);
        b.addEdge(e);
        return e;
    }


    // ── 6. Helpers: Coordinate Conversion (Math) ──────────────────

    public static double axialToPixelX(int q, int r) {
        return CENTER_X + HEX_SIZE * Math.sqrt(3) * (q + r / 2.0);
    }

    public static double axialToPixelY(int q, int r) {
        return CENTER_Y + HEX_SIZE * 1.5 * r;
    }


    // ── 7. Getters & Look-ups ─────────────────────────────────────

    public Intersection getIntersection(int id) {
        return intersections.stream()
                .filter(i -> i.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public Edge getEdge(int id) {
        return edges.stream()
                .filter(e -> e.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public List<HexagonalTile> getHexagons()      { return hexagons; }
    public List<Intersection>  getIntersections() { return intersections; }
    public List<Edge>          getEdges()         { return edges; }
    public List<Token>         getTokens()        { return tokens; }
    public List<Port>          getPorts()         { return ports; }
}
