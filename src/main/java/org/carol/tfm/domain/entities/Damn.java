package org.carol.tfm.domain.entities;

import java.io.Serializable;

public class Damn implements Serializable {

    private final String basin_id;
    private float max_capacity;
    private float current_volume = 0;

    public Damn(String basin_id, float max_capacity, float current_volume) {
        this.basin_id = basin_id;
        this.max_capacity = max_capacity;
        this.current_volume = current_volume;
    }

    public String getBasin_id() {
        return basin_id;
    }

    public float getMax_capacity() {
        return max_capacity;
    }

    public void setMax_capacity(float max_capacity) {
        this.max_capacity = max_capacity;
    }

    public float getCurrent_volume() {
        return current_volume;
    }

    public void setCurrent_volume(float current_volume) {
        this.current_volume = current_volume;
    }

    @Override
    public String toString() {
        return "Damn{" +
                "basin_id='" + basin_id + '\'' +
                ", max_capacity=" + max_capacity +
                ", current_volume=" + current_volume +
                '}';
    }
}
