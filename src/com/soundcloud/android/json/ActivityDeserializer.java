package com.soundcloud.android.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Activity;

import java.io.IOException;

public class ActivityDeserializer extends JsonDeserializer<Activity> {
    // need private instance here - non-re-entrant mapper
    static final ObjectMapper mapper = AndroidCloudAPI.Wrapper.createMapper();
    @Override
    public Activity deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = mapper.readValue(parser, JsonNode.class);
        Activity e = new Activity();
        e.type = Activity.Type.fromString(node.get("type").asText());
        e.created_at = context.parseDate(node.get("created_at").asText());
        e.tags = node.get("tags").asText();
        e.origin = mapper.readValue(node.path("origin").traverse(), e.type.typeClass);
        return e;
    }
}
