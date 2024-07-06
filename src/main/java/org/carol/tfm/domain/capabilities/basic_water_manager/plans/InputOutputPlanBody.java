package org.carol.tfm.domain.capabilities.basic_water_manager.plans;

import bdi4jade.annotation.Belief;
import bdi4jade.annotation.Parameter;
import bdi4jade.belief.BeliefSet;
import bdi4jade.belief.TransientBeliefSet;
import bdi4jade.plan.Plan;
import bdi4jade.plan.planbody.AbstractPlanBody;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.agents.reservoir.goals.ReleaseWaterGoal;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinInflow;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinOutflow;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinStatus;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;
import org.carol.tfm.domain.entities.Damn;

import java.util.Set;

public class InputOutputPlanBody extends AbstractPlanBody {

    private final Log log = LogFactory.getLog(this.getClass());

    @Belief(name = BeliefNames.BASIN_STATUS)
    private BeliefSet<String, BasinStatus> basinStatus;
    @Belief(name = BeliefNames.BASIN_INFLOW)
    private BeliefSet<String, BasinInflow> basinInflow;
    @Belief(name = BeliefNames.BASIN_OUTFLOW)
    private BeliefSet<String, BasinOutflow> basinOutflow;

    private Damn damn;
    private float currentInflow;

    @Override
    public void action() {
        //INPUT paso anterior y estado actual
        BasinStatus myBasinStatus = basinStatus.getValue().stream().filter( status -> status.getBasin_id().equals( this.damn.getBasin_id())).findAny().get();
/*
        if ( myBasinStatus.getCurrent_status().equals( BasinStatus.BASIN_STATUS_TYPES.FLOOD ) ) {
            log.info("I/O PLan failed because current basin is in Flood alarm: " + this.damn.toString() );
            setEndState(Plan.EndState.FAILED);
        }  else {

 */
            log.info("Actualizando salida del embalse " + this.damn.getBasin_id() + " " + currentInflow);
            //OUTPUT estado siguiente => Las entradas pasan a ser salidas
            BasinOutflow myasinOutflow = basinOutflow.getValue().stream().filter( status -> status.getBasin_id().equals( this.damn.getBasin_id())).findAny().get();

            ReleaseWaterGoal releaseWaterGoal = ( ReleaseWaterGoal ) this.getGoal();
            releaseWaterGoal.setOutflow( currentInflow );

            Set<BasinOutflow> modifiedOutflowBeliefeSet = basinOutflow.getValue();
            modifiedOutflowBeliefeSet.stream().filter( basinOutflow -> basinOutflow.getBasin_id().equals( this.damn.getBasin_id() ))
                    .findAny().get().setMm( currentInflow );

            boolean isSafe = askFLowGauges( releaseWaterGoal.getOutflow(), releaseWaterGoal.getDamn().getBasin_id() );

            if (  isSafe ) {
                //OUTPUT presa => no cambia su estado ya que no almaceno nada
                setEndState(Plan.EndState.SUCCESSFUL);
            } else {
                //VVOy a producir una inundación tengo qyue preguntar a los embalses qué cantidad de agua se pueden quedar
                setEndState(Plan.EndState.FAILED);
            }
        //}
    }

    private boolean askFLowGauges(float outflow, String basinId) {
        // Enviar mensaje al otro agente
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("Gauge::Flow::" + basinId , AID.ISLOCALNAME));
        msg.setContent( String.valueOf(outflow));
        myAgent.send(msg);

        // Esperar la respuesta
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
        ACLMessage reply = myAgent.blockingReceive(mt);

        if (reply != null) {
            return Boolean.parseBoolean( reply.getContent() );
        }

        return false;
    }

    @Parameter(direction = Parameter.Direction.IN, mandatory = true)
    public void setDamn(Damn damn) {
        this.damn = damn;
    }

    @Parameter(direction = Parameter.Direction.IN, mandatory = true)
    public void setCurrentInflow(float currentInflow) {
        this.currentInflow = currentInflow;
    }


}
