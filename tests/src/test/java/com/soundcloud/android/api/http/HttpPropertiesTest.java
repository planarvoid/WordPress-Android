package com.soundcloud.android.api.http;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;

public class HttpPropertiesTest {

    @Test
    public void shouldDeobfuscateClientSecret() throws Exception {
        // live
        expect(new HttpProperties().getClientSecret())
                .toEqual("26a5240f7ee0ee2d4fa9956ed80616c2");

    }
}
