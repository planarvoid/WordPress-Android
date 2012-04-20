package com.soundcloud.android.jni;


import java.io.File;
import java.nio.ByteBuffer;

public class VorbisDecoder {
    public final File file;

    @SuppressWarnings("UnusedDeclaration") // used in JNI code
    private int decoder_state;

    public VorbisDecoder(File file) {
        this.file = file;
        init(file.getAbsolutePath());
    }

    public native Info getInfo();

    public void decodeToFile(File out) throws DecoderException {
        int ret = decodeToFile(out.getAbsolutePath());
        if (ret < 0) {
            throw new DecoderException("decode return < 0", ret);
        }
    }

    // private methods
    private native int init(String file);
    private native void release();
    private native int decode(ByteBuffer out);
    private native int decodeToFile(String out);

    static {
        System.loadLibrary("soundcloud_audio_tremor");
    }
}
