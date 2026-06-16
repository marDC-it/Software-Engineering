package it.univaq.caffeine.catan.model.board;

/**
 * Abstraction for robber-like tokens.
 * The current game uses only a terrain robber; future variants can specialize the same contract.
 */
public abstract class Robber {

    public abstract boolean occupies(HexagonalTile tile);

    public abstract void moveTo(HexagonalTile tile);

    public abstract HexagonalTile getCurrentTile();

    @Override
    public String toString() {
        HexagonalTile currentTile = getCurrentTile();
        return "Robber on: " + (currentTile != null ? currentTile.getTerrainType() : "?");
    }
}
