package com.soundcloud.android.cast.api.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.soundcloud.android.cast.RemoteTrack;

import java.io.IOException;

/**
 * Serializes {@link RemoteTrack RemoteTracks} to JSON. RemoteTracks are referenced by IDs
 * on the receiver side, so we do not need to serialize their URN if the ID is present.
 */
public class RemoteTrackSerializer extends JsonSerializer<RemoteTrack> {

    static final String ID_FIELD_NAME = "id";
    static final String URN_FIELD_NAME = "urn";

    @Override
    public void serialize(RemoteTrack remoteTrack, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        if (remoteTrack.id().isPresent()) {
            gen.writeStringField(ID_FIELD_NAME, remoteTrack.id().get());
        } else {
            gen.writeStringField(URN_FIELD_NAME, remoteTrack.urn().toString());
        }
        gen.writeEndObject();
    }
}
