package org.carol.tfm.agents.reservoir;

import bdi4jade.core.SingleCapabilityAgent;
import bdi4jade.extension.planselection.utilitybased.SoftgoalPreferences;
import bdi4jade.goal.Softgoal;
import bdi4jade.plan.Plan;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.domain.capabilities.basic_water_manager.BasicManageDamCapability;
import org.carol.tfm.domain.ontology.configs.ReservoirConfig;

import java.util.Optional;
import java.util.Random;

public class Reservoir extends SingleCapabilityAgent {
    private final Random rand;
    private final String basin_id;
    private final Log log = LogFactory.getLog(this.getClass());
    private final Optional<ReservoirConfig> config;

    public Reservoir(String basin_id, Optional<ReservoirConfig> config, BasicManageDamCapability basicManageDamCapability) {
        super(basicManageDamCapability);
        this.rand = new Random(System.currentTimeMillis());
        this.basin_id = basin_id;
        this.config = config;
    }

    @Override
    protected void init() {
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("reservoir");
        sd.setName("reservoir");
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


    public String getBasin_id() {
        return basin_id;
    }
}
