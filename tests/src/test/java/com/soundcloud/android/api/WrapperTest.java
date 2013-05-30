package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;

public class WrapperTest {


    @Test
    public void shouldDeobfuscateClientSecret() throws Exception {
        // live
        expect(Wrapper.getClientSecret(true))
                .toEqual("26a5240f7ee0ee2d4fa9956ed80616c2");

        // sandbox
        expect(Wrapper.getClientSecret(false))
                .toEqual("0000000pGDzQNAPHzBH6hBTHphl4Q1e9");
    }
}
