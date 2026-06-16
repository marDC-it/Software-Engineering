package it.univaq.caffeine.catan.model.cards;

import it.univaq.caffeine.catan.model.Game;
import it.univaq.caffeine.catan.model.player.Player;

public abstract class DevelopmentCard {

    public enum InputRequirement {
        NONE,
        ONE_RESOURCE,
        TWO_RESOURCES
    }

    private final String deckKey;
    private final String name;
    private final String description;
    private boolean played;

    protected DevelopmentCard(String deckKey, String name, String description) {
        this.deckKey = deckKey;
        this.name = name;
        this.description = description;
        this.played = false;
    }

    public final void play(Game game, Player player, CardPlayContext context) {
        if (!canBePlayed(game, player)) {
            throw new IllegalStateException("No playable " + getDisplayName().replace("\n", " ")
                    + " card in hand.");
        }

        CardPlayContext effectiveContext = context != null ? context : new CardPlayContext();
        applyEffect(game, player, effectiveContext);
        played = true;
    }

    protected void applyEffect(Game game, Player player, CardPlayContext context) {

    }

    public boolean canBePlayed(Game game, Player player) {
        return !played && game.canPlayDevelopmentCards() && !isVictoryPoint();
    }

    public boolean isVictoryPoint() {
        return false;
    }

    public boolean isProgress() {
        return false;
    }

    public int getVictoryPoints() {
        return 0;
    }

    public InputRequirement getInputRequirement() {
        return InputRequirement.NONE;
    }

    public String getDisplayName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDeckKey() {
        return deckKey;
    }

    public boolean isPlayed() {
        return played;
    }

    @Override
    public String toString() {
        return getDisplayName().replace("\n", " ");
    }
}
