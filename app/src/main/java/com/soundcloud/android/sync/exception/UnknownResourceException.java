package com.soundcloud.android.sync.exception;

import android.net.Uri;

import java.io.IOException;

public class UnknownResourceException extends IOException {
    public UnknownResourceException(Uri uri) {
        super(String.valueOf(uri));
    }
}
