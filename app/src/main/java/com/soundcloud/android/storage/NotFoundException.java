package com.soundcloud.android.storage;

public class NotFoundException extends Exception {

    public NotFoundException(long resourceId) {
        super("Record not found: " + resourceId);
    }
}
