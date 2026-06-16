package it.univaq.caffeine.catan.model.bot;

import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.board.HexagonalTile;
import it.univaq.caffeine.catan.model.board.Intersection;
import it.univaq.caffeine.catan.model.enums.BuildingType;
import it.univaq.caffeine.catan.model.player.BotPlayer;

import java.util.Collections;
import java.util.List;

public class RandomBotStrategy extends AbstractBotStrategy {

    @Override
    public Intersection chooseInitialPlacement(BotPlayer bot, Game game) {
        List<Intersection> candidates = findInitialPlacementCandidates(game);
        if (candidates.isEmpty()) return null;

        Collections.shuffle(candidates, rnd);
        return candidates.get(0);
    }

    @Override
    protected boolean chooseActionWithinLimit(BotPlayer bot, Game game) {
        List<BuildingType> affordable = getAffordable(bot, game);

        if (affordable.isEmpty()) {
            return chooseTrade(bot, game);
        }

        Collections.shuffle(affordable, rnd);
        for (BuildingType type : affordable) {
            if (tryBuild(bot, game, type)) {
                countAction();
                return rnd.nextBoolean();
            }
        }
        return chooseTrade(bot, game);
    }

    @Override
    public boolean chooseTrade(BotPlayer bot, Game game) {
        return tryEasyTrade(bot, game);
    }

    @Override
    public HexagonalTile chooseRobberTile(BotPlayer bot, Game game, HexagonalTile currentTile) {
        List<HexagonalTile> candidates = findRobberCandidates(game, currentTile);
        if (candidates.isEmpty()) return null;

        Collections.shuffle(candidates, rnd);
        return candidates.get(0);
    }
}
