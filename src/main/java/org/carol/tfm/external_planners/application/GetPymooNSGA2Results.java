package org.carol.tfm.external_planners.application;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.carol.tfm.Main;
import org.carol.tfm.domain.capabilities.basic_water_manager.beliefs.BasinInflow;
import org.carol.tfm.domain.capabilities.timestep_management.plans.WaitForExecutionsPlanBody;
import org.carol.tfm.domain.entities.Damn;
import org.carol.tfm.domain.ontology.BasinDefinition;
import org.carol.tfm.domain.ontology.configs.BasinConfig;
import org.carol.tfm.external_planners.domain.PythonCaller;
import org.carol.tfm.external_planners.domain.entities.DamnManagement;
import org.carol.tfm.external_planners.domain.entities.ExternalPlannerResponse;

import java.io.*;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GetPymooNSGA2Results {
    private static final Log log = LogFactory.getLog(GetPymooNSGA2Results.class);

    public Optional<ExternalPlannerResponse> execute(List<BasinInflow> basinInflow, List<Damn> damnList) {
        try {
            final String python_script = "template_reservoir_planner_pymoo.py";
            File tempFile = File.createTempFile("tfm-", "py");
            URL pythonFileUrl =  Main.class.getResource("python/"  + python_script);

            BufferedReader br = new BufferedReader(new FileReader(pythonFileUrl.getFile()));
            PrintWriter pw =  new PrintWriter(new FileWriter(tempFile));
            String line = "";
            while ((line = br.readLine()) != null) {
                pw.println( addDataToLine(line, basinInflow, damnList) );
                pw.flush();
            }

            PythonCaller caller = new PythonCaller();
            final Optional<String> response =  caller.callAndWait( tempFile.getAbsolutePath() );
            ExternalPlannerResponse damnManagement = new ExternalPlannerResponse();

            if (response.isEmpty()) {
                return Optional.empty();
            }

            List<DamnManagement> damnManagementList = getDamnManagementFromResponse( response.get() );
            damnManagement.setDamnManagementList( damnManagementList );

            return Optional.of( damnManagement );

        } catch (Exception ex) {
            log.error("Cano not Execute python script", ex);
        }

        return Optional.empty();
    }

    private String addDataToLine(String line, List<BasinInflow> basinInflows, List<Damn> damnStatus) {
        if ( line.contains("%C1_INFLOW%")) {
            return line.replace("%C1_INFLOW%", String.valueOf( basinInflows.stream().filter( b -> b.getBasin_id().equals("c1")).findAny().get().getMm() ) );
        }

        if ( line.contains("%C2_INFLOW%")) {
            return line.replace("%C2_INFLOW%", String.valueOf( basinInflows.stream().filter( b -> b.getBasin_id().equals("c2")).findAny().get().getMm() ) );
        }

        if ( line.contains("%C3_INFLOW%")) {
            return line.replace("%C3_INFLOW%", String.valueOf( basinInflows.stream().filter( b -> b.getBasin_id().equals("c3")).findAny().get().getMm() ) );
        }

        if ( line.contains("%C3_THRESHOLD%")) {
            return line.replace("%C3_THRESHOLD%", String.valueOf( BasinDefinition.GAUGES.get("c3").flow_threshold_info ) );
        }

        if ( line.contains("%C4_THRESHOLD%")) {
            return line.replace("%C4_THRESHOLD%", String.valueOf( BasinDefinition.GAUGES.get("c4").flow_threshold_info ) );
        }

        if ( line.contains("%C1_MAX_CAPACITY%")) {
            return line.replace("%C1_MAX_CAPACITY%", String.valueOf( BasinDefinition.RESERVOIRS.get("c1").volumen_total ) );
        }

        if ( line.contains("%C2_MAX_CAPACITY%")) {
            return line.replace("%C2_MAX_CAPACITY%", String.valueOf( BasinDefinition.RESERVOIRS.get("c2").volumen_total ) );
        }

        if ( line.contains("%C3_MAX_CAPACITY%")) {
            return line.replace("%C3_MAX_CAPACITY%", String.valueOf( BasinDefinition.RESERVOIRS.get("c3").volumen_total ) );
        }

        if ( line.contains("%C1_CURRENT_VOL%")) {
            return line.replace("%C1_CURRENT_VOL%", String.valueOf( damnStatus.stream().filter( b -> b.getBasin_id().equals("c1")).findAny().get().getCurrent_volume() ) );
        }

        if ( line.contains("%C2_CURRENT_VOL%")) {
            return line.replace("%C2_CURRENT_VOL%", String.valueOf( damnStatus.stream().filter( b -> b.getBasin_id().equals("c2")).findAny().get().getCurrent_volume() ) );
        }

        if ( line.contains("%C3_CURRENT_VOL%")) {
            return line.replace("%C3_CURRENT_VOL%", String.valueOf( damnStatus.stream().filter( b -> b.getBasin_id().equals("c3")).findAny().get().getCurrent_volume() ) );
        }

        return line;
    }

    private List<DamnManagement> getDamnManagementFromResponse(String response) {
        //Look for PREFIX X
        final String X_SOLUTION_PREFIX = "X-solution <==>";
        final String F_SOLUTION_PREFIX = "F-solution <==>";
        final String G_SOLUTION_PREFIX = "G-solution <==>";

        int indexXSolution =  response.indexOf( X_SOLUTION_PREFIX );
        int indexFSolution =  response.indexOf( F_SOLUTION_PREFIX );
        int indexGSolution =  response.indexOf( G_SOLUTION_PREFIX );

        String x_response = response.substring( indexXSolution + X_SOLUTION_PREFIX.length() , indexFSolution ).trim();
        String f_response = response.substring( indexFSolution + F_SOLUTION_PREFIX.length() ).trim();
        String g_response = response.substring( indexGSolution + G_SOLUTION_PREFIX.length() ).trim();

        // Son arrays de arrays [[][][][]] quito los primeros []
        x_response = x_response.substring( 1, x_response.length() -1 );
        f_response = f_response.substring( 1, f_response.length() -1 );
        g_response = g_response.substring( 1, g_response.length() -1 );

        // De las posibles soluciones me quedo con la que tenga el menor valor de sobrepasar el umbral en C4 ya que es donde tengo la población
        int responseSelectedIndex = getBestSolution( g_response );
        if ( responseSelectedIndex < 0 ) {
            //No soy capaz de encontrar una solución
            log.error("Pymoo NSGA2 is not able to find a suitable solution");
            return new ArrayList<>();
        }

        return readSolution(x_response, responseSelectedIndex);
    }

    private List<DamnManagement> readSolution(String xResponse, int responseSelectedIndex) {
        String[] responses = xResponse.split("]");
        String response = responses[responseSelectedIndex].replaceAll("\n", "").replaceAll("\\[", "").trim();
        String[] solutionValues = response.split(" ");

        List<DamnManagement> ret = new ArrayList<>();

        for (int i = 0; i < solutionValues.length; i++) {
            ret.add( new DamnManagement( BasinDefinition.BASINS.get(i), Float.parseFloat( solutionValues[i])) );
        }

        return ret;
    }

    private int getBestSolution(String gResponse) {
        String[] responses = gResponse.split("]");
        float trespassingC4Threshold = -1 * Float.MAX_VALUE;
        int index = -1;

        for (int i = 0; i < responses.length; i++ ) {
            String[] constraintValues = responses[i].split(" ", -1);
            //last constraint is referred to last hydro gauge
            try {
                float v = Float.parseFloat( constraintValues[ constraintValues.length -1 ] );
                if ( v > trespassingC4Threshold ) {
                    index = i;
                    trespassingC4Threshold = v;
                }
            } catch ( Exception ex) {
            }
        }

        return index;
    }
}
