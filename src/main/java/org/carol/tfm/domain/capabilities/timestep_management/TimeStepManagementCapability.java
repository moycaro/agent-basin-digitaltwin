package org.carol.tfm.domain.capabilities.timestep_management;

import bdi4jade.annotation.Belief;
import bdi4jade.core.Capability;
import bdi4jade.plan.DefaultPlan;
import org.carol.tfm.domain.capabilities.timestep_management.goals.NextStepGoal;
import org.carol.tfm.domain.capabilities.timestep_management.goals.WaitForExecutionsGoal;
import org.carol.tfm.domain.capabilities.timestep_management.plans.NextStepPlanBody;
import org.carol.tfm.domain.capabilities.timestep_management.plans.WaitForExecutionsPlanBody;

public class TimeStepManagementCapability extends Capability {
    @Belief
    private Integer time_step;


    private static final long serialVersionUID = 2712019445290687786L;

    @bdi4jade.annotation.Plan
    private bdi4jade.plan.Plan nextStepPlanBody;

    @bdi4jade.annotation.Plan
    private bdi4jade.plan.Plan waitForAgentsPlanBody;

    public TimeStepManagementCapability() {
        this.nextStepPlanBody = new DefaultPlan(NextStepGoal.class, NextStepPlanBody.class);
        this.waitForAgentsPlanBody = new DefaultPlan(WaitForExecutionsGoal.class, WaitForExecutionsPlanBody.class);
    }

}

