package it.univaq.caffeine.catan.model.state;

import it.univaq.caffeine.catan.model.enums.GamePhase;

public final class GameOverState implements GameState {

    public static final GameOverState INSTANCE = new GameOverState();

    private GameOverState() {
    }

    @Override
    public GamePhase getPhase() {
        return GamePhase.GAME_OVER;
    }

    @Override
    public boolean isGameOver() {
        return true;
    }
}
