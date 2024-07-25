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
import jade.lang.acl.MessageTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.agents.reservoir.Reservoir;
import org.carol.tfm.agents.reservoir.goals.IBasinManagementGoal;
import org.carol.tfm.agents.reservoir.goals.NaturalRegimeGoal;
import org.carol.tfm.agents.reservoir.goals.ReleaseWaterGoal;
import org.carol.tfm.agents.reservoir.goals.StorageWaterGoal;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinStatus;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;
import org.carol.tfm.domain.entities.Damn;
import org.carol.tfm.domain.ontology.BasinDefinition;
import org.carol.tfm.domain.ontology.configs.BasinConfig;
import org.carol.tfm.domain.services.DataExportService;

import java.util.*;
import java.util.stream.Collectors;

public class BasicPlanner extends Agent implements GoalListener, BeliefListener {
    private final Log log = LogFactory.getLog(this.getClass());
    private final Map<String, BeliefBase> beliefBase;
    private List<Reservoir> reservoir_agents;
    private AID synchronizatorAID;
    private final BeliefBase timeStepBelieveBase;
    Map<Integer, Integer> data_received_from_step;
    Set<String> basins_goals_achieved;
    Set<String> basins_goals_planned;

    public BasicPlanner(List<Reservoir> reservoir_agents, Map<String, BeliefBase> beliefBase, BeliefBase timeStepBelieveBase) {
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
        List<GoalStatus> ERR_STATUS = Arrays.asList( GoalStatus.PLAN_FAILED );
        if ( event.getStatus().equals(GoalStatus.ACHIEVED ) ) {
            IBasinManagementGoal basinManagementGoal = ( IBasinManagementGoal) event.getGoal();
            basins_goals_achieved.add(basinManagementGoal.getBasin_id() );
            BasinConfig basinConfig = BasinDefinition.BASINS_CONFIG.get( basinManagementGoal.getBasin_id() );

            if ( basinConfig.is_last ) {
                basins_goals_achieved.clear();;
            }

            //He conseguido la meta deseada-> notifico al sincronizador para ver si tengo que cambiar de STEP o no
            notifyGoalAchieved( event.getGoal() );

            if ( !basinConfig.is_header && !basinConfig.is_last ) {
                manageIntermediateBasinsGoalsAchieved( basinConfig.next_basin_id );
            }


            //Si es una cuenca de cabecera y tengo todas las cuencas de cabecera cubiertas
            Set<String> headerBasinIds = BasinDefinition.BASINS_CONFIG.values().stream().filter( b -> b.is_header).map(b -> b.basin_id) .collect(Collectors.toSet());
            if ( basins_goals_achieved.size() == headerBasinIds.size() &&
                    basins_goals_achieved.containsAll( headerBasinIds ) ) {
                manageHeaderBasinsGoalsAchieved();
            }

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
            if ( beliefEvent.getBelief().getName().equals(BeliefNames.BASIN_INFLOW) ) {
                Integer time_step = (Integer) timeStepBelieveBase.getBelief(BeliefNames.TIME_STEP).getValue();
                if ( data_received_from_step.containsKey( time_step ) ) {
                    data_received_from_step.put( time_step, data_received_from_step.get( time_step ) + 1 );
                }  else {
                    data_received_from_step.put( time_step,  1 );
                }

                Float inflow = (Float) beliefEvent.getBelief().getValue();
                if ( data_received_from_step.get(time_step) == BasinDefinition.PLUVIOS.size() ) {
                    manageBeliefChange( (String) beliefEvent.getBelief().getMetadata("basin_id") );
                }
/*
            } else if ( beliefEvent.getBelief().getName().equals(BeliefNames.TIME_STEP ) ) {
                log.info( "[BASIC PLANNER] All basins can execute their plan -> Waiting for Next step");
*/
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
        IBasinManagementGoal basinManagementGoal = ( IBasinManagementGoal) goal;
        BasinConfig config = BasinDefinition.BASINS_CONFIG.get( basinManagementGoal.getBasin_id() );

        if ( goal instanceof ReleaseWaterGoal ) {
            DataExportService.appendLineToActions(config.basin_id + ":: REVERT RELEASE WATER PLAN" );

            //No he podido soltar agua => puedo producir un problema aguas abajo tengo que aumentar el deseo de los embalse de aguas
                //arriba de quedarse con agua Puedo mandarles un mensaje
                // y en cualquier caso tengo que cambiar los Beliefs
            this.log.info("NO DEBERIA SALTAR ESTE FAILURE.");
        } else   if ( goal instanceof  StorageWaterGoal ) {
            DataExportService.appendLineToActions(config.basin_id + ":: REVERT STORAGE PLAN" );

            //No he podido quedarme con agua => puedo producir un problema aguas abajo tengo que aumentar el deseo de los embalse de aguas
            //arriba de quedarse con agua Puedo mandarles un mensaje
            // y en cualquier caso tengo que cambiar los Beliefs
            StorageWaterGoal storageWaterGoal = (StorageWaterGoal) goal;

            List<BasinConfig> upperBasins = BasinDefinition.BASINS_CONFIG.values().stream().filter( b -> b.next_basin_id != null
                                                                                                    && b.next_basin_id.equals( config.basin_id ) ).collect(Collectors.toList());
            Map<String, Float> waterDistribute = new HashMap<>();

            upperBasins.forEach( upperBasin -> {
                if ( BasinDefinition.RESERVOIRS.containsKey( upperBasin.basin_id ) ) {
                    //esta cuenca no tiene embalse => se queda con 0
                } else {
                    //si tengo embalse miro con qué cantidad de agua se puede qudar
                    float waterToBeDistributed = calcWaterToBeDistributed( upperBasin );
                    waterDistribute.put(upperBasin.basin_id, waterToBeDistributed );
                }
            });

            //Compruebo si lo puedo distribuir o no
            final float volumeToStore =  storageWaterGoal.getCurrentInflow() * 1e-6f; //m³/h * 1h = m³ * 1e-6 hm³ / m³ = hm³
            final double volumeToDistribute = waterDistribute.values().stream().mapToDouble( r -> r).sum();
            if ( volumeToDistribute < volumeToStore ) {
                log.warn("[BASIC PLANNER] Can not distribute incoming flow into " + config.basin_id + " into their upper basins.");
                //Tengo embalse seguro pq le he pedido STORAGE
                Damn myDamn = null;
                if ( this.beliefBase.get( config.basin_id ).getBelief( BeliefNames.DAMN_STATUS ) != null ) {
                    myDamn = (Damn) this.beliefBase.get( config.basin_id ).getBelief( BeliefNames.DAMN_STATUS ).getValue();
                }

                Reservoir myReservoir = this.reservoir_agents.stream().filter(r -> r.getBasin_id().equals( config.basin_id )).findAny().get();
                Integer time_step = (Integer) timeStepBelieveBase.getBelief(BeliefNames.TIME_STEP).getValue();

                myReservoir.addGoal( new ReleaseWaterGoal( myDamn, storageWaterGoal.getCurrentInflow(), time_step ), this );

            } else {
                log.info("[BASIC PLANNER] Distributing incoming flow into " + config.basin_id + " into their upper basins.");
            }
        }
    }

    private float calcWaterToBeDistributed(BasinConfig basinConfig) {
        if (  basinConfig.is_header ) return 0;
        float volumeAvaliableToBeDistributed = 0;

        //pregunto a mis cuencas aguas arriba
        List<BasinConfig> upperBasins = BasinDefinition.BASINS_CONFIG.values().stream().filter( b -> b.next_basin_id.equals( basinConfig.basin_id ) ).collect(Collectors.toList());
        for( BasinConfig upperBasin : upperBasins ) {
            Damn upperDamn = (Damn) this.beliefBase.get( upperBasin.basin_id ).getBelief( BeliefNames.DAMN_STATUS).getValue();
            float volumeAvaliable = upperDamn.getMax_capacity() - upperDamn.getCurrent_volume();
            volumeAvaliableToBeDistributed += volumeAvaliable;
        }

        Damn myDamn = (Damn) this.beliefBase.get( basinConfig.basin_id ).getBelief( BeliefNames.DAMN_STATUS).getValue();
        float myVolumeAvaliable = myDamn.getMax_capacity() - myDamn.getCurrent_volume();
        volumeAvaliableToBeDistributed += myVolumeAvaliable;

        return volumeAvaliableToBeDistributed;
    }

    /*
        Ha cambiado algo en el estado de alguna cuenca y tengo por tanto que estudiarlo para poder definir qué GOALS asignar a cada uno
        de mis embalses
     */
    private void manageBeliefChange(String basin_id ) {
        /*Float basinsInflowBeliefSet = (BeliefSet<String, BasinInflow>) beliefBase.get( basin_id )
                .getBelief(BeliefNames.BASIN_INFLOW);
        BeliefSet<String, BasinOutflow> basinsOutflowBeliefSet = (BeliefSet<String, BasinOutflow>) beliefBase.get( basin_id )
                .getBelief(BeliefNames.BASIN_OUTFLOW);
        BeliefSet<String, BasinStatus> basinsStatusBeliefSet = (BeliefSet<String, BasinStatus>) beliefBase.get( basin_id )
                .getBelief(BeliefNames.BASIN_STATUS);
        BeliefSet<String, Damn> damnBeliefSet = (BeliefSet<String, Damn>) beliefBase.get( basin_id )
                .getBelief(BeliefNames.DAMN_STATUS);
*/
        basins_goals_planned.clear();

        Integer time_step = (Integer) timeStepBelieveBase.getBelief(BeliefNames.TIME_STEP).getValue();
        log.info( "[BASIC PLANNER] Updating plan for STEP " + time_step);

        //Primero las cuencas de cabecera
        List<BasinConfig> headerBasins = BasinDefinition.BASINS_CONFIG.values().stream().filter(c -> c.is_header ).collect(Collectors.toList());
        headerBasins.forEach( basinConfig -> {
            generatePlanForBasin( basinConfig );
            try {
                Thread.sleep(100 );
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

/*        BasinDefinition.BASINS.forEach(basin -> {
            Float myInflow = (Float) this.beliefBase.get( basin ).getBelief( BeliefNames.BASIN_INFLOW).getValue();
            BasinStatus.BASIN_STATUS_TYPES myStatus = (BasinStatus.BASIN_STATUS_TYPES) this.beliefBase.get( basin ).getBelief( BeliefNames.BASIN_STATUS ).getValue();
            Damn myDamn = null;
            if ( this.beliefBase.get( basin ).getBelief( BeliefNames.DAMN_STATUS ) != null ) {
                myDamn = (Damn) this.beliefBase.get( basin ).getBelief( BeliefNames.DAMN_STATUS ).getValue();
            }
            Reservoir myReservoir = this.reservoir_agents.stream().filter(r -> r.getBasin_id().equals( basin )).findAny().get();

            if ( myDamn == null ) {
                log.info("\t [PLANNER] para " + basin + ":: REGIMEN NATURAL");
          *//**//*      //no tengo elemento de regulacion => regimen natural
                myReservoir.addGoal( new NaturalRegimeGoal( basin, myInflow, time_step ), this );
            } else if ( myStatus.equals( BasinStatus.BASIN_STATUS_TYPES.FLOOD ) ) {
                log.info("\t [PLANNER] para " + basin + ":: RELEASE");
                myReservoir.addGoal( new ReleaseWaterGoal( myDamn, myInflow, time_step ), this );
            } else {
                log.info("\t [PLANNER] para " + basin + ":: STORAGE");
                myReservoir.addGoal( new StorageWaterGoal( myDamn, myInflow, time_step ), this );
            }

        } );*/
    }



    private void manageIntermediateBasinsGoalsAchieved(String nextBasin) {
        generatePlanForBasin( BasinDefinition.BASINS_CONFIG.get( nextBasin ) );
    }

    private void manageHeaderBasinsGoalsAchieved() {
        Set<String> nextBasins = BasinDefinition.BASINS_CONFIG.values().stream().filter(c -> c.is_header ).map( b -> b.next_basin_id).collect(Collectors.toSet());
        //cuencas de segundo nivel
        nextBasins.forEach( basin_id -> {
            generatePlanForBasin( BasinDefinition.BASINS_CONFIG.get( basin_id ) );
        });
    }

    private void generatePlanForBasin(BasinConfig basinConfig) {
        if ( basins_goals_planned.contains( basinConfig.basin_id )) {
            DataExportService.appendLineToActions("Ignorando petición de generar plan para la cuenca " + basinConfig.basin_id + " al estar repetida.");
            return;
        }

        basins_goals_planned.add(basinConfig.basin_id) ;

        Float myInflow = (Float) this.beliefBase.get( basinConfig.basin_id ).getBelief( BeliefNames.BASIN_INFLOW).getValue();
        BasinStatus.BASIN_STATUS_TYPES myStatus = (BasinStatus.BASIN_STATUS_TYPES) this.beliefBase.get( basinConfig.basin_id ).getBelief( BeliefNames.BASIN_STATUS ).getValue();
        Damn myDamn = null;
        if ( this.beliefBase.get( basinConfig.basin_id ).getBelief( BeliefNames.DAMN_STATUS ) != null ) {
            myDamn = (Damn) this.beliefBase.get( basinConfig.basin_id ).getBelief( BeliefNames.DAMN_STATUS ).getValue();
        }
        Reservoir myReservoir = this.reservoir_agents.stream().filter(r -> r.getBasin_id().equals( basinConfig.basin_id )).findAny().get();

        Integer time_step = (Integer) timeStepBelieveBase.getBelief(BeliefNames.TIME_STEP).getValue();

        if ( myDamn == null ) {
            DataExportService.appendLineToActions(basinConfig.basin_id + ":: REGIMEN NATURAL" );

            log.info("\t [PLANNER] para " + basinConfig.basin_id + ":: REGIMEN NATURAL");
            //no tengo elemento de regulacion => regimen natural
            myReservoir.addGoal( new NaturalRegimeGoal( basinConfig.basin_id, myInflow, time_step ), this );
        } else if ( myStatus.equals( BasinStatus.BASIN_STATUS_TYPES.FLOOD ) ) {
            DataExportService.appendLineToActions(basinConfig.basin_id + ":: RELEASE" );

            log.info("\t [PLANNER] para " + basinConfig.basin_id + ":: RELEASE");
            myReservoir.addGoal( new ReleaseWaterGoal( myDamn, myInflow, time_step ), this );
        } else {
            DataExportService.appendLineToActions(basinConfig.basin_id + ":: STORAGE" );

            log.info("\t [PLANNER] para " + basinConfig.basin_id + ":: STORAGE");
            myReservoir.addGoal( new StorageWaterGoal( myDamn, myInflow, time_step ), this );
        }
    }
}
