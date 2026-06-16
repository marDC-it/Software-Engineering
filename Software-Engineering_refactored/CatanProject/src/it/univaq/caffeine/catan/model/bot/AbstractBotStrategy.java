package it.univaq.caffeine.catan.model.bot;

import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.board.Edge;
import it.univaq.caffeine.catan.model.board.HexagonalTile;
import it.univaq.caffeine.catan.model.board.Intersection;
import it.univaq.caffeine.catan.model.enums.BuildingType;
import it.univaq.caffeine.catan.model.enums.ResourceType;
import it.univaq.caffeine.catan.model.player.BotPlayer;
import it.univaq.caffeine.catan.model.player.Building;
import it.univaq.caffeine.catan.model.player.Colony;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

abstract class AbstractBotStrategy implements BotStrategy {

    private static final int MAX_ACTIONS_PER_TURN = 5;

    protected final Random rnd = new Random();
    private int actionCount = 0;
    private String lastTradeInfo = null;

    @Override
    public Edge chooseInitialRoad(BotPlayer bot, Game game, Colony lastColony) {
        if (lastColony == null || lastColony.getIntersection() == null) return null;

        List<Edge> candidates = lastColony.getIntersection().getAdjacentEdges().stream()
                .filter(Edge::isFree)
                .collect(Collectors.toList());

        if (candidates.isEmpty()) return null;

        Collections.shuffle(candidates, rnd);
        return candidates.get(0);
    }

    @Override
    public final boolean chooseAction(BotPlayer bot, Game game) {
        if (actionCount >= MAX_ACTIONS_PER_TURN) return false;
        return chooseActionWithinLimit(bot, game);
    }

    protected abstract boolean chooseActionWithinLimit(BotPlayer bot, Game game);

    @Override
    public void resetTurn() {
        actionCount = 0;
    }

    protected void countAction() {
        actionCount++;
    }

    protected List<BuildingType> getAffordable(BotPlayer bot, Game game) {
        return Arrays.stream(BuildingType.values())
                .filter(type -> canAfford(bot, game, type))
                .collect(Collectors.toList());
    }

    protected boolean canAfford(BotPlayer bot, Game game, BuildingType type) {
        Map<ResourceType, Integer> cost = game.getCostRegistry().getCost(type);
        if (cost.isEmpty()) return false;
        return cost.entrySet().stream()
                .allMatch(e -> bot.hasResource(e.getKey(), e.getValue()));
    }

    protected boolean tryBuild(BotPlayer bot, Game game, BuildingType type) {
        try {
            if (type == BuildingType.DEVELOPMENT_CARD) {
                game.buyDevelopmentCard();
                return true;
            }

            int locationId = findBuildLocation(bot, game, type);
            if (locationId < 0) return false;

            game.beginBuildPhase(type);
            game.build(type, locationId);
            return true;
        } catch (Exception e) {
            closeBuildPhaseIfOpen(game);
            return false;
        }
    }

    private int findBuildLocation(BotPlayer bot, Game game, BuildingType type) {
        return switch (type) {
            case COLONY -> findColonyLocation(bot, game);
            case CITY   -> findCityLocation(bot, game);
            case STREET -> findStreetLocation(bot, game);
            default     -> -1;
        };
    }

    protected int findColonyLocation(BotPlayer bot, Game game) {
        List<Intersection> candidates = findColonyCandidates(bot, game);
        if (candidates.isEmpty()) return -1;

        Collections.shuffle(candidates, rnd);
        return candidates.get(0).getId();
    }

    protected int findCityLocation(BotPlayer bot, Game game) {
        List<Intersection> colonies = findCityCandidates(bot, game);
        if (colonies.isEmpty()) return -1;

        Collections.shuffle(colonies, rnd);
        return colonies.get(0).getId();
    }

