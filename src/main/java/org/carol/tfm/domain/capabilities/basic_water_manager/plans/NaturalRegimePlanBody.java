package org.carol.tfm.domain.capabilities.basic_water_manager.plans;

import bdi4jade.annotation.Parameter;
import bdi4jade.plan.Plan;
import bdi4jade.plan.planbody.AbstractPlanBody;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.agents.reservoir.goals.ReleaseWaterGoal;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;
import org.carol.tfm.domain.entities.Damn;
import org.carol.tfm.domain.ontology.BasinDefinition;
import org.carol.tfm.domain.ontology.configs.FlowGaugeConfig;
import org.carol.tfm.domain.services.DataExportService;

public class NaturalRegimePlanBody extends AbstractPlanBody {
    private final Log log = LogFactory.getLog(this.getClass());
    private String basin_id;
    private Integer time_step;
    private float currentInflow;

    @Override
    public void action() {
        log.info("\t\t[Natural RegimePlan] [" + this.basin_id + "]" );
        //OUTPUT estado siguiente => Las entradas pasan a ser salidas
        boolean isSafe = askFLowGauges( currentInflow, this.basin_id );
        final float volumeToStore =  this.currentInflow * 1e-6f; //m³/h * 1h = m³ * 1e-6 hm³ / m³ = hm³

        if (  isSafe ) {
            DataExportService.appendLineToValues(time_step + ";" + this.basin_id + ";NATURAL;" + volumeToStore + ";-;-");

            this.getBeliefBase().updateBelief( BeliefNames.BASIN_OUTFLOW, currentInflow);
            setEndState(Plan.EndState.SUCCESSFUL);
        } else {
            DataExportService.appendLineToValues(time_step + ";" + this.basin_id + ";NATURAL;" + volumeToStore + ";-;-");

            //Voy a producir una inundación tengo que mirar si me puedo quedar yo con algo de agua
            setEndState(Plan.EndState.FAILED);
        }
    }

    private boolean askFLowGauges(float outflow, String basinId) {
        //Miro si tengo un gauge asociado
        if ( !BasinDefinition.GAUGES.containsKey( basinId)) {
            return true;
        }

        FlowGaugeConfig flowGaugeConfig = BasinDefinition.GAUGES.get( basinId );
        log.info("\t\t[Natural RegimePlan] [" + basinId + "] Asking status to Gauge  " + flowGaugeConfig.sensor_id);

        // Enviar mensaje al otro agente
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID( flowGaugeConfig.sensor_id , AID.ISLOCALNAME));
        msg.setContent( String.valueOf(outflow));
        myAgent.send(msg);

        // Esperar la respuesta
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
        ACLMessage reply = myAgent.blockingReceive(mt);

        if (reply != null) {
            return Boolean.parseBoolean( reply.getContent() );
        }

        return false;
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

}
