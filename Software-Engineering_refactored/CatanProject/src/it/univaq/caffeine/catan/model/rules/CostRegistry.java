package it.univaq.caffeine.catan.model.rules;

import it.univaq.caffeine.catan.model.enums.BuildingType;
import it.univaq.caffeine.catan.model.enums.ResourceType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * GRASP Information Expert for building costs.
 * Knows what each structure costs in resources.
 *
 * Values are injected as a raw Map by Game, which is the sole reader
 * of GameConfig in the model layer. This class converts String keys
 * (from JSON) to proper enum keys for type-safe access.
 * CostRegistry has no dependency on GameConfig or ConfigLoader.
 */
public class CostRegistry {

    private final Map<BuildingType, Map<ResourceType, Integer>> costs;


    // ── Constructor ───────────────────────────────────────────────

    /**
     * Receives the raw building-costs map extracted from GameConfig by Game.
     * Outer key: BuildingType name (e.g. "ROAD", "CITY").
     * Inner key: ResourceType name → required quantity.
     *
     * @param buildingCosts String-keyed cost map (from game_config.json).
     */
    public CostRegistry(Map<String, Map<String, Integer>> buildingCosts) {
        Map<BuildingType, Map<ResourceType, Integer>> temp = new EnumMap<>(BuildingType.class);

        buildingCosts.forEach((buildingKey, resourceMap) -> {
            BuildingType bt = BuildingType.valueOf(buildingKey);
            Map<ResourceType, Integer> rm = new EnumMap<>(ResourceType.class);
            resourceMap.forEach((resKey, qty) ->
                    rm.put(ResourceType.valueOf(resKey), qty));
            temp.put(bt, Collections.unmodifiableMap(rm));
        });

        this.costs = Collections.unmodifiableMap(temp);
    }


    // ── Query ─────────────────────────────────────────────────────

    /**
     * Returns the resource cost for the given building type.
     * Returns an empty map if the type is not configured.
     */
    public Map<ResourceType, Integer> getCost(BuildingType type) {
        return costs.getOrDefault(type, Collections.emptyMap());
    }
}
