package org.carol.tfm.agents.planner;

import bdi4jade.belief.BeliefBase;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.agents.reservoir.Reservoir;
import org.carol.tfm.agents.reservoir.goals.IBasinManagementGoal;
import org.carol.tfm.agents.reservoir.goals.ReleaseWaterGoal;
import org.carol.tfm.agents.reservoir.goals.SetVolumeGoal;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinStatus;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;
import org.carol.tfm.domain.entities.Damn;
import org.carol.tfm.domain.ontology.BasinDefinition;
import org.carol.tfm.domain.ontology.configs.BasinConfig;
import org.carol.tfm.domain.services.DataExportService;
import org.carol.tfm.external_planners.application.GetPymooNSGA2Results;
import org.carol.tfm.external_planners.domain.entities.DamnManagement;
import org.carol.tfm.external_planners.domain.entities.ExternalPlannerResponse;

import java.util.*;
import java.util.stream.Collectors;

public class PymooPlanner extends Agent implements GoalListener, BeliefListener {
    private final Log log = LogFactory.getLog(this.getClass());
    private final Map<String, BeliefBase> beliefBase;
    private List<Reservoir> reservoir_agents;
    private AID synchronizatorAID;
    private final BeliefBase timeStepBelieveBase;
    Map<Integer, Integer> data_received_from_step;
    Set<String> basins_goals_achieved;
    Set<String> basins_goals_planned;

    public PymooPlanner(List<Reservoir> reservoir_agents, Map<String, BeliefBase> beliefBase, BeliefBase timeStepBelieveBase) {
        super();
        this.beliefBase = beliefBase;
        this.reservoir_agents = reservoir_agents;
        this.timeStepBelieveBase = timeStepBelieveBase;
        this.data_received_from_step = new HashMap<>();
        this.basins_goals_achieved = new HashSet<>();
        this.basins_goals_planned = new HashSet<>();
    }

    // Put agent initializations here
    protected void setup() {
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("basin-planner");
        sd.setName("Pymoo-Planner");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            log.error( "[Pymoo PLANNER] Can not register.", fe);
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
    }

    @Override
    public void eventOccurred(BeliefEvent beliefEvent) {
        if ( beliefEvent.getAction().equals( BeliefEvent.Action.BELIEF_UPDATED )) {
            if ( beliefEvent.getBelief().getName().equals(BeliefNames.BASIN_INFLOW) ) {
                Integer time_step = (Integer) timeStepBelieveBase.getBelief(BeliefNames.TIME_STEP).getValue();
                if ( data_received_from_step.containsKey( time_step ) ) {
                    data_received_from_step.put( time_step, data_received_from_step.get( time_step ) + 1 );
                }  else {
                    data_received_from_step.put( time_step,  1 );
                }

                Float inflow = (Float) beliefEvent.getBelief().getValue();
                if ( data_received_from_step.get(time_step) == BasinDefinition.PLUVIOS.size() ) {
                    manageBeliefChange( );
                }
/*
            } else if ( beliefEvent.getBelief().getName().equals(BeliefNames.TIME_STEP ) ) {
                log.info( "[BASIC PLANNER] All basins can execute their plan -> Waiting for Next step");
*/
            }
        }


        //ADD -> inicialization
    }

