package org.carol.tfm.agents.planner;

import bdi4jade.annotation.Belief;
import bdi4jade.belief.BeliefBase;
import bdi4jade.belief.BeliefSet;
import bdi4jade.event.BeliefEvent;
import bdi4jade.event.BeliefListener;
import bdi4jade.event.GoalEvent;
import bdi4jade.event.GoalListener;
import bdi4jade.goal.Goal;
import bdi4jade.goal.GoalStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.Main;
import org.carol.tfm.agents.reservoir.Reservoir;
import org.carol.tfm.agents.reservoir.goals.ReleaseWaterGoal;
import org.carol.tfm.agents.reservoir.goals.StorageWaterGoal;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinInflow;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinOutflow;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinStatus;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;
import org.carol.tfm.domain.entities.Damn;

import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Random;

public class BasicPlanner extends Agent implements GoalListener, BeliefListener {
    private final Log log = LogFactory.getLog(this.getClass());
    private final BeliefBase beliefBase;
    private List<Reservoir> reservoir_agents;
    private AID synchronizatorAID;

    public BasicPlanner(List<Reservoir> reservoir_agents, BeliefBase beliefBase) {
        super();
        this.beliefBase = beliefBase;
        this.reservoir_agents = reservoir_agents;
    }

    // Put agent initializations here
    protected void setup() {
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("basin-planner");
        sd.setName("Basic-Planner");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            log.error( "[BASIC PLANNER] Can not register.", fe);
        }

        //Get synchronizator name
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sdSearch = new ServiceDescription();
        sdSearch.setType("synchronizator");
        template.addServices(sdSearch);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            this.synchronizatorAID = result[0].getName();
        }
        catch (FIPAException fe) {
            log.error("Can not find Synchronizator Agent.");
            fe.printStackTrace();
        }
    }

    public void goalPerformed(GoalEvent event) {
        List<GoalStatus> ERR_STATUS = Arrays.asList( GoalStatus.PLAN_FAILED, GoalStatus.UNACHIEVABLE );
        if ( event.getStatus().equals(GoalStatus.ACHIEVED ) ) {
            //He conseguido la meta deseada-> notifico al sincronizador para ver si tengo que cambiar de STEP o no
            notifyGoalAchieved( event.getGoal() );
        } else  if ( ERR_STATUS.contains( event.getStatus() ) ) {
            //no puedo conseguir el plan => tengo que recalcular planes
            revertPlan( event.getGoal() );
        }
    }

    private void notifyGoalAchieved(Goal goal) {
        // Send the cfp to all sellers
        ACLMessage info = new ACLMessage(ACLMessage.INFORM);
        info.addReceiver(this.synchronizatorAID);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String goalAsString = objectMapper.writeValueAsString(goal);
            info.setContent(goalAsString);
            info.setConversationId("planner-goal-performed");
            this.send(info);
        } catch (JsonProcessingException e) {
            this.log.error("Can not sent msg with achieved goal", e);
        }
    }

    @Override
    public void eventOccurred(BeliefEvent beliefEvent) {
        if ( beliefEvent.getAction().equals( BeliefEvent.Action.BELIEF_UPDATED )) {
            if ( beliefEvent.getBelief().getName().equals(BeliefNames.BASIN_INFLOW ) ) {
                log.info( "[BASIC PLANNER] New Inflow data received: Updating plan");
                manageBeliefChange( );
            } else if ( beliefEvent.getBelief().getName().equals(BeliefNames.TIME_STEP ) ) {
                log.info( "[BASIC PLANNER] All basins can execute their plan -> Waiting for Next step");
            }
        }


        //ADD -> inicialization
    }

    /**
     * No puedo alcanzar un plan => tengo que intentar
     * @param goal
     */
    private void revertPlan(Goal goal) {
        log.info( "[BASIC PLANNER] Goal con not be achieved=> revert plan.");

        if ( goal instanceof ReleaseWaterGoal ) {
            //No he podido soltar agua => puedo producir un problema aguas abajo tengo que aumentar el deseo de los embalse de aguas
                //arriba de quedarse con agua Puedo mandarles un mensaje
                // y en cualquier caso tengo que cambiar los Beliefs
            this.log.info("NO DEBERIA SALTAR ESTE FAILURE.");
        } else   if ( goal instanceof  StorageWaterGoal ) {
            //No he podido quedarme con agua => puedo producir un problema aguas abajo tengo que aumentar el deseo de los embalse de aguas
            //arriba de quedarse con agua Puedo mandarles un mensaje
            // y en cualquier caso tengo que cambiar los Beliefs
            this.log.info("Tengo que añadir agua a la siguiente cuenca que no he podido almacenar en la mía.");

            //CAmbiaré parámetros del planner para decidir cómo ejecutarlo y adelante, le tendré que actualizar al inflow de la cuenca
                //el agua que sale de la anterior
            manageBeliefChange();
        }
    }

    /*
        Ha cambiado algo en el estado de alguna cuenca y tengo por tanto que estudiarlo para poder definir qué GOALS asignar a cada uno
        de mis embalses
     */
    private void manageBeliefChange() {
        BeliefSet<String, BasinInflow> basinsInflowBeliefSet = (BeliefSet<String, BasinInflow>) beliefBase
                .getBelief(BeliefNames.BASIN_INFLOW);
        BeliefSet<String, BasinOutflow> basinsOutflowBeliefSet = (BeliefSet<String, BasinOutflow>) beliefBase
                .getBelief(BeliefNames.BASIN_OUTFLOW);
        BeliefSet<String, BasinStatus> basinsStatusBeliefSet = (BeliefSet<String, BasinStatus>) beliefBase
                .getBelief(BeliefNames.BASIN_STATUS);
        BeliefSet<String, Damn> damnBeliefSet = (BeliefSet<String, Damn>) beliefBase
                .getBelief(BeliefNames.DAMN_STATUS);
        Integer time_step = (Integer) beliefBase.getBelief("time_step").getValue();

        Main.BASINS.forEach( basin -> {BasinInflow myInflow = basinsInflowBeliefSet.getValue().stream().filter( inflow -> inflow.getBasin_id().equals( basin )).findAny().get();
            BasinStatus myStatus = basinsStatusBeliefSet.getValue().stream().filter( inflow -> inflow.getBasin_id().equals( basin )).findAny().get();
            Damn myDamn = damnBeliefSet.getValue().stream().filter( inflow -> inflow.getBasin_id().equals( basin )).findAny().get();

            Reservoir myReservoir = this.reservoir_agents.stream().filter(r -> r.getBasin_id().equals( basin )).findAny().get();

            if ( myStatus.getCurrent_status().equals( BasinStatus.BASIN_STATUS_TYPES.FLOOD ) ) {
                myReservoir.addGoal( new ReleaseWaterGoal( myDamn, myInflow.getMm(), time_step ), this );
            } else {
                myReservoir.addGoal( new StorageWaterGoal( myDamn, myInflow.getMm(), time_step ), this );
            }

        } );
    }
}
