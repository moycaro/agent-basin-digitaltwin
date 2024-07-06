package org.carol.tfm.domain.capabilities.basic_water_manager.beliefs;

public class BasinStatus {
    private String basin_id;
    private BASIN_STATUS_TYPES current_status;

    public BasinStatus(String basin_id, BASIN_STATUS_TYPES current_status) {
        this.basin_id = basin_id;
        this.current_status = current_status;
    }

    public BasinStatus changeStatus( BASIN_STATUS_TYPES current_status ) {
        return new BasinStatus( this.basin_id, current_status);
    }

    public String getBasin_id() {
        return basin_id;
    }

    public void setBasin_id(String basin_id) {
        this.basin_id = basin_id;
    }

    public BASIN_STATUS_TYPES getCurrent_status() {
        return current_status;
    }

    public void setCurrent_status(BASIN_STATUS_TYPES current_status) {
        this.current_status = current_status;
    }

    public static enum BASIN_STATUS_TYPES {FLOOD, NORMAL, SCARCITY}
}
