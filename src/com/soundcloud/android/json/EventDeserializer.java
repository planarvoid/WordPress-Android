package com.soundcloud.android.json;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Favoriting;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackSharing;
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
    public Event deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode jsonNode = mapper.readValue(jsonParser, JsonNode.class);
        Event e = mapper.readValue(jsonNode, Event.class);

        if (e.isTrack()) {
            e.origin = mapper.readValue(jsonNode.path("origin"), Track.class);
        } else if (e.isTrackSharing()) {
            e.origin = mapper.readValue(jsonNode.path("origin"), TrackSharing.class);
        } else if (e.isComment()) {
            e.origin = mapper.readValue(jsonNode.path("origin"), Comment.class);
        } else if (e.isFavoriting()) {
            e.origin = mapper.readValue(jsonNode.path("origin"), Favoriting.class);
        }
        return e;
    }
}
