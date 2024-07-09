package org.carol.tfm.agents.planner;

import bdi4jade.core.SingleCapabilityAgent;
import bdi4jade.event.GoalEvent;
import bdi4jade.event.GoalListener;
import bdi4jade.goal.GoalStatus;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.domain.capabilities.timestep_management.TimeStepManagementCapability;
import org.carol.tfm.domain.capabilities.timestep_management.goals.NextStepGoal;
import org.carol.tfm.domain.capabilities.timestep_management.goals.WaitForExecutionsGoal;


public class SynchronizatorAgent extends SingleCapabilityAgent implements GoalListener {
    private final Log log = LogFactory.getLog(this.getClass());

    public SynchronizatorAgent(TimeStepManagementCapability timeStepManagementCapability) {
        super(timeStepManagementCapability);
    }

    @Override
    protected void init() {
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
            log.error( "[RESERVOIR] Can not register.", fe);
        }

        /*getCapability().getBeliefBase().addBelief(
                new TransientBelief<String, GenericValueFunction<Integer>>(
                        SATISFACTION, new GenericValueFunction<Integer>()));

         */
    }

    @Override
    public void goalPerformed(GoalEvent goalEvent) {
        if (goalEvent.getStatus().equals(GoalStatus.ACHIEVED)) {
            if ( goalEvent.getGoal() instanceof NextStepGoal) {
                this.addGoal( new WaitForExecutionsGoal(), this );
            } else if ( goalEvent.getGoal() instanceof  WaitForExecutionsGoal ) {
                this.addGoal( new NextStepGoal(), this );
            }
        }
    }
}
