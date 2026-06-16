package it.univaq.caffeine.catan.model.cards;

import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.enums.ResourceType;
import it.univaq.caffeine.catan.model.player.Player;

public class MonopolyCard extends DevelopmentCard {

    public MonopolyCard() {
        super("MONOPOLY", "Monopoly", "Take all resources of one chosen type from other players.");
    }

    @Override
    public InputRequirement getInputRequirement() {
        return InputRequirement.ONE_RESOURCE;
    }

    @Override
    protected void applyEffect(Game game, Player player, CardPlayContext context) {
        ResourceType resource = context.getSelectedResourceType();
        if (resource == null) {
            throw new IllegalArgumentException("Monopoly requires one selected resource.");
        }

        int collected = game.applyMonopolyCard(player, resource);
        context.setCollectedResourceCount(collected);
        context.setMessage("Monopoly on " + resource.name() + "! Collected: " + collected + " cards.");
    }

    @Override
    public boolean isProgress() {
        return true;
    }
}
