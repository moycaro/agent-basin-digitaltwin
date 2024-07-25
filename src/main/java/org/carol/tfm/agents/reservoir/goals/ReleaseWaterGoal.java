package org.carol.tfm.agents.reservoir.goals;

import bdi4jade.annotation.Parameter;
import bdi4jade.goal.Goal;
import org.carol.tfm.domain.entities.Damn;

import java.io.Serializable;

public class ReleaseWaterGoal implements Goal, Serializable,IBasinManagementGoal {

    private final float currentInflow;
    private float outflow;
    private final Damn damn;
    private final String basin_id;
    private final int time_step;
    private final String name = ReleaseWaterGoal.class.getName();

    public ReleaseWaterGoal(Damn damn, float currentInflow, int timeStep) {
        this.currentInflow = currentInflow;
        this.damn = damn;
        this.basin_id = this.damn.getBasin_id();
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
    public float getOutflow() {
        return outflow;
    }


    @Override
    public String toString() {
        return "Perform I/O for inflow : " + currentInflow + " in " + damn.toString();
    }

    @Parameter(direction = Parameter.Direction.IN)
    public int getTime_step() {
        return time_step;
    }

    public String getName() {
        return name;
    }

    public void setOutflow(float outflow) {
        this.outflow = outflow;
    }

    @Parameter(direction = Parameter.Direction.IN)
    @Override
    public String getBasin_id() {
        return basin_id;
    }
}

