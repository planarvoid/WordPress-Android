package com.soundcloud.android.jni;

import java.io.IOException;

public class EncoderException extends IOException {
    public final int error;
    public EncoderException(String detailMessage, int error) {
        super(detailMessage);
        this.error = error;
    }
}
