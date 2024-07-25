package org.carol.tfm.domain.capabilities.basic_water_manager;

import bdi4jade.annotation.Belief;
import bdi4jade.annotation.Plan;
import bdi4jade.core.Capability;

import bdi4jade.goal.Goal;
import bdi4jade.plan.DefaultPlan;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.agents.reservoir.goals.NaturalRegimeGoal;
import org.carol.tfm.agents.reservoir.goals.ReleaseWaterGoal;
import org.carol.tfm.agents.reservoir.goals.StorageWaterGoal;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinStatus;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;
import org.carol.tfm.domain.capabilities.basic_water_manager.plans.ReleaseWaterPlanBody;
import org.carol.tfm.domain.capabilities.basic_water_manager.plans.NaturalRegimePlanBody;
import org.carol.tfm.domain.capabilities.basic_water_manager.plans.StorageWaterPlanBody;
import org.carol.tfm.domain.entities.Damn;
import org.carol.tfm.domain.ontology.configs.ReservoirConfig;

import java.util.Optional;

public class BasicManageDamCapability extends Capability {
    private static final Log log = LogFactory.getLog(BasicManageDamCapability.class);
    @Belief
    private BasinStatus.BASIN_STATUS_TYPES basin_status;
    @Belief
    private Float basin_rainfall;
    @Belief
    private Float basin_outflow;
    @Belief
    private Damn damn_status;
    @Belief
    private Float basin_inflow;

    @Plan
    private bdi4jade.plan.Plan inputOutputPlan;

    @Plan
    private bdi4jade.plan.Plan storageWaterPlan;

    @Plan
    private bdi4jade.plan.Plan regimeNaturalPlan;

    Optional<ReservoirConfig> reservoirConfig;
    String basin_id;

    public BasicManageDamCapability(String basin_id, Optional<ReservoirConfig> config) {
        this.basin_id = basin_id;
        this.reservoirConfig = config;

        this.inputOutputPlan = new DefaultPlan(ReleaseWaterGoal.class,
                ReleaseWaterPlanBody.class) {
            @Override
            public boolean isContextApplicable(Goal goal) {
                return true;
            }
        };
        this.storageWaterPlan = new DefaultPlan(StorageWaterGoal.class, StorageWaterPlanBody.class);
        this.regimeNaturalPlan = new DefaultPlan(NaturalRegimeGoal.class, NaturalRegimePlanBody.class);
    }

    @Override
    protected void setup() {
        //HOTstart = false
        this.basin_status = BasinStatus.BASIN_STATUS_TYPES.SCARCITY;
        this.basin_rainfall = 0f;
        this.basin_inflow = 0f;
        this.basin_outflow = 0f;
        if ( this.reservoirConfig.isEmpty() ) {
            log.warn(" La cuenca " + this.basin_id + " no tiene elementos de gestión.");
            this.damn_status = null;
        } else {
            this.damn_status = new Damn( basin_id, this.reservoirConfig.get().volumen_total, 0 );
        }

        this.beliefBase.getBelief( BeliefNames.BASIN_INFLOW).putMetadata("basin_id", this.basin_id );
        this.beliefBase.getBelief( BeliefNames.BASIN_OUTFLOW).putMetadata("basin_id", this.basin_id );
        this.beliefBase.getBelief( BeliefNames.BASIN_RAINFALL).putMetadata("basin_id", this.basin_id );
        this.beliefBase.getBelief( BeliefNames.BASIN_STATUS).putMetadata("basin_id", this.basin_id );
    }
}
