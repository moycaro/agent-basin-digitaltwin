package org.carol.tfm.domain.capabilities.basic_water_manager;

import bdi4jade.annotation.Belief;
import bdi4jade.annotation.Plan;
import bdi4jade.belief.BeliefSet;
import bdi4jade.belief.TransientBeliefSet;
import bdi4jade.core.Capability;
import bdi4jade.extension.planselection.utilitybased.SoftgoalPreferences;

import bdi4jade.goal.Goal;
import bdi4jade.plan.DefaultPlan;
import org.carol.tfm.Main;
import org.carol.tfm.agents.reservoir.goals.ReleaseWaterGoal;
import org.carol.tfm.agents.reservoir.goals.StorageWaterGoal;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinInflow;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinOutflow;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinStatus;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;
import org.carol.tfm.domain.capabilities.basic_water_manager.plans.InputOutputPlanBody;
import org.carol.tfm.domain.capabilities.basic_water_manager.plans.StorageWaterPlanBody;
import org.carol.tfm.domain.entities.Damn;

public class BasicManageDamCapability extends Capability {
    @Belief
    private BeliefSet<String, BasinStatus> basinStatus = new TransientBeliefSet<>(BeliefNames.BASIN_STATUS);
    @Belief
    private BeliefSet<String, BasinInflow> basinInflow = new TransientBeliefSet<>(BeliefNames.BASIN_INFLOW);
    @Belief
    private BeliefSet<String, BasinOutflow> basinOutflow = new TransientBeliefSet<>(BeliefNames.BASIN_OUTFLOW);
    @Belief
    private BeliefSet<String, Damn> damnBeliefSet = new TransientBeliefSet<>(BeliefNames.DAMN_STATUS);
    @Belief
    private Integer time_step;

    @Plan
    private bdi4jade.plan.Plan inputOutputPlan;

    @Plan
    private bdi4jade.plan.Plan storageWaterPlan;

    public BasicManageDamCapability() {
        this.inputOutputPlan = new DefaultPlan(ReleaseWaterGoal.class,
                InputOutputPlanBody.class) {
            @Override
            public boolean isContextApplicable(Goal goal) {
                return true;
            }
        };
        this.storageWaterPlan = new DefaultPlan(StorageWaterGoal.class, StorageWaterPlanBody.class);
    }

    @Override
    protected void setup() {
        time_step = 0;

        //HOTstart = false
        Main.BASINS.forEach( basin -> {
            basinStatus.addValue( new BasinStatus(basin, BasinStatus.BASIN_STATUS_TYPES.SCARCITY));
            basinInflow.addValue(  new BasinInflow(basin , 0));
            basinOutflow.addValue(  new BasinOutflow(basin , 0));

            damnBeliefSet.addValue( new Damn(basin, 200, 0));
        });
    }
}
