package com.soundcloud.android.robolectric.shadows;

import com.soundcloud.android.jni.EncoderOptions;
import com.soundcloud.android.jni.ProgressListener;
import com.soundcloud.android.jni.VorbisEncoder;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("UnusedDeclaration")
@Implements(VorbisEncoder.class)
public class ShadowVorbisEncoder {
    public static boolean simulateProgress;
    public static IOException throwException;

    @Implementation
    public static int encodeWav(File in, File out, EncoderOptions options) throws IOException {
        if (throwException != null) throw throwException;
        if (simulateProgress && options.listener != null) {
            // simulate some progress
            options.listener.onProgress(0, 100);
            options.listener.onProgress(50, 100);
            options.listener.onProgress(100, 100);
        }
        return 0;
    }

    public static void reset() {
        simulateProgress = false;
        throwException = null;
    }
}
