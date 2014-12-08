package com.soundcloud.android.db;

import java.io.IOException;

public class MigrationCopyException extends IOException {
    public MigrationCopyException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public MigrationCopyException(String msg) {
        super(msg);
    }
}
