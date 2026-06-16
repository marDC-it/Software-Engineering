package it.univaq.caffeine.catan.model.state;

import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.enums.GamePhase;

public final class TradePhaseState implements GameState {

    public static final TradePhaseState INSTANCE = new TradePhaseState();

    private TradePhaseState() {
    }

    @Override
    public GamePhase getPhase() {
        return GamePhase.TRADE_PHASE;
    }

    @Override
    public boolean isTradePhase() {
        return true;
    }

    @Override
    public GameState finishTradePhase(Game game) {
        return ActiveState.INSTANCE;
    }
}
