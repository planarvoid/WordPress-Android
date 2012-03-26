package com.soundcloud.android.jni;


import java.io.File;

public class VorbisDecoder {

    public void decode(File in, File out) throws DecoderException {
        int ret = decode(in.getAbsolutePath(), out.getAbsolutePath());
        if (ret != 0) {
            throw new DecoderException("Error decoding", ret);
        }
    }

    public native int decode(String in, String out);

    static {
        System.loadLibrary("soundcloud_audio_tremor");
    }
}
