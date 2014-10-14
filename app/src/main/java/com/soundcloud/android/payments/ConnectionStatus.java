package com.soundcloud.android.payments;

public enum ConnectionStatus {

    DISCONNECTED,
    UNSUPPORTED,
    READY;

    public boolean isReady() {
        return this == READY;
    }

    public boolean isUnsupported() {
        return this == UNSUPPORTED;
    }

    public boolean isDisconnected() {
        return this == DISCONNECTED;
    }

}
