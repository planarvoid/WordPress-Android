package com.soundcloud.android.robolectric.shadows;

import com.soundcloud.android.creators.record.jni.EncoderOptions;
import com.soundcloud.android.creators.record.jni.VorbisEncoder;
import com.soundcloud.android.creators.upload.UserCanceledException;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@SuppressWarnings("UnusedDeclaration")
@Implements(VorbisEncoder.class)
// WARNING: This shadows a class we own and so depends on
// WARNING: a 'addClassOrPackageToInstrument' line in the TestRunner's constructor
public class ShadowVorbisEncoder {
    public static boolean simulateProgress, simulateCancel;
    public static IOException throwException;

    @Implementation
    public static int encodeWav(File in, File out, EncoderOptions options) throws IOException {
        if (throwException != null) {
            throw throwException;
        }

        if (simulateProgress && options.listener != null) {
            // simulate some progress
            options.listener.onProgress(0, 100);
            options.listener.onProgress(50, 100);
            options.listener.onProgress(100, 100);
        }
        // write some fake data
        out.createNewFile();
        FileOutputStream fos = new FileOutputStream(out);
        fos.write(new byte[8192]);
        fos.close();

        if (simulateCancel) {
            throw new UserCanceledException();
        }
        return 0;
    }

    @Implementation
    public static int encodeVorbis(File in, File out, EncoderOptions options) throws IOException {
        return encodeWav(in, out, options);
    }

    @Implementation
    public static int encodeFile(File in, File out, EncoderOptions options) throws IOException {
        return encodeWav(in, out, options);
    }

    public static void reset() {
        simulateProgress = simulateCancel = false;
        throwException = null;
    }
}
