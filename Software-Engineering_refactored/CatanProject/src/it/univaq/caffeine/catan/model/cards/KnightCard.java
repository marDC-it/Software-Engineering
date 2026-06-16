package it.univaq.caffeine.catan.model.cards;

import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.player.Player;

public class KnightCard extends DevelopmentCard {

    public KnightCard() {
        super("KNIGHT", "Knight", "Move the robber and count toward Largest Army.");
    }

    @Override
    protected void applyEffect(Game game, Player player, CardPlayContext context) {
        game.applyKnightCard(player);
        context.setRobberMoveRequired(true);
        context.setMessage("Knight! Move the Robber to a new tile.");
    }
}
