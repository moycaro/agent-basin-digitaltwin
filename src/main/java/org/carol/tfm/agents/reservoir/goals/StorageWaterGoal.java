package org.carol.tfm.agents.reservoir.goals;

import bdi4jade.annotation.Parameter;
import bdi4jade.goal.Goal;
import org.carol.tfm.domain.entities.Damn;

import java.io.Serializable;

public class StorageWaterGoal implements Goal, Serializable, IBasinManagementGoal {
    private float currentInflow;
    private float outlow;
    private int time_step;
    private Damn damn;
    private final String name = StorageWaterGoal.class.getName();

    public StorageWaterGoal(Damn damn, float currentInflow, int timeStep) {
        this.currentInflow = currentInflow;
        this.damn = damn;
        time_step = timeStep;
    }

    @Parameter(direction = Parameter.Direction.IN)
    public Damn getDamn() {
        return damn;
    }

    @Parameter(direction = Parameter.Direction.IN)
    public float getCurrentInflow() {
        return currentInflow;
    }

    @Parameter(direction = Parameter.Direction.OUT)
    public float getOutlow() {
        return outlow;
    }


    @Override
    public String toString() {
        return "Perform STORAGE for inflow : " + currentInflow + " in " + damn.toString();
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
        return this.damn.getBasin_id();
    }
}
