package com.soundcloud.android.json;

import com.soundcloud.android.model.Activity;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerFactory;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.CustomSerializerFactory;
import org.codehaus.jackson.map.ser.ScalarSerializerBase;

import java.io.IOException;

public class ActivitySerializer extends JsonSerializer<Activity> {
    private static final SerializerFactory FACTORY = new CustomSerializerFactory() {
        {
            final JsonSerializer<Float> floatSerializer = new ScalarSerializerBase<Float>(Float.class) {
                @Override
                public void serialize(Float value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
                    // assume 0 == null for now
                    if (value == null || value == 0f) {
                        jgen.writeNull();
                    } else {
                        jgen.writeNumber(value);
                    }
                }
            };

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
            _concrete.put(Float.class.getName(), floatSerializer);
            _concrete.put(Float.TYPE.getName(), floatSerializer);

        }
    };


    @Override
    public void serialize(Activity activity, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeStartObject();

        jgen.writeFieldName("type");
        jgen.writeString(activity.type);

        jgen.writeFieldName("created_at");
        provider.defaultSerializeDateValue(activity.created_at, jgen);

        jgen.writeFieldName("origin");
        provider.serializeValue(provider.getConfig().withView(activity.getView(provider.getSerializationView())),
                jgen, activity.origin, FACTORY);

        jgen.writeFieldName("tags");
        jgen.writeString(activity.tags);

        jgen.writeEndObject();
    }
}
