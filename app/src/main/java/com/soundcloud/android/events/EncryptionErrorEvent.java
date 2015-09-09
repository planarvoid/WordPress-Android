package com.soundcloud.android.events;

import org.jetbrains.annotations.NotNull;

public class EncryptionErrorEvent extends TrackingEvent {

    public static final String DETAIL_EXCEPTION = "exception";
    public static final String MESSAGE = "message";
    public static final String KIND_ENCRYPTION_ERROR = "encryption_error";
    public static final String KIND_KEY_GENERATION = "key_generation";

    public static EncryptionErrorEvent fromKeyGeneration(String message, Exception exception) {
        return new EncryptionErrorEvent(KIND_KEY_GENERATION, message, exception);
    }

    public static EncryptionErrorEvent fromEncryption(String message, Throwable exception) {
        return new EncryptionErrorEvent(KIND_ENCRYPTION_ERROR, message, exception);
    }

    protected EncryptionErrorEvent(@NotNull String kind, @NotNull String message, Throwable exception) {
        super(kind, System.currentTimeMillis());
        put(MESSAGE, message);

        if (exception != null) {
            put(DETAIL_EXCEPTION, exception.getMessage());
        }
    }

    public String getDetailMessage() {
        return get(DETAIL_EXCEPTION);
    }

    public String getMessage() {
        return get(MESSAGE);
    }
}
