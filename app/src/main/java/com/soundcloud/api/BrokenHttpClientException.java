package com.soundcloud.api;

import java.io.IOException;

public class BrokenHttpClientException extends IOException {
    private static final long serialVersionUID = -4764332412926419313L;

    public BrokenHttpClientException(Throwable throwable) {
        super(throwable == null ? null : throwable.toString());
        initCause(throwable);
    }
}
