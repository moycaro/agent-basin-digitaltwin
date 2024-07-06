package org.carol.tfm.agents.gauge;

import bdi4jade.belief.BeliefSet;
import bdi4jade.event.BeliefEvent;
import bdi4jade.event.BeliefListener;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.agents.StoreDataAgent;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinInflow;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;

public class Pluvio extends Agent implements BeliefListener {
    private final Log log = LogFactory.getLog(this.getClass());

    private AID storageAgentAID = null;
    private final String basin_id;
    public Pluvio(String basin_id) {
        super();
        this.basin_id = basin_id;
    }

    // Put agent initializations here
    protected void setup() {
        log.trace(" [PLUVIO] "+ getAID().getName() +" is ready.");

        //Me guardo el/lod IDs del storage
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sdSearch = new ServiceDescription();
        sdSearch.setType("store-agent");
        template.addServices(sdSearch);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            this.storageAgentAID = result[0].getName();
        }
        catch (FIPAException fe) {
            log.error("Can not find Synchronizator Agent.");
            fe.printStackTrace();
        }
        // Add a TickerBehaviour that schedules a request to seller agents every minute
/*        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick() {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("store-agent");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    plannerAgents = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        plannerAgents[i] = result[i].getName();
                        log.trace("[PLUVIO] Found StoreData Agent " + plannerAgents[i].getName());
                    }
                }
                catch (FIPAException fe) {
                    log.error("Can not find StoreData Agent.");
                    fe.printStackTrace();
                }

                // Perform the request
                myAgent.addBehaviour(new SendDataBehaviour());
            }
        } );*/
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Printout a dismissal message
        log.info(" [PLUVIO] "+ getAID().getName()+" terminating.");
    }

    @Override
    public void eventOccurred(BeliefEvent beliefEvent) {
        if ( beliefEvent.getAction().equals( BeliefEvent.Action.BELIEF_UPDATED )) {
            if ( beliefEvent.getBelief().getName().equals(BeliefNames.TIME_STEP ) ) {
                sendDataBehaviour( (Integer) beliefEvent.getBelief().getValue() );
            }
        }
    }


    /**
     Inner class SendDataBehaviour.
     This is the behaviour used by Gauge agents to send data to a planner
     */
    private void sendDataBehaviour(int step) {
        ACLMessage info = new ACLMessage(ACLMessage.INFORM);
        info.addReceiver(this.storageAgentAID);
        Random rand = new Random();
        float currentValue = rand.nextFloat(0, 120);

        log.info( "\t\t [PLUVIO] " + basin_id + " (v: " + currentValue + ") ");
        info.setContent( basin_id + "@@" +  currentValue);
        info.setConversationId("sensor-data");

        this.send(info);
    }

    /**
     Inner class SendDataBehaviour.
     This is the behaviour used by Gauge agents to send data to a planner
     */
   /* private class SendDataBehaviour extends Behaviour {
        public void action() {
            // Send the cfp to all sellers
            ACLMessage info = new ACLMessage(ACLMessage.INFORM);
            Arrays.stream(plannerAgents).forEach(info::addReceiver);
            Random rand = new Random();
            info.setContent( basin_id + "@@" + rand.nextFloat(0, 120) );
            info.setConversationId("sensor-data");
            myAgent.send( info );
        }

        public boolean done() {
            log.trace("SendDataBehaviour END");
            return true;
        }
    }*/
}
