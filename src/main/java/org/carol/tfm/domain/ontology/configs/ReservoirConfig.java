package org.carol.tfm.domain.ontology.configs;

public class ReservoirConfig {
    public String sensor_id;
    public float cota_minima;//msnm
    public float cota_aliviadero;//msnm
    public float volumen_total;//hm3

    public ReservoirConfig(String sensor_id, float cota_minima, float cota_aliviadero, float volumen_total) {
        this.sensor_id = sensor_id;
        this.cota_minima = cota_minima;
        this.cota_aliviadero = cota_aliviadero;
        this.volumen_total = volumen_total;
    }
}
