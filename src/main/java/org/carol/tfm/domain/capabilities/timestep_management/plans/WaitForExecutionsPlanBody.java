package org.carol.tfm.domain.capabilities.timestep_management.plans;

import bdi4jade.goal.Goal;
import bdi4jade.plan.Plan;
import bdi4jade.plan.planbody.AbstractPlanBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.agents.reservoir.goals.GoalDeserializer;
import org.carol.tfm.domain.ontology.BasinDefinition;

public class WaitForExecutionsPlanBody extends AbstractPlanBody {
    private static final Log log = LogFactory.getLog(WaitForExecutionsPlanBody.class);
    private int goals_achieved_in_current_timestep;

    @Override
    public void action() {
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
        ACLMessage msg = myAgent.receive(mt);
        if (msg != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                SimpleModule module = new SimpleModule("CustomGoalDeserializer", new Version(1, 0, 0, null, null, null));
                module.addDeserializer(Goal.class, new GoalDeserializer());
                objectMapper.registerModule(module);

                Goal achievedGoal = objectMapper.readValue(msg.getContent(), Goal.class);

                log.trace("[STEP] Basin goal achieved  " );
                goals_achieved_in_current_timestep++;
                if (goals_achieved_in_current_timestep == BasinDefinition.BASINS.size()) {
                    goals_achieved_in_current_timestep = 0;
                    this.setEndState( Plan.EndState.SUCCESSFUL );
                }
            } catch (JsonProcessingException e) {
                log.error("Can not read achieved goal msg.", e);
            }
        } else {
            block();
        }
    }
    @Override
    public void onStart() {
        this.goals_achieved_in_current_timestep = 0;
    }


}
