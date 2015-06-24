package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class JsonFixtures {

    public static String resourceAsString(Class klazz, String res) throws IOException {
        InputStream is = klazz.getResourceAsStream(res);
        if (is == null) {
            throw new AssertionError("Failed finding resource " + res);
        }
        return IOUtils.readInputStream(is);
    }


    public static byte[] resourceAsBytes(Class klazz, String res) throws IOException {
        InputStream is = klazz.getResourceAsStream(res);
        if (is == null) {
            throw new AssertionError("Failed finding resource " + res);
        }
        return IOUtils.readInputStreamAsBytes(is);
    }
}
