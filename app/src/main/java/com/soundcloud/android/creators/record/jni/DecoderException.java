package com.soundcloud.android.creators.record.jni;

import java.io.IOException;

public class DecoderException extends IOException {

    public DecoderException(String detailMessage, int error) {
        super(detailMessage + ": " + VorbisConstants.getString(error));
    }
}
