package org.carol.tfm.agents.planner;

import bdi4jade.belief.BeliefBase;
import bdi4jade.belief.BeliefSet;
import bdi4jade.event.BeliefEvent;
import bdi4jade.event.BeliefListener;
import bdi4jade.event.GoalEvent;
import bdi4jade.event.GoalListener;
import bdi4jade.goal.Goal;
import bdi4jade.goal.GoalStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.Main;
import org.carol.tfm.agents.StoreDataAgent;
import org.carol.tfm.agents.reservoir.goals.GoalDeserializer;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinInflow;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;

public class SynchronizatorAgent extends Agent {
    private final Log log = LogFactory.getLog(this.getClass());

    private int goals_achieved_in_current_timestep = 0;
    private final BeliefBase beliefBase;

    public SynchronizatorAgent(BeliefBase beliefBase) {
        this.beliefBase = beliefBase;
    }

    // Put agent initializations here
    protected void setup() {
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("synchronizator");
        sd.setName("synchronizator");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            log.error( "[SYNCHRONIZATOR] Can not register.", fe);
        }

        // Add the behaviour receive sensor data
        addBehaviour(new SynchronizatorAgent.ManageGoalPerformed());
    }

    /**
     Inner class ManageGoalPerformed.
     This is the behaviour used by Synchronizator to let the TimeStep be synchronized
     */
    private class ManageGoalPerformed extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    SimpleModule module = new SimpleModule("CustomCarDeserializer", new Version(1, 0, 0, null, null, null));
                    module.addDeserializer(Goal.class, new GoalDeserializer());
                    objectMapper.registerModule(module);

                    Goal achievedGoal = objectMapper.readValue(msg.getContent(), Goal.class);

                    Integer time_step = (Integer) beliefBase.getBelief("time_step").getValue();

                    log.trace("[STEP " + time_step + "] Basin goal achieved  " );
                    goals_achieved_in_current_timestep++;
                    if (goals_achieved_in_current_timestep == Main.BASINS.size()) {
                        goals_achieved_in_current_timestep = 0;
                        time_step++;

                        log.info("*************************************************************************************");
                        log.info("********** STEP: " + time_step);

                          beliefBase.updateBelief("time_step",  time_step );
                    }
                } catch (JsonProcessingException e) {
                    log.error("Can not read achieved goal msg.", e);
                }
            } else {
                block();
            }
        }
    }

/*
    public void goalPerformed(GoalEvent event) {
        if ( event.getStatus().equals(GoalStatus.ACHIEVED ) ) {
            //He conseguido la meta deseada
            this.log.info("**** GOAL ACHIEVED: " + event.toString());
            goals_achieved_in_current_timestep++;
            if (goals_achieved_in_current_timestep == Main.BASINS.size()) {
                this.log.info("TODAS, cambio de STEP");
                Integer time_step = (Integer) beliefBase.getBelief("time_step").getValue();
                beliefBase.updateBelief("time_step",  time_step++ );
            }
        }
    }
*/
}
