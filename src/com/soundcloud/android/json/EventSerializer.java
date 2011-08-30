package com.soundcloud.android.json;

import com.soundcloud.android.model.Event;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerFactory;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.BeanSerializerFactory;
import org.codehaus.jackson.map.ser.CustomSerializerFactory;

import java.io.IOException;

public class EventSerializer extends JsonSerializer<Event> {
    private static final SerializerFactory FACTORY = new CustomSerializerFactory();

    @Override
    public void serialize(Event event, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();

        jgen.writeFieldName("type");
        jgen.writeString(event.type);

        jgen.writeFieldName("created_at");
        provider.defaultSerializeDateValue(event.created_at, jgen);

        jgen.writeFieldName("origin");
        provider.serializeValue(provider.getConfig(), jgen, event.origin, FACTORY);

        jgen.writeFieldName("tags");
        jgen.writeString(event.tags);

        jgen.writeEndObject();
    }
}
