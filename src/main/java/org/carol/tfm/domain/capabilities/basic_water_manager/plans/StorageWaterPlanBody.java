package org.carol.tfm.domain.capabilities.basic_water_manager.plans;

import bdi4jade.annotation.Belief;
import bdi4jade.annotation.Parameter;
import bdi4jade.belief.BeliefSet;
import bdi4jade.belief.TransientBeliefSet;
import bdi4jade.plan.Plan;
import bdi4jade.plan.planbody.AbstractPlanBody;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinInflow;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinOutflow;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinStatus;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;
import org.carol.tfm.domain.entities.Damn;

import java.io.Serializable;
import java.util.Set;

public class StorageWaterPlanBody extends AbstractPlanBody  {
    private final Log log = LogFactory.getLog(this.getClass());

    @Belief(name = BeliefNames.BASIN_STATUS)
    private BeliefSet<String, BasinStatus> basinStatus;
    @Belief(name = BeliefNames.BASIN_INFLOW)
    private BeliefSet<String, BasinInflow> basinInflow;
    @Belief(name = BeliefNames.BASIN_OUTFLOW)
    private BeliefSet<String, BasinOutflow> basinOutflow;
    @Belief(name = BeliefNames.DAMN_STATUS)
    private BeliefSet<String, Damn> basinDamn;

    private Damn damn;
    private float currentInflow;
    private float outflow;

    @Override
    public void action() {
        //INPUT paso anterior y estado actual
        BasinStatus myBasinStatus = basinStatus.getValue().stream().filter( status -> status.getBasin_id().equals( this.damn.getBasin_id())).findAny().get();

        //si mi presa ya está llena o no puede retener todo el agua
        if ( this.damn.getCurrent_volume() + this.currentInflow >= this.damn.getMax_capacity() ) {
            log.info("I/O PLan failed because with new incoming water " + this.currentInflow + " current damn is FULL : " + this.damn.toString());
            outflow = this.currentInflow;

            Set<BasinStatus> modifiedStatusBeliefeSet = basinStatus.getValue();
            modifiedStatusBeliefeSet.stream().filter( basinOutflow -> basinOutflow.getBasin_id().equals( this.damn.getBasin_id() ))
                    .findAny().get().setCurrent_status( BasinStatus.BASIN_STATUS_TYPES.FLOOD );
            basinStatus.setValue( modifiedStatusBeliefeSet );

            setEndState(Plan.EndState.FAILED);
        } else {
            float volumeAvaliable = this.damn.getMax_capacity() -  this.damn.getCurrent_volume();
            outflow = ( volumeAvaliable > currentInflow ) ? 0 : (currentInflow - volumeAvaliable );

            //OUTPUT outflow => MAX de lo que la presa es capaz de almacenar
            Set<BasinOutflow> modifiedOutflowBeliefeSet = basinOutflow.getValue();
            modifiedOutflowBeliefeSet.stream().filter( basinOutflow -> basinOutflow.getBasin_id().equals( this.damn.getBasin_id() ))
                    .findAny().get().setMm( outflow );
            basinOutflow.setValue( modifiedOutflowBeliefeSet );

            //OUTPUT estado => si mi salida es > 0 es que he llenado  mi presa
            if ( outflow > 0) {
                Set<BasinStatus> modifiedStatusBeliefeSet = basinStatus.getValue();
                modifiedStatusBeliefeSet.stream().filter( basinOutflow -> basinOutflow.getBasin_id().equals( this.damn.getBasin_id() ))
                        .findAny().get().setCurrent_status( BasinStatus.BASIN_STATUS_TYPES.FLOOD );
                basinStatus.setValue( modifiedStatusBeliefeSet );
            }

            //OUTPUT presa => agua almacenada
            float currentVolume = this.damn.getCurrent_volume() + this.currentInflow - outflow;
            Set<Damn> modifiedDamnBeliefeSet = basinDamn.getValue();
            modifiedDamnBeliefeSet.stream().filter( basinOutflow -> basinOutflow.getBasin_id().equals( this.damn.getBasin_id() ))
                    .findAny().get().setCurrent_volume( currentVolume );
            basinDamn.setValue( modifiedDamnBeliefeSet );

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
