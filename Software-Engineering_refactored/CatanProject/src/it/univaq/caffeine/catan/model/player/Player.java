package it.univaq.caffeine.catan.model.player;

import it.univaq.caffeine.catan.model.Bank;
import it.univaq.caffeine.catan.model.cards.DevelopmentCard;
import it.univaq.caffeine.catan.model.cards.ResourceCard;
import it.univaq.caffeine.catan.model.enums.ResourceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Common base class for every player.
 * Human- and bot-specific behavior belongs in subclasses.
 */
public abstract class Player {

    // ── 1. Instance Variables ─────────────────────────────────────

    // Basic player data
    private final String name;
    private final String color;

    // Inventory and scoring
    private final List<DevelopmentCard> cards;
    private final List<Building>        buildings;
    private final List<ResourceCard>    resources;
    private int                         victoryPoints;
    private int                         knightsPlayed = 0;

    // Physical pieces available (injected by Game after creation)
    private int availableColonies;
    private int availableCities;
    private int availableStreets;


    // ── 2. Constructors ───────────────────────────────────────────

    protected Player(String name, String color) {
        this.name          = name;
        this.color         = color;
        this.cards         = new ArrayList<>();
        this.buildings     = new ArrayList<>();
        this.resources     = new ArrayList<>();
        this.victoryPoints = 0;
    }


    // ── 3. Core Logic: Resources, Buildings & Points ──────────────

    public void addBuilding(Building b) {
        buildings.add(b);
    }

    public void addResource(ResourceCard c) {
        resources.add(c);
    }

    public void addDevelopmentCard(DevelopmentCard card) {
        cards.add(card);
    }

    /**
     * Removes a specific quantity of a given resource from the player's hand.
     */
    public void removeResource(ResourceType type, int qty) {
        int removed = 0;
        Iterator<ResourceCard> it = resources.iterator();
        while (it.hasNext() && removed < qty) {
            if (it.next().getType() == type) {
                it.remove();
                removed++;
            }
        }
    }

    /**
     * Checks if the player has at least the specified quantity of a given resource.
     */
    public boolean hasResource(ResourceType type, int qty) {
        return countResources(type) >= qty;
    }

    public int countResources(ResourceType type) {
        return (int) resources.stream().filter(c -> c.getType() == type).count();
    }

    public void addVictoryPoints(int pts) {
        victoryPoints += pts;
    }

    public void incrementKnightsPlayed() {
        knightsPlayed++;
    }

    /**
     * Polymorphic hook used when the robber forces discards.
     * Human players report a pending discard to the UI; bots discard immediately.
     */
    public abstract void handleRobberDiscard(Bank bank,
                                             int cardsToDiscard,
                                             Map<Player, Integer> humanDiscards);

    protected void discardRandomResources(Bank bank, int count) {
        List<ResourceCard> res = new ArrayList<>(resources);
        Collections.shuffle(res);
        for (int i = 0; i < Math.min(count, res.size()); i++) {
            bank.receiveResourcesFrom(this, res.get(i).getType(), 1);
        }
    }


    // ── 4. Available Pieces Management ────────────────────────────

    /** Set by Game.confirmGameSetup() using cfg.getMaxColonyForPlayer() */
    public void setAvailableColonies(int n) { this.availableColonies = n; }

    /** Set by Game.confirmGameSetup() using cfg.getMaxCitiesForPlayer() */
    public void setAvailableCities(int n)   { this.availableCities   = n; }

    /** Set by Game.confirmGameSetup() using cfg.getMaxStreetsForPlayer() */
    public void setAvailableStreets(int n)  { this.availableStreets  = n; }

    /** SD6 msg 1.1.8 — called by Game.placeColony() */
    public void decrementAvailableColonies() { availableColonies--; }

    /** SD7 — called by Game.placeRoad() */
    public void decrementAvailableStreets()  { availableStreets--; }

    /** Called by Game.placeCity() */
    public void decrementAvailableCities()   { availableCities--; }


    // ── 5. Getters ────────────────────────────────────────────────

    public String                getName()              { return name; }
    public String                getColor()             { return color; }
    public abstract boolean      isBot();
    public boolean               isHuman()              { return !isBot(); }
    public String                getDisplayIcon()       { return "👤"; }
    public String                getTurnDisplaySuffix() { return ""; }

    public List<Building>        getBuildings()         { return buildings; }
    public List<ResourceCard>    getResources()         { return resources; }
    public List<DevelopmentCard> getCards()             { return cards; }
    public int                   getVictoryPoints()     { return victoryPoints; }

    public int                   getAvailableColonies() { return availableColonies; }
    public int                   getAvailableCities()   { return availableCities; }
    public int                   getAvailableStreets()  { return availableStreets; }

    public int                   getKnightsPlayed()     { return knightsPlayed; }


    // ── 6. Overrides ──────────────────────────────────────────────

    protected String getRoleSuffix() {
        return "";
    }

    @Override
    public String toString() {
        return name + " (" + color + getRoleSuffix() + ") VP:" + victoryPoints;
    }
}
