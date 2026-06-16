package it.univaq.caffeine.catan.model.player;

import it.univaq.caffeine.catan.model.board.Intersection;

public abstract class VertexBuilding extends Building {

    private Intersection intersection;

    protected VertexBuilding(String color, Player owner) {
        super(color, owner);
    }

    public void setIntersection(Intersection intersection) {
        this.intersection = intersection;
    }

    @Override
    public Intersection getIntersection() {
        return intersection;
    }

    @Override
    public abstract int getProductionAmount();
}
