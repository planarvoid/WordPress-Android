package com.soundcloud.android.json;

import com.soundcloud.android.model.Event;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerFactory;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.CustomSerializerFactory;
import org.codehaus.jackson.map.ser.ScalarSerializerBase;

import java.io.IOException;

public class EventSerializer extends JsonSerializer<Event> {
    private static final SerializerFactory FACTORY = new CustomSerializerFactory() {
        {
            final JsonSerializer<Long> longSerializer = new ScalarSerializerBase<Long>(Long.class) {
                @Override
                public void serialize(Long value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                    // assume 0 == null for now
                    if (value == null || value == 0) {
                        jgen.writeNull();
                    } else {
                        jgen.writeNumber(value);
                    }
                }
            };
            _concrete.put(Long.class.getName(), longSerializer);
            _concrete.put(Long.TYPE.getName(), longSerializer);
        }
    };


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
