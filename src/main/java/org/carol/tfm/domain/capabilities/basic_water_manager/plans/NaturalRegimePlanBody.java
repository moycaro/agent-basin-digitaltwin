package org.carol.tfm.domain.capabilities.basic_water_manager.plans;

import bdi4jade.plan.Plan;
import bdi4jade.plan.planbody.AbstractPlanBody;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class NaturalRegimePlanBody extends AbstractPlanBody {
    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public void action() {
        log.info("Natural Regime Plan ");
        setEndState(Plan.EndState.SUCCESSFUL);
    }
}
