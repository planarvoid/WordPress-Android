package com.soundcloud.android.sync.content;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;

public class FollowErrorsTest {

    JsonTransformer jsonTransformer;

    @Before
    public void setUp() throws Exception {
        jsonTransformer = new JacksonJsonTransformer();
    }

    @Test
    public void shouldBeAgeRestrictedForAgeRestrictedErrorMessage() throws Exception {
        String message = "{\"errors\":[{\"error_message\":\"DENY_AGE_RESTRICTED\",\"age\":21}]}";
        FollowErrors errors = jsonTransformer.fromJson(message, TypeToken.of(FollowErrors.class));
        expect(errors.isAgeRestricted()).toBeTrue();
        expect(errors.isAgeUnknown()).toBeFalse();
        expect(errors.getAge()).toBe(21);
    }

    @Test
    public void shouldBeAgeUknownForAgeUnknownErrorMessage() throws Exception {
        String message = "{\"errors\":[{\"error_message\":\"DENY_AGE_UNKNOWN\"}]}";
        FollowErrors errors = jsonTransformer.fromJson(message, TypeToken.of(FollowErrors.class));
        expect(errors.isAgeRestricted()).toBeFalse();
        expect(errors.isAgeUnknown()).toBeTrue();
        expect(errors.getAge()).toBeNull();
    }

    @Test
    public void shouldNotBeAgeRestrictedOrUnknownForAnyOtherErrorMessage() throws Exception {
        String message = "{\"errors\":[{\"error_message\":\"Forbidden\"}]}";
        FollowErrors errors = jsonTransformer.fromJson(message, TypeToken.of(FollowErrors.class));
        expect(errors.isAgeRestricted()).toBeFalse();
        expect(errors.isAgeUnknown()).toBeFalse();
        expect(errors.getAge()).toBeNull();
    }
}
