package it.univaq.caffeine.catan.model.state;

import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.cards.CardPlayContext;
import it.univaq.caffeine.catan.model.enums.BuildingType;
import it.univaq.caffeine.catan.model.enums.GamePhase;

public interface GameState {

    GamePhase getPhase();

    default String getDisplayName() {
        return getPhase().getDisplayName();
    }

    default boolean isSetup() {
        return false;
    }

    default boolean isInitialPlacement() {
        return false;
    }

    default boolean isActive() {
        return false;
    }

    default boolean isBuildPhase() {
        return false;
    }

    default boolean isTradePhase() {
        return false;
    }

    default boolean isRobberPhase() {
        return false;
    }

    default boolean isGameOver() {
        return false;
    }

    default boolean isTurnPhase() {
        return isActive() || isBuildPhase() || isTradePhase() || isRobberPhase();
    }

    default boolean canTakeTurnActions() {
        return isActive();
    }

    default GameState confirmSetup(Game game) {
        throw invalid("confirm game setup");
    }

    default GameState beginBuildPhase(Game game, BuildingType buildingType) {
        throw invalid("start build phase");
    }

    default GameState cancelBuildPhase(Game game) {
        throw invalid("cancel build phase");
    }

    default GameState finishBuildPhase(Game game) {
        throw invalid("finish build phase");
    }

    default GameState beginTradePhase(Game game) {
        throw invalid("start trade phase");
    }

    default GameState finishTradePhase(Game game) {
        throw invalid("finish trade phase");
    }

    default GameState finishRobberPhase(Game game) {
        throw invalid("finish robber phase");
    }

    default GameState completeInitialPlacement(Game game) {
        throw invalid("complete initial placement");
    }

    default GameState afterDiceRoll(Game game, int diceResult) {
        return this;
    }

    default GameState afterDevelopmentCard(Game game, CardPlayContext context) {
        return this;
    }

    default GameState gameOver(Game game) {
        return GameOverState.INSTANCE;
    }

    default IllegalStateException invalid(String action) {
        return new IllegalStateException(
                "Cannot " + action + " while game phase is " + getDisplayName() + ".");
    }
}
