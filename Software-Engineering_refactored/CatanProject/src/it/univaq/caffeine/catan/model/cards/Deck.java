package it.univaq.caffeine.catan.model.cards;

import java.util.*;

/**
 * Deck — development card deck whose composition is defined in game_config.json.
 * Standard Catan: 14 Knight, 2 Monopoly, 2 Discovery, 2 Road Building, 5 Victory Point cards.
 *
 * The composition map is injected by Game, which is the sole reader of
 * GameConfig in the model layer. Deck has no dependency on GameConfig or ConfigLoader.
 */
public class Deck {

    // ══════════════════════════════════════════════════════════════
    //  1. INSTANCE VARIABLES
    // ══════════════════════════════════════════════════════════════
    private final List<DevelopmentCard> cards = new ArrayList<>();
    private final int totalSize;

    // ══════════════════════════════════════════════════════════════
    //  2. CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════

    /**
     * Builds the deck from the given composition map.
     * Key: card type name (e.g. "KNIGHT"). Value: number of copies.
     * Game extracts this map from GameConfig and passes it here.
     *
     * @param deckComposition Card-type → count map (from game_config.json).
     */
    public Deck(Map<String, Integer> deckComposition) {
        deckComposition.forEach((typeName, count) -> {
            for (int i = 0; i < count; i++) {
                cards.add(DevelopmentCardFactory.create(typeName));
            }
        });
        totalSize = cards.size();
    }

    // ══════════════════════════════════════════════════════════════
    //  3. CORE LOGIC & ACTIONS
    // ══════════════════════════════════════════════════════════════
    public void shuffle() {
        Collections.shuffle(cards);
    }

    public DevelopmentCard drawCard() {
        return cards.isEmpty() ? null : cards.remove(0);
    }

    /** Count of cards of a specific configured key remaining. */
    public long countByDeckKey(String deckKey) {
        return cards.stream().filter(c -> c.getDeckKey().equals(deckKey)).count();
    }

    // ══════════════════════════════════════════════════════════════
    //  4. GETTERS & STATUS
    // ══════════════════════════════════════════════════════════════
    public int getTotalSize() {
        return totalSize;
    }

    public List<DevelopmentCard> getCards() {
        return cards;
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
