package org.carol.tfm.domain.ontology.configs;

public class FlowGaugeConfig {

    public String sensor_id;
    public float info;
    public float pre_alarm;
    public float alarm;

    public FlowGaugeConfig(String sensor_id, float info, float pre_alarm, float alarm) {
        this.sensor_id = sensor_id;
        this.info = info;
        this.pre_alarm = pre_alarm;
        this.alarm = alarm;
    }
}
