package com.soundcloud.android.api.json;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.NotNull;

import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * Deserializes possibly null JSON values as {@link Optional Optionals}. Null values are deserialized as absent Optionals.
 * Non-null values are deserialized as present Optionals using the appropriate deserializer for the wrapped type.
 */
class OptionalDeserializer extends JsonDeserializer<Optional<?>> implements ContextualDeserializer {

    /**
     * Null for the instance of this class registered with the {@link ObjectMapper}. The ObjectMapper will use that
     * instance to create type-specific instances by calling {@link #createContextual(DeserializationContext, BeanProperty)}.
     */
    @Nullable private final JavaType valueType;

    /**
     * Used to create a deserializer that will provide type-specific deserializers in
     * {@link #createContextual(DeserializationContext, BeanProperty)}.
     */
    OptionalDeserializer() {
        valueType = null;
    }

    /**
     * Used to create a type-specific deserializer.
     */
    private OptionalDeserializer(@NotNull JavaType valueType) {
        checkNotNull(valueType, "valueType");
        this.valueType = valueType;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        return new OptionalDeserializer(property.getType().containedType(0));
    }

    @Override
    public Optional<?> deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        checkNotNull(valueType, "No value type set.");
        return Optional.of(ctxt.findRootValueDeserializer(valueType).deserialize(parser, ctxt));
    }

    @Override
    public Optional<?> getNullValue() {
        checkNotNull(valueType, "No value type set.");
        return Optional.absent();
    }
}
