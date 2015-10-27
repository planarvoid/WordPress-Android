package com.soundcloud.android.sync.affiliations;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.json.JacksonJsonTransformer;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;

public class FollowErrorsTest {

    private JsonTransformer jsonTransformer;

    @Before
    public void setUp() throws Exception {
        jsonTransformer = new JacksonJsonTransformer();
    }

    @Test
    public void shouldBeAgeRestrictedForAgeRestrictedErrorMessage() throws Exception {
        String message = "{\"errors\":[{\"error_message\":\"DENY_AGE_RESTRICTED\",\"age\":21}]}";
        FollowErrors errors = jsonTransformer.fromJson(message, TypeToken.of(FollowErrors.class));
        assertThat(errors.isAgeRestricted()).isTrue();
        assertThat(errors.isAgeUnknown()).isFalse();
        assertThat(errors.getAge()).isEqualTo(21);
    }

    @Test
    public void shouldBeAgeUknownForAgeUnknownErrorMessage() throws Exception {
        String message = "{\"errors\":[{\"error_message\":\"DENY_AGE_UNKNOWN\"}]}";
        FollowErrors errors = jsonTransformer.fromJson(message, TypeToken.of(FollowErrors.class));
        assertThat(errors.isAgeRestricted()).isFalse();
        assertThat(errors.isAgeUnknown()).isTrue();
        assertThat(errors.getAge()).isNull();
    }

    @Test
    public void shouldNotBeAgeRestrictedOrUnknownForAnyOtherErrorMessage() throws Exception {
        String message = "{\"errors\":[{\"error_message\":\"Forbidden\"}]}";
        FollowErrors errors = jsonTransformer.fromJson(message, TypeToken.of(FollowErrors.class));
        assertThat(errors.isAgeRestricted()).isFalse();
        assertThat(errors.isAgeUnknown()).isFalse();
        assertThat(errors.getAge()).isNull();
    }
}
