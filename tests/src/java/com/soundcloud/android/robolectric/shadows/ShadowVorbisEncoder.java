package com.soundcloud.android.robolectric.shadows;

import com.soundcloud.android.jni.ProgressListener;
import com.soundcloud.android.jni.VorbisEncoder;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("UnusedDeclaration")
@Implements(VorbisEncoder.class)
public class ShadowVorbisEncoder {
    @Implementation
    public static int encodeWav(File in, File out, float quality, ProgressListener l) throws IOException {
        return 0;
    }
}
