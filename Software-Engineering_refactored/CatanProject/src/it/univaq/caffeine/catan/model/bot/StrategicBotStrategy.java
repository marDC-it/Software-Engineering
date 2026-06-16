package it.univaq.caffeine.catan.model.bot;

import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.board.HexagonalTile;
import it.univaq.caffeine.catan.model.board.Intersection;
import it.univaq.caffeine.catan.model.enums.BuildingType;
import it.univaq.caffeine.catan.model.player.BotPlayer;

import java.util.Comparator;
import java.util.List;

public class StrategicBotStrategy extends AbstractBotStrategy {

    @Override
    public Intersection chooseInitialPlacement(BotPlayer bot, Game game) {
        List<Intersection> candidates = findInitialPlacementCandidates(game);
        if (candidates.isEmpty()) return null;

        return candidates.stream()
                .max(Comparator.comparingInt(this::intersectionScore))
                .orElse(candidates.get(0));
    }

    @Override
    protected boolean chooseActionWithinLimit(BotPlayer bot, Game game) {
        BuildingType[] priority = {
                BuildingType.CITY,
                BuildingType.COLONY,
                BuildingType.DEVELOPMENT_CARD,
                BuildingType.STREET
        };

        for (BuildingType type : priority) {
            if (canAfford(bot, game, type) && tryBuild(bot, game, type)) {
                countAction();
                return true;
            }
        }
        return chooseTrade(bot, game);
    }

    @Override
    public boolean chooseTrade(BotPlayer bot, Game game) {
        return tryStrategicTrade(bot, game);
    }

    @Override
    protected int findColonyLocation(BotPlayer bot, Game game) {
        return findColonyCandidates(bot, game).stream()
                .max(Comparator.comparingInt(this::intersectionScore))
                .map(Intersection::getId)
                .orElse(-1);
    }

    @Override
    protected int findCityLocation(BotPlayer bot, Game game) {
        return findCityCandidates(bot, game).stream()
                .max(Comparator.comparingInt(this::intersectionScore))
                .map(Intersection::getId)
                .orElse(-1);
    }

    @Override
    public HexagonalTile chooseRobberTile(BotPlayer bot, Game game, HexagonalTile currentTile) {
        List<HexagonalTile> candidates = findRobberCandidates(game, currentTile);
        if (candidates.isEmpty()) return null;

        return candidates.stream()
                .filter(t -> t.getIntersections().stream()
                        .anyMatch(i -> i.getBuilding() != null
                                && !i.getBuilding().getOwner().equals(bot)))
                .max(Comparator.comparingInt(t ->
                        t.getToken() != null ? 6 - Math.abs(7 - t.getToken().getNumber()) : 0))
                .orElseGet(() -> {
                    java.util.Collections.shuffle(candidates, rnd);
                    return candidates.get(0);
                });
    }
}
