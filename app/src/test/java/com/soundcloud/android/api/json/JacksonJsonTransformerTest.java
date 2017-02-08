package com.soundcloud.android.api.json;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.java.optional.Optional;
import org.junit.Test;

public class JacksonJsonTransformerTest {

    private ObjectMapper objectMapper = JacksonJsonTransformer.buildObjectMapper();

    private static class WithOptional {
        public Optional<String> optional;

        WithOptional(@JsonProperty("optional") Optional<String> optional) {
            this.optional = optional;
        }
    }

    @Test
    public void deserializePresentOptional() throws Exception {
        WithOptional withOptional = objectMapper.readValue("{\"optional\":\"bar\"}", WithOptional.class);
        assertThat(withOptional.optional.isPresent()).isTrue();
        assertThat(withOptional.optional.get()).isEqualTo("bar");
    }

    @Test
    public void deserializeAbsentOptionalFromNull() throws Exception {
        WithOptional withOptional = objectMapper.readValue("{\"optional\":null}", WithOptional.class);
        assertThat(withOptional.optional.isPresent()).isFalse();
    }

    @Test
    public void deserializeAbsentOptionalFromMissing() throws Exception {
        WithOptional withOptional = objectMapper.readValue("{}", WithOptional.class);
        assertThat(withOptional.optional.isPresent()).isFalse();
    }

    @Test
    public void serializePresentOptional() throws Exception {
        WithOptional withOptional = new WithOptional(Optional.of("bar"));
        String json = objectMapper.writeValueAsString(withOptional);
        assertThat(json).isEqualTo("{\"optional\":\"bar\"}");
    }

    @Test
    public void serializeAbsentOptional() throws Exception {
        WithOptional withOptional = new WithOptional(Optional.absent());
        String json = objectMapper.writeValueAsString(withOptional);
        assertThat(json).isEqualTo("{\"optional\":null}");
    }
}
