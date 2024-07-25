package org.carol.tfm.domain.ontology.configs;

public class BasinConfig {
    public float area;
    public boolean is_header = false;
    public boolean is_last = false;
    public String next_basin_id;
    public String basin_id;

    public BasinConfig(String basin_id, float area, boolean is_header, boolean is_last, String next_basin_id) {
        this.area = area;
        this.is_header = is_header;
        this.is_last = is_last;
        this.next_basin_id = next_basin_id;
        this.basin_id = basin_id;
    }
}
