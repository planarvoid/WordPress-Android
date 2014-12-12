package com.soundcloud.android.db;

import static java.lang.String.format;

public class MigrationFileFormatException extends RuntimeException {
    public MigrationFileFormatException(int versionNumber) {
        super(format("Migration file for version %d is not formatted correctly or does not exist", versionNumber));
    }
}
