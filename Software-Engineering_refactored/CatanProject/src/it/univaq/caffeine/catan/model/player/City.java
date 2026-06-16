package it.univaq.caffeine.catan.model.player;

import it.univaq.caffeine.catan.model.enums.BuildingType;

public class City extends VertexBuilding {
    private final int victoryPoints;

    public City(String color, Player owner, int victoryPoints) {
        super(color, owner);
        this.victoryPoints = victoryPoints;
    }

    @Override
    public BuildingType getBuildingType() { return BuildingType.CITY; }

    @Override
    public int getVictoryPoints() { return victoryPoints; }

    @Override
    public int getProductionAmount() { return 2; }
}
