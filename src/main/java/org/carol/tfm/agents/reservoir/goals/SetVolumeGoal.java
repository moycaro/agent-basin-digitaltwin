package org.carol.tfm.agents.reservoir.goals;

import bdi4jade.annotation.Parameter;
import bdi4jade.goal.Goal;
import org.carol.tfm.domain.entities.Damn;
import org.carol.tfm.external_planners.domain.entities.DamnManagement;

import java.io.Serializable;

public class SetVolumeGoal implements Goal, Serializable,IBasinManagementGoal {

    private final DamnManagement damnManagement;
    private final int time_step;
    private final String name = SetVolumeGoal.class.getName();

    public SetVolumeGoal(DamnManagement damnManagement, int timeStep) {
        this.damnManagement = damnManagement;
        time_step = timeStep;
    }

    @Parameter(direction = Parameter.Direction.IN)
    public DamnManagement getDamnManagement() {
        return damnManagement;
    }

    @Override
    public String toString() {
        return "Perform set operation with volume to storage in " + damnManagement.getBasin_id();
    }

    @Parameter(direction = Parameter.Direction.IN)
    public int getTime_step() {
        return time_step;
    }

    public String getName() {
        return name;
    }

    @Parameter(direction = Parameter.Direction.IN)
    @Override
    public String getBasin_id() {
        return damnManagement.getBasin_id();
    }
}