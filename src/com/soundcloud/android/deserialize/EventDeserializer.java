package com.soundcloud.android.deserialize;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.Comment;
import com.soundcloud.android.model.Event;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.ObjectMapper;

import android.util.Log;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

public class EventDeserializer extends JsonDeserializer {
    @Override
    public Event deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper() {
            {
                getDeserializationConfig().setDateFormat(AndroidCloudAPI.CloudDateFormat.INSTANCE);
            }
        };
        JsonNode jsonNode = mapper.readValue(jsonParser, JsonNode.class);
        Event e = mapper.readValue(jsonNode, Event.class);

        if (e.type.contentEquals(Event.Types.TRACK)) {
            e.track = mapper.readValue(jsonNode.path("origin"), Track.class);
            e.origin_id = e.track.id;
        } else if (e.type.contentEquals(Event.Types.TRACK_SHARING)) {
            e.track = mapper.readValue(jsonNode.path("origin").path("track"), Track.class);
            e.origin_id = e.track.id;
        } else if (e.type.contentEquals(Event.Types.COMMENT)) {
            e.comment = mapper.readValue(jsonNode.path("origin"), Comment.class);
            e.origin_id = e.comment.id;
        } else if (e.type.contentEquals(Event.Types.FAVORITING)) {
            e.track = mapper.readValue(jsonNode.path("origin").path("track"), Track.class);
            e.user = mapper.readValue(jsonNode.path("origin").path("user"), User.class);
            e.origin_id = e.track.id;
        }
        return e;
    }
}
