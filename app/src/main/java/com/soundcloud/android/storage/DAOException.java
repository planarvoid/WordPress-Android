package com.soundcloud.android.storage;

public class DAOException extends RuntimeException {
    public DAOException() {
        super();
    }

    public DAOException(NumberFormatException e) {
        super(e);
    }

    public DAOException(String s) {
        super(s);
    }
}
