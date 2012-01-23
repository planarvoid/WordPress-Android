package com.soundcloud.android.json;

import android.util.Log;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Activity;
import com.soundcloud.android.model.User;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

public class UserDeserializer extends JsonDeserializer<User> {
    @Override
    public User deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return SoundCloudApplication.USER_CACHE.assertCached(AndroidCloudAPI.DefaultMapper.readValue(parser, User.class));
    }
}
