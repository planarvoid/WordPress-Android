package com.soundcloud.android.robolectric.shadows;

import com.soundcloud.java.strings.Charsets;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import org.apache.commons.codec.binary.Base64;


@Implements(android.util.Base64.class)
public class ShadowBase64 {
    @Implementation
    public static String encodeToString(byte[] bytes, int flags) {
        Base64 base64 = new Base64();
        return new String(base64.encode(bytes), Charsets.US_ASCII);
    }
    @Implementation
    public static byte[] decode(String str, int flags) {
        Base64 base64 = new Base64();
        return base64.decodeBase64(str.getBytes(Charsets.US_ASCII));
    }
}