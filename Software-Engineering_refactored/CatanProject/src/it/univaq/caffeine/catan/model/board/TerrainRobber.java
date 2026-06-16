package it.univaq.caffeine.catan.model.board;

/**
 * Terrain robber implementation used by the current Catan rules.
 */
public final class TerrainRobber extends Robber {
    private HexagonalTile currentTile;

    /** Placed on the Desert tile at game start. */
    public TerrainRobber(HexagonalTile desertTile) {
        this.currentTile = desertTile;
    }

    @Override
    public boolean occupies(HexagonalTile tile) {
        return currentTile == tile;
    }

    @Override
    public void moveTo(HexagonalTile tile) {
        this.currentTile = tile;
    }

    @Override
    public HexagonalTile getCurrentTile() {
        return currentTile;
    }
}
