package org.carol.tfm.agents;

import bdi4jade.belief.BeliefBase;
import bdi4jade.belief.BeliefSet;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinInflow;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;

import java.util.Map;
import java.util.Set;

public class StoreDataAgent extends Agent {
    private final Log log = LogFactory.getLog(this.getClass());
    private final Map<String, BeliefBase> beliefBase;

    public StoreDataAgent(Map<String, BeliefBase> beliefBase) {
        super();
        this.beliefBase = beliefBase;
    }

    // Put agent initializations here
    protected void setup() {
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("store-agent");
        sd.setName("Basic Store Data Agent");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            log.error( "[StoreData] Can not register.", fe);
        }

        // Add the behaviour receive sensor data
        addBehaviour(new StoreDataAgent.ManageIncomingDataService());
    }

    /**
     Inner class ManageIncomingDataService.
     This is the behaviour used by StoreData to manager incoming data
     */
    private class ManageIncomingDataService extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                final String[] msgTokens = msg.getContent().split("@@");
                final String basinId = msgTokens[0];
                final Float rainfall = Float.parseFloat( msgTokens[1]);
                log.trace("[StoreData] New value received from RainGauge in basin " + basinId + " " + rainfall + "mm.");
                beliefBase.get(basinId).updateBelief(BeliefNames.BASIN_INFLOW, rainfall );
                log.info("[StoreData] BasinInflow for " + basinId + " belief updated.");
            } else {
                block();
            }
        }
    }
}
