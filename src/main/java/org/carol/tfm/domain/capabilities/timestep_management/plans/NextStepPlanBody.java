package org.carol.tfm.domain.capabilities.timestep_management.plans;

import bdi4jade.plan.Plan;
import bdi4jade.plan.planbody.AbstractPlanBody;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;
import org.carol.tfm.domain.services.DataExportService;

public class NextStepPlanBody extends AbstractPlanBody {
    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public void action() {
        Integer currentTs = (Integer) this.getBeliefBase().getBelief(BeliefNames.TIME_STEP ).getValue();
        Integer newTs = currentTs == null ? 0 : (currentTs + 1) ;

        DataExportService.appendLineToActions("STEP " + newTs );

        log.info("*************************************************************************************");
        log.info("********** STEP: " + newTs );
        this.getBeliefBase().updateBelief(BeliefNames.TIME_STEP, newTs );
        setEndState(Plan.EndState.SUCCESSFUL);
    }

}
