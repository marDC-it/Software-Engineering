package it.univaq.caffeine.catan.model.state;

import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.enums.BuildingType;
import it.univaq.caffeine.catan.model.enums.GamePhase;

public final class BuildPhaseState implements GameState {

    public static final BuildPhaseState INSTANCE = new BuildPhaseState();

    private BuildPhaseState() {
    }

    @Override
    public GamePhase getPhase() {
        return GamePhase.BUILD_PHASE;
    }

    @Override
    public boolean isBuildPhase() {
        return true;
    }

    @Override
    public GameState beginBuildPhase(Game game, BuildingType buildingType) {
        if (game.getFreeRoadsRemaining() > 0) {
            throw new IllegalStateException("Road Building card placement must be completed first.");
        }
        return this;
    }

    @Override
    public GameState cancelBuildPhase(Game game) {
        if (game.getFreeRoadsRemaining() > 0) {
            throw new IllegalStateException("Cannot cancel Road Building card placement.");
        }
        return ActiveState.INSTANCE;
    }

    @Override
    public GameState finishBuildPhase(Game game) {
        return ActiveState.INSTANCE;
    }
}
