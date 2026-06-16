package it.univaq.caffeine.catan.model.player;

import it.univaq.caffeine.catan.model.Bank;

import java.util.Map;

public class HumanPlayer extends Player {

    public HumanPlayer(String name, String color) {
        super(name, color);
    }

    @Override
    public void handleRobberDiscard(Bank bank,
                                    int cardsToDiscard,
                                    Map<Player, Integer> humanDiscards) {
        humanDiscards.put(this, cardsToDiscard);
    }

    @Override
    public boolean isBot() {
        return false;
    }
}
