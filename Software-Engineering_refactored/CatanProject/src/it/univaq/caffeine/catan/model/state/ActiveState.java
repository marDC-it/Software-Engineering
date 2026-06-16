package it.univaq.caffeine.catan.model.state;

import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.cards.CardPlayContext;
import it.univaq.caffeine.catan.model.enums.BuildingType;
import it.univaq.caffeine.catan.model.enums.GamePhase;

public final class ActiveState implements GameState {

    public static final ActiveState INSTANCE = new ActiveState();

    private ActiveState() {
    }

    @Override
    public GamePhase getPhase() {
        return GamePhase.ACTIVE;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public GameState beginBuildPhase(Game game, BuildingType buildingType) {
        return BuildPhaseState.INSTANCE;
    }

    @Override
    public GameState beginTradePhase(Game game) {
        return TradePhaseState.INSTANCE;
    }

    @Override
    public GameState afterDiceRoll(Game game, int diceResult) {
        return diceResult == 7 ? RobberPhaseState.INSTANCE : this;
    }

    @Override
    public GameState afterDevelopmentCard(Game game, CardPlayContext context) {
        if (context.isRobberMoveRequired()) {
            return RobberPhaseState.INSTANCE;
        }
        if (context.isRoadPlacementRequired()) {
            return BuildPhaseState.INSTANCE;
        }
        return this;
    }
}
