package com.soundcloud.android.task;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.RoboApiBaseTests;
import org.codehaus.jackson.map.ObjectReader;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class AsyncApiTaskTest extends RoboApiBaseTests {
    private List<String> parse(String input) throws IOException {
        ObjectReader reader = api.getMapper().reader();
        return AsyncApiTask.parseError(reader, new ByteArrayInputStream(input.getBytes()));
    }

    @Test
    public void shouldParseDifferentErrorMessageFormats1() throws Exception {
        assertThat(
                parse("{\"errors\":{\"error\":[\"Email has already been taken\",\"Email is already taken.\"]}}"),
                equalTo(Arrays.asList("Email has already been taken", "Email is already taken.")));
    }

    @Test
    public void shouldParseDifferentErrorMessageFormats2() throws Exception {
        assertThat(
                parse("{\"errors\":{\"error\":\"Username has already been taken\"}}"),
                equalTo(Arrays.asList("Username has already been taken")));
    }

    @Test
    public void shouldParseDifferentErrorMessageFormats3() throws Exception {
        assertThat(
                parse("{\"error\":\"Unknown Email Address\"}"),
                equalTo(Arrays.asList("Unknown Email Address")));
    }

    @Test
    public void shouldParseDifferentErrorMessageFormats4() throws Exception {
        assertThat(
                parse("{\"errors\":[{\"error_message\":\"Username is too short (minimum is 3 characters)\"}]}"),
                equalTo(Arrays.asList("Username is too short (minimum is 3 characters)")));
    }
}
