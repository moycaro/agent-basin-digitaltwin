package org.carol.tfm.domain.ontology.configs;

public class FlowGaugeConfig {

    public String sensor_id;
    public float flow_threshold_info; //m³/s
    public float flow_threshold_pre_alarm;//m³/s
    public float flow_threshold_alarm;//m³/s

    public FlowGaugeConfig(String sensor_id, float info, float pre_alarm, float alarm) {
        this.sensor_id = sensor_id;
        this.flow_threshold_info = info;
        this.flow_threshold_pre_alarm = pre_alarm;
        this.flow_threshold_alarm = alarm;
    }
}
