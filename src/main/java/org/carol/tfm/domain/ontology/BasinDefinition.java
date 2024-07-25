package org.carol.tfm.domain.ontology;

import org.carol.tfm.domain.ontology.configs.BasinConfig;
import org.carol.tfm.domain.ontology.configs.FlowGaugeConfig;
import org.carol.tfm.domain.ontology.configs.PluviometerConfig;
import org.carol.tfm.domain.ontology.configs.ReservoirConfig;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

public class BasinDefinition {
    public static final LocalDate INITIAL_DATE = LocalDate.of(2008, 1, 1 );

    public static final List<String> BASINS = List.of("c1", "c2", "c3", "c4");

    public static final Map<String, BasinConfig> BASINS_CONFIG = Map.ofEntries(
            entry("c1", new BasinConfig("c1", 181.9f, true, false, "c3") ),
            entry("c2", new BasinConfig( "c2",244.3f, true, false, "c3" ) ),
            entry("c3", new BasinConfig( "c3",146.9f, false, false, "c4") ),
            entry("c4", new BasinConfig( "c4",390.6f, false, true, null) )
    );

    public static final Map<String, PluviometerConfig> PLUVIOS = Map.ofEntries(
            entry("c1", new PluviometerConfig( "SPE00156270") ),
            entry("c2", new PluviometerConfig( "SPE00156018") ),
            entry("c3", new PluviometerConfig( "SPE00156153") )
    );

    public static final Map<String, FlowGaugeConfig> GAUGES = Map.ofEntries(
            entry("c3", new FlowGaugeConfig("A137", 120f, Float.NaN, Float.NaN ) ),
            entry("c4", new FlowGaugeConfig("A115", 130f, 210f, Float.NaN ) )
    );

    public static final Map<String, ReservoirConfig> RESERVOIRS = Map.ofEntries(
            entry("c1", new ReservoirConfig( "E065", 1360, 1434.5f, 21.793f) ),
            entry("c2", new ReservoirConfig( "E063", 1710f, 817.5f, 16.046f) ),
            entry("c3", new ReservoirConfig( "E050", 715f, 817.5f, 152.317f) )
    );
}
