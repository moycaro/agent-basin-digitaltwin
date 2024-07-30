package org.carol.tfm.domain.capabilities.basic_water_manager.plans;

import bdi4jade.annotation.Parameter;
import bdi4jade.plan.planbody.AbstractPlanBody;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;

public class CompleteOperationPlanBody extends AbstractPlanBody  {
    private final Log log = LogFactory.getLog(this.getClass());
    private String basin_id;
    private Integer time_step;
    private float currentInflow;
    private float volumeToStorage;

    @Override
    public void action() {
        log.info("\t\t[CompleteOperation Plan] [" + this.basin_id + "]" );
        final float inflowVolume =  this.currentInflow * 1e-6f; //m³/h * 1h = m³ * 1e-6 hm³ / m³ = hm³

        this.getBeliefBase().updateBelief( BeliefNames.BASIN_OUTFLOW, currentInflow);
    }


    @Parameter(direction = Parameter.Direction.IN, mandatory = true)
    public void setBasin_id(String basin_id) {
        this.basin_id = basin_id;
    }

    @Parameter(direction = Parameter.Direction.IN, mandatory = true)
    public void setTime_step(int time_step) {
        this.time_step = time_step;
    }

    @Parameter(direction = Parameter.Direction.IN, mandatory = true)
    public void setCurrentInflow(float currentInflow) {
        this.currentInflow = currentInflow;
    }

    @Parameter(direction = Parameter.Direction.IN, mandatory = true)
    public void setVolumeToStorage(float volumeToStorage) {
        this.volumeToStorage = volumeToStorage;
    }

}
