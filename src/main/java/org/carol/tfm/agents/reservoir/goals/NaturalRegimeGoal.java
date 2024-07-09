package org.carol.tfm.agents.reservoir.goals;

import bdi4jade.goal.Goal;
import org.carol.tfm.domain.entities.Damn;

import java.io.Serializable;

public class NaturalRegimeGoal implements Goal, Serializable  {
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
}
