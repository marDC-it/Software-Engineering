package it.univaq.caffeine.catan.model.player;

import it.univaq.caffeine.catan.model.Bank;
import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.board.Edge;
import it.univaq.caffeine.catan.model.board.HexagonalTile;
import it.univaq.caffeine.catan.model.board.Intersection;
import it.univaq.caffeine.catan.model.bot.BotStrategy;
import it.univaq.caffeine.catan.model.bot.RandomBotStrategy;
import it.univaq.caffeine.catan.model.bot.StrategicBotStrategy;

import java.util.Map;
import java.util.Objects;

public class BotPlayer extends Player {

    private final BotStrategy strategy;

    public BotPlayer(String name, String color, String difficulty) {
        this(name, color, createStrategy(difficulty));
    }

    public BotPlayer(String name, String color, BotStrategy strategy) {
        super(name, color);
        this.strategy = Objects.requireNonNull(strategy, "strategy");
    }

    private static BotStrategy createStrategy(String difficulty) {
        if ("Strategic".equalsIgnoreCase(difficulty)) {
            return new StrategicBotStrategy();
        }
        return new RandomBotStrategy();
    }

    public Intersection chooseInitialPlacement(Game game) {
        return strategy.chooseInitialPlacement(this, game);
    }

    public Edge chooseInitialRoad(Game game, Colony lastColony) {
        return strategy.chooseInitialRoad(this, game, lastColony);
    }

    public boolean chooseAction(Game game) {
        return strategy.chooseAction(this, game);
    }

    public boolean chooseTrade(Game game) {
        return strategy.chooseTrade(this, game);
    }

    public HexagonalTile chooseRobberTile(Game game, HexagonalTile currentTile) {
        return strategy.chooseRobberTile(this, game, currentTile);
    }

    public void resetTurn() {
        strategy.resetTurn();
    }

    public String getAndClearLastTradeInfo() {
        return strategy.getAndClearLastTradeInfo();
    }

    @Override
    public void handleRobberDiscard(Bank bank,
                                    int cardsToDiscard,
                                    Map<Player, Integer> humanDiscards) {
        discardRandomResources(bank, cardsToDiscard);
    }

    @Override
    public boolean isBot() {
        return true;
    }

    @Override
    public String getDisplayIcon() {
        return "🤖";
    }

    @Override
    public String getTurnDisplaySuffix() {
        return " " + getDisplayIcon();
    }

    @Override
    protected String getRoleSuffix() {
        return "/Bot";
    }
}
