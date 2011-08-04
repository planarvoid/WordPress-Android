package com.soundcloud.android.deserialize;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

public class EventDeserializer extends JsonDeserializer<Event> {

    static ObjectMapper stdMapper;

    private ObjectMapper getStdMapper() {
        if (stdMapper == null) {
            stdMapper = AndroidCloudAPI.Wrapper.createMapper();
        }
        return stdMapper;
    }

    @Override
    public Event deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {

        final ObjectMapper mapper = getStdMapper();

        JsonNode jsonNode = mapper.readValue(jsonParser, JsonNode.class);
        Event e = mapper.readValue(jsonNode, Event.class);


        if (e.type.contentEquals(Event.Types.TRACK)) {
            e.track = mapper.readValue(jsonNode.path("origin"), Track.class);
            e.user = mapper.readValue(jsonNode.path("origin").path("user"), User.class);
            e.origin_id = e.track.id;
        } else if (e.type.contentEquals(Event.Types.TRACK_SHARING)) {
            e.track = mapper.readValue(jsonNode.path("origin").path("track"), Track.class);
            e.user = e.track.user;
            e.origin_id = e.track.id;
        } else if (e.type.contentEquals(Event.Types.COMMENT)) {
            e.comment = mapper.readValue(jsonNode.path("origin"), Comment.class);
            e.user = e.comment.user;
            e.track = mapper.readValue(jsonNode.path("origin").path("track"), Track.class);
        } else if (e.type.contentEquals(Event.Types.FAVORITING)) {
            e.track = mapper.readValue(jsonNode.path("origin").path("track"), Track.class);
            e.user = mapper.readValue(jsonNode.path("origin").path("user"), User.class);
            e.origin_id = e.track.id;
        }
        return e;
    }
}
