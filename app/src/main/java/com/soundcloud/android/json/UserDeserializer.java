package com.soundcloud.android.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;

import java.io.IOException;

public class UserDeserializer extends JsonDeserializer<User> {
    private final ObjectMapper mapper;

    public UserDeserializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public User deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return SoundCloudApplication.USER_CACHE.assertCached(mapper.readValue(parser, User.class));
    }
}