    protected int findStreetLocation(BotPlayer bot, Game game) {
        List<Edge> candidates = game.getMap().getEdges().stream()
                .filter(Edge::isFree)
                .filter(e -> {
                    for (Intersection ep : e.getEndpoints()) {
                        Building b = ep.getBuilding();
                        if (b != null && b.getOwner().equals(bot)) return true;

                        for (Edge adj : ep.getAdjacentEdges()) {
                            if (adj != e && adj.getStreet() != null
                                    && adj.getStreet().getOwner().equals(bot)) return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());

        if (candidates.isEmpty()) return -1;

        Collections.shuffle(candidates, rnd);
        return candidates.get(0).getId();
    }

    protected List<Intersection> findInitialPlacementCandidates(Game game) {
        return game.getMap().getIntersections().stream()
                .filter(Intersection::isFree)
                .filter(i -> i.getAdjacentIntersections().stream().allMatch(Intersection::isFree))
                .collect(Collectors.toList());
    }

    protected List<Intersection> findColonyCandidates(BotPlayer bot, Game game) {
        return game.getMap().getIntersections().stream()
                .filter(Intersection::isFree)
                .filter(i -> i.getAdjacentIntersections().stream().allMatch(Intersection::isFree))
                .filter(i -> i.getAdjacentEdges().stream()
                        .anyMatch(e -> e.getStreet() != null
                                && e.getStreet().getOwner().equals(bot)))
                .collect(Collectors.toList());
    }

    protected List<Intersection> findCityCandidates(BotPlayer bot, Game game) {
        return game.getMap().getIntersections().stream()
                .filter(i -> i.getBuilding() instanceof Colony
                        && i.getBuilding().getOwner().equals(bot))
                .collect(Collectors.toList());
    }

    protected List<HexagonalTile> findRobberCandidates(Game game, HexagonalTile currentTile) {
        List<HexagonalTile> candidates = game.getMap().getHexagons().stream()
                .filter(t -> t != currentTile)
                .filter(t -> t.getTerrainType() != ResourceType.DESERT)
                .collect(Collectors.toList());

        if (!candidates.isEmpty()) return candidates;

        return game.getMap().getHexagons().stream()
                .filter(t -> t != currentTile)
                .collect(Collectors.toList());
    }

    protected boolean tryEasyTrade(BotPlayer bot, Game game) {
        if (rnd.nextBoolean()) return false;

        ResourceType bestGive = null;
        int maxCount = 0;

        for (ResourceType rt : tradableResources()) {
            int count = bot.countResources(rt);
            int rate = game.getEffectiveTradeRate(bot, rt);

            if (count >= rate && count > maxCount) {
                maxCount = count;
                bestGive = rt;
            }
        }

        if (bestGive == null) return false;

        final ResourceType give = bestGive;
        List<ResourceType> options = Arrays.stream(tradableResources())
                .filter(rt -> rt != give)
                .collect(Collectors.toList());
        ResourceType receive = options.get(rnd.nextInt(options.size()));

        try {
            game.beginTradePhase();
            game.bankTrade(give, receive);
            recordTrade("Gave " + game.getEffectiveTradeRate(bot, give) + "x " + give.name()
                    + "  →  Received 1x " + receive.name());
            game.finishTradePhase();
            return true;
        } catch (Exception ignored) {
            closeTradePhaseIfOpen(game);
            return false;
        }
    }

    protected boolean tryStrategicTrade(BotPlayer bot, Game game) {
        BuildingType[] priority = {
                BuildingType.CITY, BuildingType.COLONY,
                BuildingType.DEVELOPMENT_CARD, BuildingType.STREET
        };

        for (BuildingType target : priority) {
            Map<ResourceType, Integer> cost = game.getCostRegistry().getCost(target);
            if (cost.isEmpty()) continue;

            for (var entry : cost.entrySet()) {
                ResourceType need = entry.getKey();
                int needQty = entry.getValue();

                if (bot.countResources(need) >= needQty) continue;

                for (ResourceType give : tradableResources()) {
                    if (give == need) continue;

                    int rate = game.getEffectiveTradeRate(bot, give);
                    if (bot.hasResource(give, rate)) {
                        try {
                            game.beginTradePhase();
                            game.bankTrade(give, need);
                            recordTrade("Gave " + rate + "x " + give.name()
                                    + "  →  Received 1x " + need.name());
                            game.finishTradePhase();
                            return true;
                        } catch (Exception ignored) {
                            closeTradePhaseIfOpen(game);
                        }
                    }
                }
            }
        }
        return false;
    }

    protected ResourceType[] tradableResources() {
        return new ResourceType[]{
                ResourceType.BRICK, ResourceType.WOOD,
                ResourceType.WHEAT, ResourceType.SHEEP, ResourceType.ORE
        };
    }

    protected int intersectionScore(Intersection inter) {
        int score = 0;
        for (HexagonalTile tile : inter.getAdjacentTiles()) {
            if (tile.getToken() != null) {
                int n = tile.getToken().getNumber();
                score += 6 - Math.abs(7 - n);
            }
        }
        return score;
    }

    protected void recordTrade(String tradeInfo) {
        lastTradeInfo = tradeInfo;
    }

    private void closeBuildPhaseIfOpen(Game game) {
        if (game.isBuildPhase() && game.getFreeRoadsRemaining() == 0) {
            try {
                game.cancelBuildPhase();
            } catch (Exception ignored) {
            }
        }
    }

    private void closeTradePhaseIfOpen(Game game) {
        if (game.isTradePhase()) {
            try {
                game.finishTradePhase();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public String getAndClearLastTradeInfo() {
        String info = lastTradeInfo;
        lastTradeInfo = null;
        return info;
    }
}
