package it.univaq.caffeine.catan.model.cards;

import it.univaq.caffeine.catan.model.board.Edge;
import it.univaq.caffeine.catan.model.board.HexagonalTile;
import it.univaq.caffeine.catan.model.board.Intersection;
import it.univaq.caffeine.catan.model.enums.ResourceType;
import it.univaq.caffeine.catan.model.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CardPlayContext {

    private ResourceType selectedResourceType;
    private List<ResourceType> selectedResourceTypes = new ArrayList<>();
    private List<Edge> selectedEdges = new ArrayList<>();
    private Intersection selectedIntersection;
    private Player targetPlayer;
    private HexagonalTile robberTargetTile;

    private int collectedResourceCount;
    private boolean robberMoveRequired;
    private boolean roadPlacementRequired;
    private String message;

    public ResourceType getSelectedResourceType() {
        return selectedResourceType;
    }

    public void setSelectedResourceType(ResourceType selectedResourceType) {
        this.selectedResourceType = selectedResourceType;
    }

    public List<ResourceType> getSelectedResourceTypes() {
        return Collections.unmodifiableList(selectedResourceTypes);
    }

    public void setSelectedResourceTypes(List<ResourceType> selectedResourceTypes) {
        this.selectedResourceTypes = new ArrayList<>(selectedResourceTypes);
    }

    public List<Edge> getSelectedEdges() {
        return Collections.unmodifiableList(selectedEdges);
    }

    public void setSelectedEdges(List<Edge> selectedEdges) {
        this.selectedEdges = new ArrayList<>(selectedEdges);
    }

    public Intersection getSelectedIntersection() {
        return selectedIntersection;
    }

    public void setSelectedIntersection(Intersection selectedIntersection) {
        this.selectedIntersection = selectedIntersection;
    }

    public Player getTargetPlayer() {
        return targetPlayer;
    }

    public void setTargetPlayer(Player targetPlayer) {
        this.targetPlayer = targetPlayer;
    }

    public HexagonalTile getRobberTargetTile() {
        return robberTargetTile;
    }

    public void setRobberTargetTile(HexagonalTile robberTargetTile) {
        this.robberTargetTile = robberTargetTile;
    }

    public int getCollectedResourceCount() {
        return collectedResourceCount;
    }

    public void setCollectedResourceCount(int collectedResourceCount) {
        this.collectedResourceCount = collectedResourceCount;
    }

    public boolean isRobberMoveRequired() {
        return robberMoveRequired;
    }

    public void setRobberMoveRequired(boolean robberMoveRequired) {
        this.robberMoveRequired = robberMoveRequired;
    }

    public boolean isRoadPlacementRequired() {
        return roadPlacementRequired;
    }

    public void setRoadPlacementRequired(boolean roadPlacementRequired) {
        this.roadPlacementRequired = roadPlacementRequired;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
