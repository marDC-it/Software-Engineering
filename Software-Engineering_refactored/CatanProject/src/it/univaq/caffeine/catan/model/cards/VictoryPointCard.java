package it.univaq.caffeine.catan.model.cards;

import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.player.Player;

public class VictoryPointCard extends DevelopmentCard {

    public VictoryPointCard(String deckKey, String name, String description) {
        super(deckKey, name, description);
    }

    @Override
    public boolean canBePlayed(Game game, Player player) {
        return false;
    }

    @Override
    public boolean isVictoryPoint() {
        return true;
    }

    @Override
    public int getVictoryPoints() {
        return 1;
    }
}
