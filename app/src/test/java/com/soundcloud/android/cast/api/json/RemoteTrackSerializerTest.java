package com.soundcloud.android.cast.api.json;

import static com.soundcloud.android.cast.api.json.RemoteTrackSerializer.ID_FIELD_NAME;
import static com.soundcloud.android.cast.api.json.RemoteTrackSerializer.URN_FIELD_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.soundcloud.android.cast.RemoteTrack;
import com.soundcloud.android.model.Urn;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class RemoteTrackSerializerTest {

    private Writer jsonWriter;
    private JsonGenerator jsonGenerator;
    private SerializerProvider serializerProvider;
    private RemoteTrackSerializer serializer;

    @Before
    public void setUp() throws IOException {
        jsonWriter = new StringWriter();
        jsonGenerator = new JsonFactory().createGenerator(jsonWriter);
        serializerProvider = new ObjectMapper().getSerializerProvider();

        serializer = new RemoteTrackSerializer();
    }

    @Test
    public void serializeUrnIfIdIsAbsent() throws IOException {
        RemoteTrack remoteTrack = RemoteTrack.create(Urn.forTrack(12345L));

        serializer.serialize(remoteTrack, jsonGenerator, serializerProvider);
        jsonGenerator.flush();

        String expected = "{" +
                "\"" + URN_FIELD_NAME + "\":" + "\"" + remoteTrack.urn() + "\"" +
                "}";
        assertThat(jsonWriter.toString()).isEqualTo(expected);
    }

    @Test
    public void serializeOnlyIdIfPresent() throws IOException {
        String id = "fake-track-id";
        RemoteTrack remoteTrack = RemoteTrack.create(id, Urn.forTrack(12345L));

        serializer.serialize(remoteTrack, jsonGenerator, serializerProvider);
        jsonGenerator.flush();

        String expected = "{" +
                "\"" + ID_FIELD_NAME + "\":" + "\"" + id + "\"" +
                "}";
        assertThat(jsonWriter.toString()).isEqualTo(expected);
    }
}
