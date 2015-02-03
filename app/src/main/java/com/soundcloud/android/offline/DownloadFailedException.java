package com.soundcloud.android.offline;

import com.google.common.base.Objects;

class DownloadFailedException extends Exception {

    private final Exception wrapped;
    private final DownloadRequest fromRequest;

    public DownloadFailedException(DownloadRequest fromRequest, Exception wrapped) {
        this.fromRequest = fromRequest;
        this.wrapped = wrapped;
    }

    public Exception getException() {
        return wrapped;
    }

    public DownloadRequest getDownloadRequest() {
        return fromRequest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DownloadFailedException)) {
            return false;
        }
        final DownloadFailedException that = (DownloadFailedException) o;
        return Objects.equal(fromRequest, that.fromRequest) && Objects.equal(wrapped, that.wrapped);
    }

    @Override
    public int hashCode() {
        int result = wrapped.hashCode();
        result = 31 * result + fromRequest.hashCode();
        return result;
    }
}
