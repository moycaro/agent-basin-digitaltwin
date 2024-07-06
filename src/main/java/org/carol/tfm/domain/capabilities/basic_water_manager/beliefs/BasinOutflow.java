package org.carol.tfm.domain.capabilities.basic_water_manager.beliefs;

public class BasinOutflow {
    private String basin_id;
    private float mm;

    public BasinOutflow(String basin_id, float mm) {
        this.basin_id = basin_id;
        this.mm = mm;
    }

    public BasinInflow changeValue(float newTimestepMM ) {
        return new BasinInflow( this.basin_id,  newTimestepMM);
    }

    public String getBasin_id() {
        return basin_id;
    }

    public void setBasin_id(String basin_id) {
        this.basin_id = basin_id;
    }

    public float getMm() {
        return mm;
    }

    public void setMm(float mm) {
        this.mm = mm;
    }
}
