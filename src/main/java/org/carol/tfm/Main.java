package org.carol.tfm;

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

import java.time.Duration;
import java.util.*;

public class Main   {

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
    private final BasicManageDamCapability basicManageDamCapability;
    private List<Reservoir> reservoir_agents;
    private List<Pluvio> pluvio_agents;
    public static final List<String> BASINS = List.of("basin_1");

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

        try {
            //shared between all reservoir agents
            this.basicManageDamCapability = new BasicManageDamCapability();
            this.reservoir_agents = new ArrayList<>();
            this.pluvio_agents = new ArrayList<>();
            StoreDataAgent storeDataAgent = new StoreDataAgent( this.basicManageDamCapability.getBeliefBase()  );
            AgentController acStoreData = ((AgentContainer) controller).acceptNewAgent("StoreData::", storeDataAgent );
            acStoreData.start();

            SynchronizatorAgent synchronizatorAgent = new SynchronizatorAgent( this.basicManageDamCapability.getBeliefBase() );
            AgentController acSynchornizator = ((AgentContainer) controller).acceptNewAgent("Synchronizator::", synchronizatorAgent );
            acSynchornizator.start();

            BasicPlanner basicPlanner = new BasicPlanner( this.reservoir_agents, this.basicManageDamCapability.getBeliefBase() );
            AgentController acBasicPlanner = ((AgentContainer) controller).acceptNewAgent("Planner::Basic", basicPlanner );

            this.basicManageDamCapability.getBeliefBase().addBeliefListener(basicPlanner);
            acBasicPlanner.start();

            for (String basin_id : BASINS) {
                Pluvio pluvio = new Pluvio( basin_id );
                Reservoir reservoir = new Reservoir(basin_id,this.basicManageDamCapability);
                FlowGauge flowGauge = new FlowGauge( basin_id, new float[] {30f, 40f, 50f, 60f, 70f} );
                AgentController basin_pluvio = ((AgentContainer) controller).acceptNewAgent("Gauge::Pluvio::" + basin_id,  pluvio);
                AgentController basin_reservoir = ((AgentContainer) controller).acceptNewAgent("Reservoir::" + basin_id, reservoir );
                AgentController basin_flow_gauge = ((AgentContainer) controller).acceptNewAgent("Gauge::Flow::" + basin_id,  flowGauge);

                reservoir_agents.add(  reservoir );
                pluvio_agents.add( pluvio );

                this.basicManageDamCapability.getBeliefBase().addBeliefListener(pluvio);
                this.basicManageDamCapability.getBeliefBase().addBeliefListener(flowGauge);

                basin_pluvio.start();
                basin_reservoir.start();
                basin_flow_gauge.start();
            }


            log.info("*************************************************************************************");
            log.info("********** STEP: " + 0);
            Thread.sleep( Duration.ofSeconds(2));
            this.basicManageDamCapability.getBeliefBase().updateBelief("time_step",  0 );
        } catch (StaleProxyException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
