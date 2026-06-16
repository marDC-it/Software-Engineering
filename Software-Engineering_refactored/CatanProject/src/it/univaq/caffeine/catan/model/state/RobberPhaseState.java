package it.univaq.caffeine.catan.model.state;

import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.enums.GamePhase;

public final class RobberPhaseState implements GameState {

    public static final RobberPhaseState INSTANCE = new RobberPhaseState();

    private RobberPhaseState() {
    }

    @Override
    public GamePhase getPhase() {
        return GamePhase.ROBBER_PHASE;
    }

    @Override
    public boolean isRobberPhase() {
        return true;
    }

    @Override
    public GameState finishRobberPhase(Game game) {
        return ActiveState.INSTANCE;
    }
}
