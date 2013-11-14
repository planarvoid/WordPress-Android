package com.soundcloud.android.creators.record.jni;

import java.io.IOException;

public class EncoderException extends IOException {
    public EncoderException(String detailMessage, int error) {
        super(detailMessage + ": " + VorbisConstants.getString(error));
    }
}
