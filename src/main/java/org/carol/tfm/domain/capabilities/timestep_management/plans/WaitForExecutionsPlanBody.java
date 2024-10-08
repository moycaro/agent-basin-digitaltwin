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
import org.carol.tfm.agents.reservoir.goals.NaturalRegimeGoal;
import org.carol.tfm.agents.reservoir.goals.ReleaseWaterGoal;
import org.carol.tfm.agents.reservoir.goals.StorageWaterGoal;
import org.carol.tfm.domain.ontology.BasinDefinition;

public class WaitForExecutionsPlanBody extends AbstractPlanBody {
    private static final Log log = LogFactory.getLog(WaitForExecutionsPlanBody.class);
    private int goals_achieved_in_current_timestep;

    @Override
    public void action() {
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
        ACLMessage msg = myAgent.receive(mt);
        if (msg != null) {
            final String convertationId = msg.getConversationId();
            if ( convertationId.equals("planner-goal-performed")) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    SimpleModule module = new SimpleModule("CustomGoalDeserializer", new Version(1, 0, 0, null, null, null));
                    module.addDeserializer(Goal.class, new GoalDeserializer());
                    objectMapper.registerModule(module);

                    Goal achievedGoal = objectMapper.readValue(msg.getContent(), Goal.class);
                    String basin_id = "-";
                    if ( achievedGoal instanceof ReleaseWaterGoal ) {
                        basin_id = ((ReleaseWaterGoal) achievedGoal).getBasin_id();
                    } else if ( achievedGoal instanceof NaturalRegimeGoal) {
                        basin_id = ((NaturalRegimeGoal) achievedGoal).getBasin_id();
                    } else if ( achievedGoal instanceof StorageWaterGoal) {
                        basin_id = ((StorageWaterGoal) achievedGoal).getDamn().getBasin_id();
                    }


                    log.info("\t [STEP] Basin goal achieved " + basin_id);
                    goals_achieved_in_current_timestep++;
                    if (goals_achieved_in_current_timestep == BasinDefinition.BASINS.size()) {
                        goals_achieved_in_current_timestep = 0;
                        this.setEndState( Plan.EndState.SUCCESSFUL );
                    }
                } catch (JsonProcessingException e) {
                    log.error("Can not read achieved goal msg.", e);
                }
            } else if ( convertationId.equals("pymoo-planner-end")) {
                this.setEndState( Plan.EndState.SUCCESSFUL );
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
