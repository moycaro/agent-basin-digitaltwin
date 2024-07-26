package org.carol.tfm.external_planners.domain.entities;

public class DamnManagement {
    private String basin_id;
    private float vol_to_be_storaged;

    public DamnManagement(String basin_id, float vol_to_be_storaged) {
        this.basin_id = basin_id;
        this.vol_to_be_storaged = vol_to_be_storaged;
    }

    public String getBasin_id() {
        return basin_id;
    }

    public void setBasin_id(String basin_id) {
        this.basin_id = basin_id;
    }

    public float getVol_to_be_storaged() {
        return vol_to_be_storaged;
    }

    public void setVol_to_be_storaged(float vol_to_be_storaged) {
        this.vol_to_be_storaged = vol_to_be_storaged;
    }
}
