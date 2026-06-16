package it.univaq.caffeine.catan.model.enums;

public enum GamePhase {
    SETUP("Setup"),
    INITIAL_PLACEMENT("InitialPlacement"),
    ACTIVE("ActiveGame"),
    ROBBER_PHASE("RobberPhase"),
    TRADE_PHASE("TradePhase"),
    BUILD_PHASE("BuildPhase"),
    GAME_OVER("GameOver");

    private final String displayName;

    GamePhase(String displayName) {
        this.displayName = displayName;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isTurnPhase() {
        return this == ACTIVE
                || this == BUILD_PHASE
                || this == ROBBER_PHASE
                || this == TRADE_PHASE;
    }

    public boolean isGameOver() {
        return this == GAME_OVER;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
