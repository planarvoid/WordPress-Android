package com.soundcloud.android.robolectric.shadows;

import com.soundcloud.android.jni.VorbisEncoder;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

@Implements(VorbisEncoder.class)
public class ShadowVorbisEncoder {
    @Implementation
    public int init(String output, String mode, long channels, long rate, float quality) {
        return 0;
    }

    @Implementation
    public int addSamples(ByteBuffer samples, long length) {
        return 0;
    }

    @Implementation
    public static int encodeWav(File in, File out, float quality) throws IOException {
        return 0;
    }
}
