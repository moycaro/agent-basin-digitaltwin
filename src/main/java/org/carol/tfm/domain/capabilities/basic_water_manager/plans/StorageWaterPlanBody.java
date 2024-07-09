package org.carol.tfm.domain.capabilities.basic_water_manager.plans;

import bdi4jade.annotation.Belief;
import bdi4jade.annotation.Parameter;
import bdi4jade.plan.Plan;
import bdi4jade.plan.planbody.AbstractPlanBody;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinOutflow;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinStatus;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;
import org.carol.tfm.domain.entities.Damn;

import java.io.Serializable;
import java.util.Set;

public class StorageWaterPlanBody extends AbstractPlanBody  {
    private final Log log = LogFactory.getLog(this.getClass());

    @Belief(name = BeliefNames.BASIN_STATUS)
    private BasinStatus.BASIN_STATUS_TYPES basin_status;
    @Belief(name = BeliefNames.BASIN_INFLOW)
    private Float basinInflow;
    @Belief(name = BeliefNames.BASIN_OUTFLOW)
    private Float basinOutflow;
    @Belief(name = BeliefNames.DAMN_STATUS)
    private Damn basinDamn;

    private Damn damn;
    private float currentInflow;
    private float outflow;

    @Override
    public void action() {
        //INPUT paso anterior y estado actual

        //si mi presa ya está llena o no puede retener todo el agua
        if ( this.damn.getCurrent_volume() + this.currentInflow >= this.damn.getMax_capacity() ) {
            log.info("I/O PLan failed because with new incoming water " + this.currentInflow + " current damn is FULL : " + this.damn.toString());
            outflow = this.currentInflow;

            this.getBeliefBase().updateBelief( BeliefNames.BASIN_STATUS, BasinStatus.BASIN_STATUS_TYPES.FLOOD);
            setEndState(Plan.EndState.FAILED);
        } else {
            float volumeAvaliable = this.damn.getMax_capacity() -  this.damn.getCurrent_volume();
            outflow = ( volumeAvaliable > currentInflow ) ? 0 : (currentInflow - volumeAvaliable );

            //OUTPUT outflow => MAX de lo que la presa es capaz de almacenar
            this.getBeliefBase().updateBelief( BeliefNames.BASIN_OUTFLOW, outflow );

            //OUTPUT estado => si mi salida es > 0 es que he llenado  mi presa
            if ( outflow > 0) {
                this.getBeliefBase().updateBelief( BeliefNames.BASIN_STATUS, BasinStatus.BASIN_STATUS_TYPES.FLOOD);
            }

            //OUTPUT presa => agua almacenada
            float currentVolume = this.damn.getCurrent_volume() + this.currentInflow - outflow;
            this.damn.setCurrent_volume( currentVolume );
            this.getBeliefBase().updateBelief( BeliefNames.DAMN_STATUS, this.damn );

            setEndState(Plan.EndState.SUCCESSFUL);
        }
    }

    @Parameter(direction = Parameter.Direction.IN, mandatory = true)
    public void setDamn(Damn damn) {
        this.damn = damn;
    }

    @Parameter(direction = Parameter.Direction.IN, mandatory = true)
    public void setCurrentInflow(float currentInflow) {
        this.currentInflow = currentInflow;
    }


}
