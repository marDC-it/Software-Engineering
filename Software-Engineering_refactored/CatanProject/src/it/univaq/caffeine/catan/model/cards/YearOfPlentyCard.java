package it.univaq.caffeine.catan.model.cards;

import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.enums.ResourceType;
import it.univaq.caffeine.catan.model.player.Player;

import java.util.List;

public class YearOfPlentyCard extends DevelopmentCard {

    public YearOfPlentyCard() {
        super("DISCOVERY", "Discovery", "Take two chosen resources from the bank.");
    }

    @Override
    public InputRequirement getInputRequirement() {
        return InputRequirement.TWO_RESOURCES;
    }

    @Override
    protected void applyEffect(Game game, Player player, CardPlayContext context) {
        List<ResourceType> selected = context.getSelectedResourceTypes();
        if (selected.size() < 2) {
            throw new IllegalArgumentException("Discovery requires two selected resources.");
        }

        ResourceType first = selected.get(0);
        ResourceType second = selected.get(1);
        game.applyYearOfPlentyCard(player, first, second);
        context.setMessage("Received: " + first.name() + " + " + second.name());
    }

    @Override
    public boolean isProgress() {
        return true;
    }
}
