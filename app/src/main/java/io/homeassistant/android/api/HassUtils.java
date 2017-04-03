package io.homeassistant.android.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.ason.Ason;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.homeassistant.android.api.results.Entity;

import static io.homeassistant.android.api.Domain.AUTOMATION;
import static io.homeassistant.android.api.Domain.BINARY_SENSOR;
import static io.homeassistant.android.api.Domain.GROUP;
import static io.homeassistant.android.api.Domain.INPUT_BOOLEAN;
import static io.homeassistant.android.api.Domain.INPUT_SELECT;
import static io.homeassistant.android.api.Domain.LIGHT;
import static io.homeassistant.android.api.Domain.SCENE;
import static io.homeassistant.android.api.Domain.SENSOR;
import static io.homeassistant.android.api.Domain.SUN;
import static io.homeassistant.android.api.Domain.SWITCH;

public final class HassUtils {

    private HassUtils() {
    }

    /**
     * Extract entities from a RequestResult of a StateRequest
     *
     * @param result    the RequestResult to extract from
     * @param entityMap the Map where entities are store with their id as key
     * @return true if items were added, else false
     */
    public static boolean extractEntitiesFromStateResult(@Nullable Object result, @NonNull Map<String, Entity> entityMap) {
        if (result != null && result.getClass().isArray()) {
            // Clear map before adding new content
            entityMap.clear();
            for (Object o : (Object[]) result) {
                Entity entity = Ason.deserialize((Ason) o, Entity.class);
                entityMap.put(entity.id, entity);
            }
            return true;
        }
        return false;
    }

    public static void extractGroups(@NonNull Map<String, Entity> entityMap, List<Entity> entities) {
        entities.clear();
        List<Entity> groups = new ArrayList<>();
        for (String entityId : entityMap.keySet()) {
            if (extractDomainFromEntityId(entityId).equals(GROUP)) {
                Entity entity = entityMap.get(entityId);
                if (entity.attributes.hidden) {
                    continue;
                }
                // Add visible group item
                entity.type = EntityType.GROUP;
                groups.add(entity);
            }
        }

        // Sort groups according to their order number
        Collections.sort(groups);

        // Add group children
        for (Entity group : groups) {
            entities.add(group);
            // Search and add items from group
            String[] children = group.attributes.children;
            for (int i = 0; i < children.length; i++) {
                Entity child = entityMap.get(children[i]);
                if (child == null) continue;
                child.type = extractTypeFromEntity(child);
                if (child.attributes == null || !child.attributes.hidden) {
                    entities.add(child);
                }
            }

            // Add spacer
            Entity spacer = new Entity();
            spacer.type = EntityType.SPACER;
            entities.add(spacer);
        }
    }

    public static String extractDomainFromEntityId(String entityId) {
        return entityId.split("\\.")[0];
    }

    public static EntityType extractTypeFromEntity(Entity entity) {
        String domain = extractDomainFromEntityId(entity.id);
        switch (domain) {
            case AUTOMATION:
            case INPUT_BOOLEAN:
            case LIGHT:
            case SWITCH:
                return EntityType.SWITCH;
            case BINARY_SENSOR:
            case SENSOR:
            case SUN:
                return EntityType.SENSOR;
            case INPUT_SELECT:
                return EntityType.INPUT_SELECT;
            case SCENE:
                return EntityType.SCENE;
            default:
                if (entity.attributes != null && entity.attributes.friendly_name != null) {
                    return EntityType.TEXT;
                }
                return EntityType.BASE;
        }
    }

    @NonNull
    public static String extractEntityName(@NonNull Entity e) {
        return e.attributes != null && e.attributes.friendly_name != null ? e.attributes.friendly_name : e.id;
    }
}