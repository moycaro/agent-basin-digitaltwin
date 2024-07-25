package org.carol.tfm.domain.capabilities.basic_water_manager.plans;

import bdi4jade.annotation.Belief;
import bdi4jade.annotation.Parameter;
import bdi4jade.plan.Plan;
import bdi4jade.plan.planbody.AbstractPlanBody;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.agents.reservoir.goals.ReleaseWaterGoal;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinStatus;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BeliefNames;
import org.carol.tfm.domain.entities.Damn;
import org.carol.tfm.domain.ontology.BasinDefinition;
import org.carol.tfm.domain.ontology.configs.FlowGaugeConfig;
import org.carol.tfm.domain.services.DataExportService;

public class ReleaseWaterPlanBody extends AbstractPlanBody {

    private final Log log = LogFactory.getLog(this.getClass());

    @Belief(name = BeliefNames.BASIN_STATUS)
    private BasinStatus.BASIN_STATUS_TYPES basinStatus;
    @Belief(name = BeliefNames.BASIN_INFLOW)
    private Float basinInflow;
    @Belief(name = BeliefNames.BASIN_OUTFLOW)
    private Float basinOutflow;


    private Damn damn;
    private float currentInflow;
    private String basin_id;
    private Integer time_step;

    @Override
    public void action() {
        //INPUT paso anterior y estado actual
        log.info("\t\t[IOPlan] [" + this.damn.getBasin_id() + "] Damn max vol: " + this.damn.getMax_capacity() + "; current vol " + this.damn.getCurrent_volume() );
/*
        if ( myBasinStatus.getCurrent_status().equals( BasinStatus.BASIN_STATUS_TYPES.FLOOD ) ) {
            log.info("I/O PLan failed because current basin is in Flood alarm: " + this.damn.toString() );
            setEndState(Plan.EndState.FAILED);
        }  else {

 */
            float percentageToAchieve = 0.8f;
            float desiredVolume = this.damn.getMax_capacity() * percentageToAchieve;
            boolean isSafe = true;
            float desiredOutflow = 0;

            if ( this.damn.getCurrent_volume() > desiredVolume ) {
                //Intento quitarme algo más de agua para acercarme al 80%
                DataExportService.appendLineToActions(this.basin_id +  ": Current volume " + this.damn.getCurrent_volume() + " trying to reach 80%");

                desiredOutflow = this.currentInflow + (( this.damn.getCurrent_volume() - desiredVolume  ) * 1000000);
                isSafe = askFLowGauges( desiredOutflow, this.damn.getBasin_id() );
                if ( !isSafe ) {
                    //pido el 0.9
                    percentageToAchieve = 0.9f;
                    desiredVolume = this.damn.getMax_capacity() * percentageToAchieve;

                    DataExportService.appendLineToActions(this.basin_id +  ": Current volume " + this.damn.getCurrent_volume() + " trying to reach 90%");
                    desiredOutflow = this.currentInflow + (( this.damn.getCurrent_volume() - desiredVolume  ) * 1000000);
                    isSafe = askFLowGauges( desiredOutflow, this.damn.getBasin_id() );

                    if ( !isSafe ) {
                        //pido el 0.95
                        percentageToAchieve = 0.95f;
                        desiredVolume = this.damn.getMax_capacity() * percentageToAchieve;

                        DataExportService.appendLineToActions(this.basin_id +  ": Current volume " + this.damn.getCurrent_volume() + " trying to reach 95%");
                        desiredOutflow = this.currentInflow + (( this.damn.getCurrent_volume() - desiredVolume  ) * 1000000);
                        isSafe = askFLowGauges( desiredOutflow, this.damn.getBasin_id() );
                    }
                }

            }

             if ( !isSafe ) {
                desiredOutflow = currentInflow;

                DataExportService.appendLineToActions(this.basin_id +  ": Current volume " + this.damn.getCurrent_volume() + " trying to reach just incoming flow");
                isSafe = askFLowGauges( currentInflow, this.damn.getBasin_id() );
            }


            //OUTPUT estado siguiente => Intento quedarme siempre con el 80% del volumen total
            float volumeToStore = desiredOutflow * 1e-6f; //m³/h * 1h = m³ * 1e-6 hm³ / m³ = hm³

            if (  isSafe ) {
                DataExportService.appendLineToValues(time_step + ";" + this.basin_id + ";Release;" + volumeToStore + ";" + this.damn.getMax_capacity() + ";" + this.damn.getCurrent_volume());

                ReleaseWaterGoal releaseWaterGoal = ( ReleaseWaterGoal ) this.getGoal();
                releaseWaterGoal.setOutflow( desiredOutflow );

                //OUTPUT outflow => Inflow + lo que he sido capaz de quedarme
                this.getBeliefBase().updateBelief( BeliefNames.BASIN_OUTFLOW, desiredOutflow);

                //OUTPUT presa => agua almacenada
                float currentVolume = this.damn.getCurrent_volume() - volumeToStore;
                this.damn.setCurrent_volume( currentVolume );
                this.getBeliefBase().updateBelief( BeliefNames.DAMN_STATUS, this.damn );

                //OUTPUT estado => si mi VOL actual es == 80 puedo volver a almacenar
                if (  this.damn.getCurrent_volume() <= (this.damn.getMax_capacity() * 0.8) ) {
                    this.getBeliefBase().updateBelief( BeliefNames.BASIN_STATUS, BasinStatus.BASIN_STATUS_TYPES.NORMAL);
                }

                //OUTPUT presa => no cambia su estado ya que no almaceno nada
                setEndState(Plan.EndState.SUCCESSFUL);
            } else {
                //Voy a producir una inundación tengo que mirar si me puedo quedar yo con algo de agua
                DataExportService.appendLineToValues(time_step + ";" + this.basin_id + ";Release;" + volumeToStore + ";" + this.damn.getMax_capacity() + ";" + this.damn.getCurrent_volume());

                setEndState(Plan.EndState.FAILED);
            }
        //}
    }

    private boolean askFLowGauges(float outflow, String basinId) {
        //Miro si tengo un gauge asociado
        if ( !BasinDefinition.GAUGES.containsKey( basinId)) {
            return true;
        }

        FlowGaugeConfig flowGaugeConfig = BasinDefinition.GAUGES.get( basinId );
        log.info("\t\t[IOPlan] [" + this.damn.getBasin_id() + "] Asking status to Gauge  " + flowGaugeConfig.sensor_id);

        // Enviar mensaje al otro agente
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID( flowGaugeConfig.sensor_id , AID.ISLOCALNAME));
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

    @Parameter(direction = Parameter.Direction.IN, mandatory = true)
    public void setBasin_id(String basin_id) {
        this.basin_id = basin_id;
    }

    @Parameter(direction = Parameter.Direction.IN, mandatory = true)
    public void setTime_step(int time_step) {
        this.time_step = time_step;
    }

}
