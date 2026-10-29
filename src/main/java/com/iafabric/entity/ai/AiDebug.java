package com.iafabric.entity.ai;

import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.Mob;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Class to easily list the current tasks/goals of entities and identify potential issues
 */
public class AiDebug {
    private static final List<Mob> entities = new ArrayList<>();
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("iceandfire_fabric");

    private AiDebug() {
        //Hides default constructor.
    }

    public static boolean isEnabled() {
        return false;
    }

    public static void logData() {
        List<Mob> entitiesCopy = new ArrayList<>(entities);
        for (Mob entity : entitiesCopy) {
            if (!entity.isAlive()) {
                entities.remove(entity);
                continue;
            }
            if (entity.goalSelector != null) {
                List<String> goals = entity.goalSelector.getAvailableGoals().stream().filter(WrappedGoal::isRunning).map(goal -> goal.getGoal().toString()).collect(Collectors.toList());
                if (!goals.isEmpty())
                    LOGGER.debug("{} - GOALS: {}", entity, goals);
            }
            if (entity.targetSelector != null) {
                List<String> targets = entity.targetSelector.getAvailableGoals().stream().filter(WrappedGoal::isRunning).map(goal -> goal.getGoal().toString()).collect(Collectors.toList());
                if (!targets.isEmpty())
                    LOGGER.debug("{} - TARGET: {}", entity, targets);
            }

        }
    }

    public static boolean contains(Mob entity) {
        return entities.contains(entity);
    }

    public static void addEntity(Mob entity) {
        if (entities.contains(entity)) {
            entities.remove(entity);
        } else {
            entities.add(entity);
        }
    }


}
