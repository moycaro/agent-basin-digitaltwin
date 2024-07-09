package org.carol.tfm;

import bdi4jade.belief.BeliefBase;
import bdi4jade.event.GoalEvent;
import bdi4jade.event.GoalListener;
import jade.BootProfileImpl;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.PlatformController;
import jade.wrapper.StaleProxyException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.carol.tfm.agents.gauge.FlowGauge;
import org.carol.tfm.agents.gauge.Pluvio;
import org.carol.tfm.agents.planner.BasicPlanner;
import org.carol.tfm.agents.planner.SynchronizatorAgent;
import org.carol.tfm.domain.capabilities.basic_water_manager.BasicManageDamCapability;
import org.carol.tfm.agents.reservoir.Reservoir;
import org.carol.tfm.agents.StoreDataAgent;
import org.carol.tfm.domain.capabilities.timestep_management.TimeStepManagementCapability;
import org.carol.tfm.domain.capabilities.timestep_management.goals.NextStepGoal;
import org.carol.tfm.domain.ontology.BasinDefinition;
import org.carol.tfm.domain.ontology.configs.PluviometerConfig;
import org.carol.tfm.domain.ontology.configs.ReservoirConfig;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws Exception {
        LoggerContext context = (org.apache.logging.log4j.core.LoggerContext)
                LogManager.getContext(false);
        context.setConfigLocation(Main.class
                .getResource("log4j2.configurationFile").toURI());

        //new Main().createAndShowGUI();
        Main runner = new Main();
    }

    //private final PilotoPanel agentTestPanel;
    private ProfileImpl bootProfile;
    private final Log log;
    private jade.core.Runtime runtime;
    private final Map<String, BeliefBase> basicManageDamCapability = new HashMap<>();
    private List<Reservoir> reservoir_agents = new ArrayList<>();
    private List<Pluvio> pluvio_agents = new ArrayList<>();

    public Main() {
        this.log = LogFactory.getLog(this.getClass());
      //  this.agentTestPanel = new PilotoPanel();

        List<String> params = new ArrayList<String>();
        params.add("-gui");
        params.add("-detect-main:false");

        log.info("Plataform parameters: " + params);

        this.bootProfile = new BootProfileImpl(params.toArray(new String[0]));

        this.runtime = jade.core.Runtime.instance();
        PlatformController controller = runtime
                .createMainContainer(bootProfile);
        //Capability para gestión del paso temporal
        TimeStepManagementCapability timeStepManagementCapability = new TimeStepManagementCapability();

        try {
            StoreDataAgent storeDataAgent = new StoreDataAgent( this.basicManageDamCapability );
            AgentController acStoreData = ((AgentContainer) controller).acceptNewAgent("StoreData::", storeDataAgent );
            acStoreData.start();

            for (String basin_id : BasinDefinition.BASINS) {
                Optional<ReservoirConfig> reservoirConfigOpt = BasinDefinition.RESERVOIRS.containsKey( basin_id ) ? Optional.of( BasinDefinition.RESERVOIRS.get( basin_id ) ) : Optional.empty();
                BasicManageDamCapability currentDamnCapability = new BasicManageDamCapability(basin_id, reservoirConfigOpt);

                basicManageDamCapability.put( basin_id, currentDamnCapability.getBeliefBase());
                Reservoir reservoir = new Reservoir(basin_id, reservoirConfigOpt ,  currentDamnCapability);
                AgentController basin_reservoir = ((AgentContainer) controller).acceptNewAgent("Reservoir::" + basin_id, reservoir );
                reservoir_agents.add(  reservoir );
                basin_reservoir.start();

                Pluvio pluvio = null;
                if ( BasinDefinition.PLUVIOS.containsKey( basin_id ) ) {
                    PluviometerConfig config =  BasinDefinition.PLUVIOS.get( basin_id );
                    pluvio = new Pluvio( basin_id, config, timeStepManagementCapability.getBeliefBase() );
                    AgentController basin_pluvio = ((AgentContainer) controller).acceptNewAgent(config.sensor_id,  pluvio);
                    pluvio_agents.add( pluvio );
                    basin_pluvio.start();
                    timeStepManagementCapability.getBeliefBase().addBeliefListener(pluvio);
                }

                FlowGauge flowGauge = null;
                if ( BasinDefinition.GAUGES.containsKey( basin_id ) ) {
                    flowGauge = new FlowGauge( basin_id, BasinDefinition.GAUGES.get( basin_id ) );
                    AgentController basin_flow_gauge = ((AgentContainer) controller).acceptNewAgent("Gauge::Flow::" + basin_id,  flowGauge);
                    currentDamnCapability.getBeliefBase().addBeliefListener(flowGauge);
                    basin_flow_gauge.start();
                }
            }

            SynchronizatorAgent synchronizatorAgent = new SynchronizatorAgent( timeStepManagementCapability );
            AgentController acSynchornizator = ((AgentContainer) controller).acceptNewAgent("Synchronizator::", synchronizatorAgent );
            acSynchornizator.start();

            BasicPlanner basicPlanner = new BasicPlanner( this.reservoir_agents, this.basicManageDamCapability, timeStepManagementCapability.getBeliefBase() );
            AgentController acBasicPlanner = ((AgentContainer) controller).acceptNewAgent("Planner::Basic", basicPlanner );

            this.basicManageDamCapability.values().stream().forEach(believeBase -> believeBase.addBeliefListener(basicPlanner) );
            acBasicPlanner.start();

            log.info("*************************************************************************************");
            log.info("********** STEP: " + 0);
            Thread.sleep( Duration.ofSeconds(2));

            synchronizatorAgent.addGoal( new NextStepGoal(), synchronizatorAgent );
        } catch (StaleProxyException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
