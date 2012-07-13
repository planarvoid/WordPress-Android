package com.soundcloud.android.jni;

import java.io.IOException;

public class EncoderException extends IOException {
    public EncoderException(String detailMessage, int error) {
        super(detailMessage + ":" + VorbisConstants.getString(error));
    }
}
