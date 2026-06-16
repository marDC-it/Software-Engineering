package it.univaq.caffeine.catan.model.cards;

import java.util.Map;
import java.util.function.Supplier;

public final class DevelopmentCardFactory {

    private static final Map<String, Supplier<DevelopmentCard>> FACTORIES = Map.of(
            "KNIGHT", KnightCard::new,
            "MONOPOLY", MonopolyCard::new,
            "DISCOVERY", YearOfPlentyCard::new,
            "ROAD_BUILDING", RoadBuildingCard::new,
            "TOWN_HALL", () -> new VictoryPointCard(
                    "TOWN_HALL", "Town Hall", "Worth 1 hidden victory point."),
            "MARKET_SQUARE", () -> new VictoryPointCard(
                    "MARKET_SQUARE", "Market\nSquare", "Worth 1 hidden victory point."),
            "CATHEDRAL", () -> new VictoryPointCard(
                    "CATHEDRAL", "Cathedral", "Worth 1 hidden victory point."),
            "LIBRARY", () -> new VictoryPointCard(
                    "LIBRARY", "Library", "Worth 1 hidden victory point."),
            "UNIVERSITY", () -> new VictoryPointCard(
                    "UNIVERSITY", "University", "Worth 1 hidden victory point.")
    );

    private DevelopmentCardFactory() {
    }

    public static DevelopmentCard create(String deckKey) {
        Supplier<DevelopmentCard> factory = FACTORIES.get(deckKey);
        if (factory == null) {
            throw new IllegalArgumentException("Unsupported development card type: " + deckKey);
        }
        return factory.get();
    }
}
