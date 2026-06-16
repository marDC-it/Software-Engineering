package it.univaq.caffeine.catan.model.player;

import it.univaq.caffeine.catan.model.board.Intersection;
import it.univaq.caffeine.catan.model.enums.BuildingType;
import it.univaq.caffeine.catan.model.enums.ResourceType;
import it.univaq.caffeine.catan.model.rules.CostRegistry;

import java.util.Map;

public abstract class Building {
    protected String color;
    protected Player owner;

    public Building(String color, Player owner) {
        this.color = color;
        this.owner = owner;
    }

    public Map<ResourceType, Integer> getCost(CostRegistry costRegistry) {
        return costRegistry.getCost(getBuildingType());
    }

    public abstract BuildingType getBuildingType();

    public abstract int getVictoryPoints();

    public int getProductionAmount() { return 0; }
    public Intersection getIntersection() { return null; }

    public String getColor() { return color; }
    public Player getOwner() { return owner; }
}
