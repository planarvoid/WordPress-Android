package com.soundcloud.android.testsupport.fixtures;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class JsonFixtures {

    public static String resourceAsString(Class klazz, String res) throws IOException {
        InputStream is = klazz.getResourceAsStream(res);
        expect(is).not.toBeNull();
        return IOUtils.readInputStream(is);
    }


    public static byte[] resourceAsBytes(Class klazz, String res) throws IOException {
        InputStream is = klazz.getResourceAsStream(res);
        expect(is).not.toBeNull();
        return IOUtils.readInputStreamAsBytes(is);
    }
}
