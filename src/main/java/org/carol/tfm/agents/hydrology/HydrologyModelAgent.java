package org.carol.tfm.agents.hydrology;

import bdi4jade.belief.BeliefBase;
import bdi4jade.event.BeliefEvent;
import bdi4jade.event.BeliefListener;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;
import org.carol.tfm.domain.ontology.BasinDefinition;

import java.util.Map;

public class HydrologyModelAgent extends Agent implements BeliefListener {
    private final Log log = LogFactory.getLog(this.getClass());
    private final Map<String, BeliefBase> beliefBase;

    private final float RUNOFF_COEF = 0.2f;
    private final int TIMESTEP_MINUTES = 60;

    public HydrologyModelAgent(Map<String, BeliefBase> beliefBase) {
        super();
        this.beliefBase = beliefBase;
    }

    // Put agent initializations here
    protected void setup() {
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("hydrology-model-agent");
        sd.setName("Hydrology model Agent");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            log.error( "[StoreData] Can not register.", fe);
        }
    }

    @Override
    public void eventOccurred(BeliefEvent beliefEvent) {
        if ( beliefEvent.getAction().equals( BeliefEvent.Action.BELIEF_UPDATED ) &&
             beliefEvent.getBelief().getName().equals(BeliefNames.BASIN_RAINFALL) ) {
            //Nuevo dato de lluvia detectado => calcular caudal
            final Float rainfall = (Float) beliefEvent.getBelief().getValue();
            final String basin_id = (String) beliefEvent.getBelief().getMetadata("basin_id");
            final float basin_area = BasinDefinition.BASINS_CONFIG.get( basin_id ).area;
            final float runoff = getFlowRunoff( rainfall, basin_area);

            this.beliefBase.get( basin_id ).updateBelief( BeliefNames.BASIN_INFLOW, runoff);
        }
    }

    private float getFlowRunoff(Float rainfall, float basinArea) {
        if ( Float.isNaN( rainfall.floatValue() ) ) {
            return 0f;
        }

        float v = basinArea * 1000000 * rainfall * 0.001f; //(km² * mm²/km² * mm * m/mm); = m³

        float v_runoff = v * RUNOFF_COEF;
        float flow = v_runoff / ( TIMESTEP_MINUTES * 1/60); //m³ * (m * h/m) = m³/h

        return flow;
    }
}
