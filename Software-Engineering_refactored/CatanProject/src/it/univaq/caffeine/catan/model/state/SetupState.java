package it.univaq.caffeine.catan.model.state;

import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.enums.GamePhase;

public final class SetupState implements GameState {

    public static final SetupState INSTANCE = new SetupState();

    private SetupState() {
    }

    @Override
    public GamePhase getPhase() {
        return GamePhase.SETUP;
    }

    @Override
    public boolean isSetup() {
        return true;
    }

    @Override
    public GameState confirmSetup(Game game) {
        return InitialPlacementState.INSTANCE;
    }
}
