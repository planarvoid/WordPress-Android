package com.soundcloud.android.api.http.json;

import static com.soundcloud.android.Expect.expect;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.model.User;
import org.junit.Before;
import org.junit.Test;

public class JackonJsonTransformerTest {

    private JacksonJsonTransformer jsonTransformer;

    @Before
    public void setUp() throws Exception {
        jsonTransformer = new JacksonJsonTransformer();
    }

    @Test
    public void shouldParseUserObject() throws Exception {
        User user = jsonTransformer.fromJson("{\"id\" : 22, \"kind\" : \"user\"}", TypeToken.of(User.class));
        expect(user.getId()).toBe(22L);
    }

    @Test
    public void shouldParseSuggestedUserObject() throws Exception {
        SuggestedUser user = jsonTransformer.fromJson("{\"id\" : 11, \"username\" : \"lolwat\",\"city\" : \"berlin\" }", TypeToken.of(SuggestedUser.class));
        expect(user.getId()).toBe(11L);
        expect(user.getUsername()).toEqual("lolwat");
        expect(user.getCity()).toEqual("berlin");
    }

}
