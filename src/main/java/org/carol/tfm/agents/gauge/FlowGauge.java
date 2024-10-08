package org.carol.tfm.agents.gauge;

import bdi4jade.event.BeliefEvent;
import bdi4jade.event.BeliefListener;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinOutflow;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;
import org.carol.tfm.domain.ontology.configs.FlowGaugeConfig;

public class FlowGauge extends Agent implements BeliefListener  {
    private final Log log = LogFactory.getLog(this.getClass());

    private AID[] plannerAgents;
    private final String basin_id;
    private final FlowGaugeConfig config;
    public FlowGauge(String basin_id, FlowGaugeConfig config) {
        super();
        this.basin_id = basin_id;
        this.config = config;
    }

    // Put agent initializations here
    protected void setup() {
        log.trace(" [FLOW-GAUGE] "+ getAID().getName() +" is ready.");

        // Podría tener un TickerBehaviour con las mediciones reales del entorno
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    if (msg.getPerformative() == ACLMessage.REQUEST) {
                        log.info( this.myAgent.getName() + " Checking for save outflow " + msg.getContent() );
                        float outflow = Float.parseFloat(msg.getContent()); // de la cuenca en m³/h

                        // Enviar una respuesta
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.INFORM);
                        float threshold = config.flow_threshold_info * 3600; //m³/sg * sg/h = m³/h
                        reply.setContent( outflow > threshold ? "false" : "true" ); //ES SEGURO?
                        send(reply);
                    }
                } else {
                    block();
                }
            }
        });

    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Printout a dismissal message
        log.info(" [HYDRO_GAUGE] "+ getAID().getName()+" terminating.");
    }

    @Override
    public void eventOccurred(BeliefEvent beliefEvent) {
        //Me interesa recoger cuando cambia el OUTFLOW que viene por mi cuenca
        /*
        if ( beliefEvent.getAction().equals( BeliefEvent.Action.BELIEF_UPDATED )) {
            if ( beliefEvent.getBelief().getName().equals(BeliefNames.BASIN_OUTFLOW ) ) {

                int a = 0;
                a++;
            }
        }
         */
    }
}
