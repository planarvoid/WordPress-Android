package com.soundcloud.android.json;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Event;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

public class EventDeserializer extends JsonDeserializer<Event> {
    // need private instance here - non-re-entrant mapper
    static final ObjectMapper mapper = AndroidCloudAPI.Wrapper.createMapper();
    @Override
    public Event deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = mapper.readValue(parser, JsonNode.class);
        Event e = new Event();
        e.type = node.get("type").getValueAsText();
        e.created_at = context.parseDate(node.get("created_at").getValueAsText());
        e.tags = node.get("tags").getValueAsText();
        e.origin = mapper.readValue(node.path("origin"), e.getOriginClass());
        return e;
    }
}
