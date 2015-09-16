package com.soundcloud.android.events;

import org.jetbrains.annotations.NotNull;

public class EncryptionEvent extends TrackingEvent {

    public static final String KIND_ENCRYPTION_ERROR = "encryption_error";
    public static final String KIND_KEY_GENERATION_ERROR = "key_generation_error";
    public static final String KIND_SUCCESSUFULL_ENCRYPTION = "encryption_success";

    public static EncryptionEvent fromKeyGenerationError() {
        return new EncryptionEvent(KIND_KEY_GENERATION_ERROR);
    }

    public static EncryptionEvent fromEncryptionError() {
        return new EncryptionEvent(KIND_ENCRYPTION_ERROR);
    }

    public static EncryptionEvent fromEncryptionSuccess() {
        return new EncryptionEvent(KIND_SUCCESSUFULL_ENCRYPTION);
    }

    protected EncryptionEvent(@NotNull String kind) {
        super(kind, System.currentTimeMillis());
    }

}
