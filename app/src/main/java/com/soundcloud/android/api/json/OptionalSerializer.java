package com.soundcloud.android.api.json;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.NotNull;

import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * Serializes {@link Optional Optionals} to JSON. Absent Optionals are serialized as null. Present optionals are
 * serialized using the appropriate serializer for the wrapped type.
 */
class OptionalSerializer extends JsonSerializer<Optional> implements ContextualSerializer {

    /**
     * Null for the instance of this class registered with the {@link ObjectMapper}. The ObjectMapper will use that
     * instance to create type-specific instances by calling {@link #createContextual(SerializerProvider, BeanProperty)}.
     */
    @Nullable private final BeanProperty property;

    /**
     * Used to create a serializer that will provide type-specific serializers in
     * {@link #createContextual(SerializerProvider, BeanProperty)}.
     */
    OptionalSerializer() {
        property = null;
    }

    /**
     * Used to create a type-specific serializer.
     */
    private OptionalSerializer(@NotNull BeanProperty property) {
        checkNotNull(property, "valueType");
        this.property = property;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        return new OptionalSerializer(property);
    }

    @Override
    public void serialize(Optional value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        checkNotNull(property, "No property set.");
        if (value.isPresent()) {
            serializers.findValueSerializer(property.getType().containedType(0), property).serialize(value.get(), gen, serializers);
        } else {
            gen.writeNull();
        }
    }
}
