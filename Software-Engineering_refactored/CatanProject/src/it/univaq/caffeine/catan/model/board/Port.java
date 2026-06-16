package it.univaq.caffeine.catan.model.board;

import it.univaq.caffeine.catan.model.enums.ResourceType;
import java.util.ArrayList;
import java.util.List;

public class Port {
    private final ResourceType resourceType;
    private final int          exchangeRate;
    private final double       displayX;
    private final double       displayY;
    private final String       label;


    private List<Intersection> adjacentIntersections = new ArrayList<>();

    public Port(ResourceType resourceType, int exchangeRate, double x, double y) {
        this.resourceType = resourceType;
        this.exchangeRate = exchangeRate;
        this.displayX     = x;
        this.displayY     = y;
        this.label        = buildLabel();
    }

    private String buildLabel() {
        if (resourceType == null) return "3:1";
        return "2:1\n" + resourceType.name().substring(0, 2);
    }


    public void setAdjacentIntersections(List<Intersection> list) {
        this.adjacentIntersections = list;
    }
    public List<Intersection> getAdjacentIntersections() {
        return adjacentIntersections;
    }

    public boolean     isGeneric()         { return resourceType == null; }
    public ResourceType getResourceType()  { return resourceType; }
    public int    getExchangeRate()        { return exchangeRate; }
    public double getDisplayX()           { return displayX; }
    public double getDisplayY()           { return displayY; }
    public String getLabel()              { return label; }

    @Override public String toString() {
        return (resourceType == null ? "Generic" : resourceType.name())
                + " " + exchangeRate + ":1";
    }
}