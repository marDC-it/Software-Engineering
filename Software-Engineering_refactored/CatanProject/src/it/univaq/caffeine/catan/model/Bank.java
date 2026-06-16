package it.univaq.caffeine.catan.model;

import it.univaq.caffeine.catan.model.cards.ResourceCard;
import it.univaq.caffeine.catan.model.enums.ResourceType;
import it.univaq.caffeine.catan.model.player.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * GRASP Information Expert for the central resource stock.
 * Manages how many resource cards are currently available in the bank.
 *
 * The initial quantity per type is injected as a primitive integer from Game,
 * which is the sole reader of GameConfig in the model layer.
 */
public class Bank {

    // ── 1. Instance Variables ─────────────────────────────────────

    /** Tracks the current quantity of available cards for each resource type */
    private final Map<ResourceType, Integer> resourceStock;


    // ── 2. Constructor ────────────────────────────────────────────

    /**
     *
     * @param resourcesPerType Initial stock per resource type (usually 19).
     */
    public Bank(int resourcesPerType) {
        resourceStock = new EnumMap<>(ResourceType.class);
        for (ResourceType t : ResourceType.values()) {
            if (t != ResourceType.DESERT) {
                resourceStock.put(t, resourcesPerType);
            }
        }
    }


    // ── 3. Resource Operations ────────────────────────────────────

    /**
     * Checks if the bank holds at least the requested quantity of a specific resource.
     *
     * @param type     The resource type to check.
     * @param quantity The required amount.
     * @return True if the bank has enough resources, false otherwise.
     */
    public boolean hasResources(ResourceType type, int quantity) {
        return resourceStock.getOrDefault(type, 0) >= quantity;
    }

    /**
     * Transfers resources from the Bank to a Player.
     * The Bank's internal stock decreases, and the Player gains the corresponding ResourceCards.
     *
     * @param player   The player receiving the resources.
     * @param type     The type of resource being transferred.
     * @param quantity The amount of resources being transferred.
     */
    public void transferResourcesTo(Player player, ResourceType type, int quantity) {
        if (!hasResources(type, quantity)) {
            return;
        }

        resourceStock.merge(type, -quantity, Integer::sum);

        for (int i = 0; i < quantity; i++) {
            player.addResource(new ResourceCard(type));
        }
    }

    /**
     * Receives resources from a Player back into the Bank.
     * The Bank's internal stock increases, and the Player loses the corresponding cards.
     *
     * @param player   The player returning the resources.
     * @param type     The type of resource being returned.
     * @param quantity The amount of resources being returned.
     */
    public void receiveResourcesFrom(Player player, ResourceType type, int quantity) {
        resourceStock.merge(type, quantity, Integer::sum);
        player.removeResource(type, quantity);
    }


    // ── 4. Getters ────────────────────────────────────────────────

    /**
     * Retrieves the current state of the resource stock.
     *
     * @return A map containing the available quantities for each resource type.
     */
    public Map<ResourceType, Integer> getResourceStock() {
        return resourceStock;
    }

}
