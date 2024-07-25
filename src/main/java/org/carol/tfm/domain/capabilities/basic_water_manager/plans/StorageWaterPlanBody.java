package org.carol.tfm.domain.capabilities.basic_water_manager.plans;

import bdi4jade.annotation.Belief;
import bdi4jade.annotation.Parameter;
import bdi4jade.plan.Plan;
import bdi4jade.plan.planbody.AbstractPlanBody;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinStatus;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;
import org.carol.tfm.domain.entities.Damn;
import org.carol.tfm.domain.services.DataExportService;

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
    private String basin_id;
    private Integer time_step;

    @Override
    public void action() {
        //INPUT paso anterior y estado actual

        //sacar volumen a embalsar de mi inflow
        final float volumeToStore =  this.currentInflow * 1e-6f; //m³/h * 1h = m³ * 1e-6 hm³ / m³ = hm³

        //si mi presa ya está llena o no puede retener todo el agua
        if ( this.damn.getCurrent_volume() + volumeToStore >= this.damn.getMax_capacity() ) {
            float volumeAvaliable = this.damn.getMax_capacity() -  this.damn.getCurrent_volume();
            float volumeToRelease = ( volumeAvaliable > volumeToStore ) ? 0 : (volumeToStore - volumeAvaliable );
            DataExportService.appendLineToValues(time_step + ";" + this.basin_id + ";STORAGE;" + volumeToStore + ";" + this.damn.getMax_capacity() + ";" + this.damn.getCurrent_volume());

            log.info("\t\t[StoragePlan] FAILED: damn max vol:" + this.damn.getMax_capacity() + "; current vol to be storage" + volumeToStore);

            outflow = this.currentInflow;

            this.getBeliefBase().updateBelief( BeliefNames.BASIN_STATUS, BasinStatus.BASIN_STATUS_TYPES.FLOOD);
            setEndState(Plan.EndState.FAILED);
        } else {
            float volumeAvaliable = this.damn.getMax_capacity() -  this.damn.getCurrent_volume();
            float volumeToRelease = ( volumeAvaliable > volumeToStore ) ? 0 : (volumeToStore - volumeAvaliable );
            outflow = volumeToRelease * 1000000; // hm² * m³ /hm³ = m³ (en 1H)
            DataExportService.appendLineToValues(time_step + ";" + this.basin_id + ";STORAGE;" + volumeToStore + ";" + this.damn.getMax_capacity() + ";" + this.damn.getCurrent_volume());

            //OUTPUT outflow => MAX de lo que la presa es capaz de almacenar
            this.getBeliefBase().updateBelief( BeliefNames.BASIN_OUTFLOW, outflow );

            //OUTPUT estado => si mi salida es > 0 es que he llenado  mi presa
            if ( outflow > 0) {
                this.getBeliefBase().updateBelief( BeliefNames.BASIN_STATUS, BasinStatus.BASIN_STATUS_TYPES.FLOOD);
            }

            //OUTPUT presa => agua almacenada
            float currentVolume = this.damn.getCurrent_volume() + volumeToStore - volumeToRelease;
            this.damn.setCurrent_volume( currentVolume );
            this.getBeliefBase().updateBelief( BeliefNames.DAMN_STATUS, this.damn );

            log.info("\t\t[StoragePlan] [" + this.damn.getBasin_id() + "] Damn max vol: " + this.damn.getMax_capacity() + "; current vol " + currentVolume);

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

    @Parameter(direction = Parameter.Direction.IN, mandatory = true)
    public void setBasin_id(String basin_id) {
        this.basin_id = basin_id;
    }

    @Parameter(direction = Parameter.Direction.IN, mandatory = true)
    public void setTime_step(int time_step) {
        this.time_step = time_step;
    }

}
