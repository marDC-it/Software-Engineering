package it.univaq.caffeine.catan.model.state;

import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.enums.GamePhase;

public final class InitialPlacementState implements GameState {

    public static final InitialPlacementState INSTANCE = new InitialPlacementState();

    private InitialPlacementState() {
    }

    @Override
    public GamePhase getPhase() {
        return GamePhase.INITIAL_PLACEMENT;
    }

    @Override
    public boolean isInitialPlacement() {
        return true;
    }

    @Override
    public GameState completeInitialPlacement(Game game) {
        return ActiveState.INSTANCE;
    }
}
