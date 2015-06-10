package com.soundcloud.android.crypto;

public class EncryptionException extends Exception {

    public EncryptionException(String message, Exception wrapped) {
        super(message, wrapped);
    }

    public EncryptionException(String message) {
        super(message);
    }
}
