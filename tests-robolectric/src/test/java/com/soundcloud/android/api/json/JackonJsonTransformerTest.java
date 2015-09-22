package com.soundcloud.android.api.json;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class JackonJsonTransformerTest {

    private JacksonJsonTransformer jsonTransformer;

    @Before
    public void setUp() throws Exception {
        jsonTransformer = new JacksonJsonTransformer();
    }

    @Test
    public void shouldParseUserObject() throws Exception {
        PublicApiUser user = jsonTransformer.fromJson("{\"id\" : 22, \"kind\" : \"user\"}", TypeToken.of(PublicApiUser.class));
        expect(user.getId()).toBe(22L);
    }

}