    /*
        Ha cambiado algo en el estado de alguna cuenca y tengo por tanto que estudiarlo para poder definir qué GOALS asignar a cada uno
        de mis embalses
    */
    private void manageBeliefChange() {
        Map<String, Float> inflowBeliefs = new HashMap<>();
        List<Damn> damns = new ArrayList<>();
        beliefBase.keySet().forEach( basin_id -> {
            Float myInflow = (Float) this.beliefBase.get( basin_id ).getBelief( BeliefNames.BASIN_INFLOW).getValue();
            float volumeToStore =  myInflow * 1e-6f; //m³/h * 1h = m³ * 1e-6 hm³ / m³ = hm³

            inflowBeliefs.put( basin_id, volumeToStore );

            if ( this.beliefBase.get(basin_id ).getBelief( BeliefNames.DAMN_STATUS ) != null ) {
                Damn myDamn = (Damn) this.beliefBase.get( basin_id ).getBelief( BeliefNames.DAMN_STATUS ).getValue();
                if ( myDamn != null) {
                    damns.add( myDamn );
                }
            }
        });

        Integer time_step = (Integer) timeStepBelieveBase.getBelief(BeliefNames.TIME_STEP).getValue();

        log.info( "[Pymoo PLANNER] Updating plan for STEP " + time_step);

        //Le pido al PyhonService de Pymoo que se ejecute
        GetPymooNSGA2Results getPymooNSGA2Results = new GetPymooNSGA2Results();
        Optional<ExternalPlannerResponse> plannerResponse = getPymooNSGA2Results.execute( inflowBeliefs, damns );

/*
        if ( plannerResponse.isEmpty() ) {
            log.warn( "[Pymoo PLANNER] NO PLAN FOUND");
            this.reservoir_agents.stream().forEach( reservoirAgent -> {
                //reservoirAgent.addGoal( new SetVolumeGoal( new DamnManagement(reservoirAgent.getBasin_id(), 0f), time_step ), this );
            });
        } else {
            log.warn("[Pymoo PLANNER] :) PLAN FOUND");
            List<DamnManagement> damnManagementList = plannerResponse.get().getDamnManagementList();
            this.reservoir_agents.stream().forEach(reservoirAgent -> {
                Optional<DamnManagement> optionalDamnManagement = damnManagementList.stream().filter(m -> m.getBasin_id().equals(reservoirAgent.getBasin_id())).findAny();
                if (optionalDamnManagement.isEmpty()) {
                    log.warn("No debería ocurrir que nos falte un plan para un embalse.");
                    reservoirAgent.addGoal(new SetVolumeGoal(new DamnManagement(reservoirAgent.getBasin_id(), 0f), time_step), this);
                } else {
                    reservoirAgent.addGoal(new SetVolumeGoal( optionalDamnManagement.get() , time_step), this);
                }
            });
        }
*/
        setBeliefs( plannerResponse, time_step );

        notifyGoalAchieved();
    }

