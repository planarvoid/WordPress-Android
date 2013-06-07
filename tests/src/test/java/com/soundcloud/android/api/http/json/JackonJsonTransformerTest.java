package com.soundcloud.android.api.http.json;

import static com.soundcloud.android.Expect.expect;

import com.google.common.reflect.TypeToken;
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
    public void shouldParseSingleObject() throws Exception {
        Me user = jsonTransformer.fromJson("{\"id\" : 22}", TypeToken.of(Me.class));
        expect(user.getId()).toBe(22);
    }

    private static class Me{
        private Integer id;

        private Integer getId() {
            return id;
        }

        private void setId(Integer id) {
            this.id = id;
        }
    }
}
