package it.univaq.caffeine.catan.model.cards;

import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.player.Player;

public class RoadBuildingCard extends DevelopmentCard {

    public RoadBuildingCard() {
        super("ROAD_BUILDING", "Road\nBuilding", "Place two roads without paying resources.");
    }

    @Override
    protected void applyEffect(Game game, Player player, CardPlayContext context) {
        game.applyRoadBuildingCard(player);
        context.setRoadPlacementRequired(true);
        context.setMessage("Road Building! Place 2 roads for free.");
    }

    @Override
    public boolean isProgress() {
        return true;
    }
}
