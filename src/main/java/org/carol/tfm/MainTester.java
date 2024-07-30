package org.carol.tfm;

import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinInflow;
import org.carol.tfm.domain.entities.Damn;
import org.carol.tfm.domain.ontology.BasinDefinition;
import org.carol.tfm.external_planners.application.GetPymooNSGA2Results;
import org.carol.tfm.external_planners.domain.PythonCaller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainTester {
    public static void main(String[] args) throws Exception {
        Map<String, Float> basinInflow = new HashMap<>();
        BasinDefinition.BASINS.forEach( basin_id -> {
            basinInflow.put( basin_id, 11.03f);
        });

        List<Damn> damnList = new ArrayList<>();
        BasinDefinition.BASINS.forEach( basin_id -> {
            Damn damn = new Damn(basin_id, 0f, 0.4f);
            damnList.add( damn );
        });

        GetPymooNSGA2Results caller = new GetPymooNSGA2Results();
        caller.execute( basinInflow, damnList );
    }
}
