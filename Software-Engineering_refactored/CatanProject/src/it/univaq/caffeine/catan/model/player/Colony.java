package it.univaq.caffeine.catan.model.player;

import it.univaq.caffeine.catan.model.enums.BuildingType;

public class Colony extends VertexBuilding {
    private final int victoryPoints;

    public Colony(String color, Player owner, int victoryPoints) {
        super(color, owner);
        this.victoryPoints = victoryPoints;
    }

    @Override
    public BuildingType getBuildingType() { return BuildingType.COLONY; }

    @Override
    public int getVictoryPoints() { return victoryPoints; }

    @Override
    public int getProductionAmount() { return 1; }
}