    private void setBeliefs(Optional<ExternalPlannerResponse> plannerResponse, Integer timeStep) {
        // C1 Cuenca de cabecera
        Float myInflow = (Float) this.beliefBase.get( "c1" ).getBelief( BeliefNames.BASIN_INFLOW).getValue();
        final float inflowVolume =  myInflow * 1e-6f; //m³/h * 1h = m³ * 1e-6 hm³ / m³ = hm³
        Optional<DamnManagement> myManagement = plannerResponse.isEmpty() ? Optional.empty() : plannerResponse.get().getDamnManagementList().stream().filter( d -> d.getBasin_id().equals("c1")).findAny();
        float planToStorage = myManagement.isEmpty() ? 0f : myManagement.get().getVol_to_be_storaged();
        if ( planToStorage > inflowVolume ) {
            planToStorage = inflowVolume;
        }
        float released_water1 = inflowVolume - planToStorage;
            //OUTPUT outflow => MAX de lo que la presa es capaz de almacenar
            this.beliefBase.get( "c1").updateBelief( BeliefNames.BASIN_OUTFLOW, released_water1 > 0 ? released_water1 : 0f );
            //OUTPUT presa => agua almacenada
            Damn myDamn = (Damn) this.beliefBase.get( "c1" ).getBelief( BeliefNames.DAMN_STATUS ).getValue();
            float currentVolume = myDamn.getCurrent_volume() + planToStorage;
            myDamn.setCurrent_volume( currentVolume );
            this.beliefBase.get("c1").updateBelief( BeliefNames. DAMN_STATUS, myDamn );
DataExportService.appendLineToReservoirC1("" + myDamn.getCurrent_volume() );

        // C2 Cuenca de cabecera
        Float myInflow2 = (Float) this.beliefBase.get( "c2" ).getBelief( BeliefNames.BASIN_INFLOW).getValue();
        final float inflowVolume2 =  myInflow2 * 1e-6f; //m³/h * 1h = m³ * 1e-6 hm³ / m³ = hm³
        Optional<DamnManagement> myManagement2 = plannerResponse.isEmpty() ? Optional.empty() : plannerResponse.get().getDamnManagementList().stream().filter( d -> d.getBasin_id().equals("c2")).findAny();
        float planToStorage2 = myManagement.isEmpty() ? 0f : myManagement2.get().getVol_to_be_storaged();
        if ( planToStorage2 > inflowVolume2 ) {
            planToStorage2 = inflowVolume2;
        }

        float released_water2 = inflowVolume2 - planToStorage2;
            //OUTPUT outflow => MAX de lo que la presa es capaz de almacenar
            this.beliefBase.get( "c2").updateBelief( BeliefNames.BASIN_OUTFLOW, released_water2 > 0 ? released_water2 : 0f );
            //OUTPUT presa => agua almacenada
            Damn myDamn2 = (Damn) this.beliefBase.get( "c2" ).getBelief( BeliefNames.DAMN_STATUS ).getValue();
            float currentVolume2 = myDamn2.getCurrent_volume() + planToStorage2;
            myDamn2.setCurrent_volume( currentVolume2 );
            this.beliefBase.get("c2").updateBelief( BeliefNames.DAMN_STATUS, myDamn2 );
DataExportService.appendLineToReservoirC2("" + myDamn2.getCurrent_volume() );

        // C3 Cuenca intermedia
        Float myInflow3 = (Float) this.beliefBase.get( "c3" ).getBelief( BeliefNames.BASIN_INFLOW).getValue();
        final float inflowVolume3 =  myInflow3 * 1e-6f; //m³/h * 1h = m³ * 1e-6 hm³ / m³ = hm³
        final float inflowVolumeAdjusted3 = inflowVolume3 + released_water1 + released_water2;
        Optional<DamnManagement> myManagement3 = plannerResponse.isEmpty() ? Optional.empty() : plannerResponse.get().getDamnManagementList().stream().filter( d -> d.getBasin_id().equals("c3")).findAny();
        float planToStorage3 = myManagement.isEmpty() ? 0f : myManagement3.get().getVol_to_be_storaged();
        if ( planToStorage3 > inflowVolumeAdjusted3 ) {
            planToStorage3 = inflowVolumeAdjusted3;
        }
        float released_water3 = inflowVolumeAdjusted3 - planToStorage3;
            //OUTPUT outflow => MAX de lo que la presa es capaz de almacenar
            this.beliefBase.get( "c3").updateBelief( BeliefNames.BASIN_OUTFLOW, released_water3 > 0 ? released_water3 : 0f );
            //OUTPUT presa => agua almacenada
            Damn myDamn3 = (Damn) this.beliefBase.get( "c3" ).getBelief( BeliefNames.DAMN_STATUS ).getValue();
            float currentVolume3 = myDamn3.getCurrent_volume() + planToStorage3;
            myDamn3.setCurrent_volume( currentVolume3 );
            this.beliefBase.get("c3").updateBelief( BeliefNames.DAMN_STATUS, myDamn3 );
DataExportService.appendLineToReservoirC3("" + myDamn3.getCurrent_volume() );

        // C4 Cuenca de cierre
        final float inflowVolumeAdjusted4 = released_water3;
        float released_water4 = inflowVolumeAdjusted4;
            //OUTPUT outflow => MAX de lo que la presa es capaz de almacenar
            this.beliefBase.get( "c4").updateBelief( BeliefNames.BASIN_OUTFLOW, released_water4 > 0 ? released_water4 : 0f );


log.info( "C1: inflow " + inflowVolume + " storage " + planToStorage + " release: " + released_water1 + " current Volume" + currentVolume);
log.info( "C2: inflow " + inflowVolume2 + "storage " + planToStorage2 + " release: " + released_water2 + " current Volume" + currentVolume2);
log.info( "C3: inflow " + inflowVolume3 +  "(adjusted: " + inflowVolumeAdjusted3 + ") storage "  + planToStorage3 + " release: " + released_water3 + " current Volume" + currentVolume3);
log.info( "C4: (adjusted: " + inflowVolumeAdjusted4 + ") release: " + released_water4);
    }

    private void notifyGoalAchieved() {
        // Send the cfp to all sellers
        ACLMessage info = new ACLMessage(ACLMessage.INFORM);
        info.addReceiver(this.synchronizatorAID);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            info.setContent("");
            info.setConversationId("pymoo-planner-end");
            this.send(info);
        } catch (Exception e) {
            this.log.error("Can not sent msg with achieved goal", e);
        }
    }

}
