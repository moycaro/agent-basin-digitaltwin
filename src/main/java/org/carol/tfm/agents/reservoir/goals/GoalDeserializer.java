package org.carol.tfm.agents.reservoir.goals;

import bdi4jade.goal.Goal;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.carol.tfm.domain.entities.Damn;

import java.io.IOException;

public class GoalDeserializer extends StdDeserializer<Goal> {

    public GoalDeserializer() {
        this(null);
    }

    public GoalDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Goal deserialize(JsonParser parser, DeserializationContext deserializer) throws IOException, JacksonException {
        ObjectCodec codec = parser.getCodec();
        JsonNode node = codec.readTree(parser);

        String goalName = node.get("name").asText();
        int timeStep = node.get("time_step").asInt();
        Double currentInflow = node.get("currentInflow").asDouble();

        Goal ret = null;
        if (goalName.equals( ReleaseWaterGoal.class.getName() ) ) {
            ret = new ReleaseWaterGoal( deserializeDamn(node.get("damn") ) , currentInflow.floatValue() , timeStep);
        } else if (goalName.equals( StorageWaterGoal.class.getName() ) ) {
            ret = new StorageWaterGoal( deserializeDamn(node.get("damn") ), currentInflow.floatValue() , timeStep);
        }

        return ret;
    }

    private Damn deserializeDamn(JsonNode damnNode) {
        String basin_id = damnNode.get("basin_id").asText();
        Double max_capacity = damnNode.get("max_capacity").asDouble();
        Double current_volume = damnNode.get("current_volume").asDouble();

        return new Damn( basin_id, max_capacity.floatValue(), current_volume.floatValue());
    }
}
