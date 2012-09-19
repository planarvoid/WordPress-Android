package com.soundcloud.android.task;

import static com.soundcloud.android.Expect.expect;

import com.fasterxml.jackson.databind.ObjectReader;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RunWith(DefaultTestRunner.class)
public class AsyncApiTaskTest {
    private List<String> parse(String input) throws IOException {
        ObjectReader reader = DefaultTestRunner.application.getMapper().reader();
        return AsyncApiTask.parseError(reader, new ByteArrayInputStream(input.getBytes()));
    }

    @Test
    public void shouldParseDifferentErrorMessageFormats1() throws Exception {
        expect(parse("{\"errors\":{\"error\":[\"Email has already been taken\",\"Email is already taken.\"]}}"))
                .toEqual(Arrays.asList("Email has already been taken", "Email is already taken."));
    }

    @Test
    public void shouldParseDifferentErrorMessageFormats2() throws Exception {
        expect(parse("{\"errors\":{\"error\":\"Username has already been taken\"}}"))
                .toEqual(Arrays.asList("Username has already been taken"));
    }

    @Test
    public void shouldParseDifferentErrorMessageFormats3() throws Exception {
        expect(parse("{\"error\":\"Unknown Email Address\"}"))
                .toEqual(Arrays.asList("Unknown Email Address"));
    }

    @Test
    public void shouldParseDifferentErrorMessageFormats4() throws Exception {
        expect(parse("{\"errors\":[{\"error_message\":\"Username is too short (minimum is 3 characters)\"}]}"))
                .toEqual(Arrays.asList("Username is too short (minimum is 3 characters)"));
    }

    @Test
    public void shouldIgnoreMalformedJSON() throws Exception {
        expect(parse("failz").isEmpty()).toBeTrue();
    }
}
