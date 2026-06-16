package it.univaq.caffeine.catan.model.bot;

import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.board.Edge;
import it.univaq.caffeine.catan.model.board.HexagonalTile;
import it.univaq.caffeine.catan.model.board.Intersection;
import it.univaq.caffeine.catan.model.player.BotPlayer;
import it.univaq.caffeine.catan.model.player.Colony;

public interface BotStrategy {

    Intersection chooseInitialPlacement(BotPlayer bot, Game game);

    Edge chooseInitialRoad(BotPlayer bot, Game game, Colony lastColony);

    boolean chooseAction(BotPlayer bot, Game game);

    boolean chooseTrade(BotPlayer bot, Game game);

    HexagonalTile chooseRobberTile(BotPlayer bot, Game game, HexagonalTile currentTile);

    void resetTurn();

    String getAndClearLastTradeInfo();
}
