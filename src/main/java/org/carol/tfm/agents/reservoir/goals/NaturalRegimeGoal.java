package org.carol.tfm.agents.reservoir.goals;

import bdi4jade.annotation.Parameter;
import bdi4jade.goal.Goal;
import org.carol.tfm.domain.entities.Damn;

import java.io.Serializable;

public class NaturalRegimeGoal implements Goal, Serializable, IBasinManagementGoal  {
    private final String basin_id;
    private final float currentInflow;
    private float outflow;
    private final int time_step;
    private final String name = NaturalRegimeGoal.class.getName();

    public NaturalRegimeGoal(String basin_id, float currentInflow, int timeStep) {
        this.currentInflow = currentInflow;
        time_step = timeStep;
        this.basin_id = basin_id;
    }

    @Parameter(direction = Parameter.Direction.IN)
    @Override
    public String getBasin_id() {
        return basin_id;
    }
    @Parameter(direction = Parameter.Direction.IN)
    public float getCurrentInflow() {
        return currentInflow;
    }

    public float getOutflow() {
        return outflow;
    }

    @Parameter(direction = Parameter.Direction.IN)
    public int getTime_step() {
        return time_step;
    }

    public String getName() {
        return name;
    }


}
