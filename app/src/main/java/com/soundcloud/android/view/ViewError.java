package com.soundcloud.android.view;

import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.sync.SyncFailedException;

public enum ViewError {
    CONNECTION_ERROR,
    SERVER_ERROR;

    public static ViewError from(Throwable error) {
        if (error instanceof ApiRequestException) {
            return (((ApiRequestException) error).isNetworkError() ?
                    ViewError.CONNECTION_ERROR :
                    ViewError.SERVER_ERROR);
        } else if (error instanceof SyncFailedException) {
            // default Sync Failures to connection for now as we can't tell the diff
            return ViewError.CONNECTION_ERROR;
        }
        throw new IllegalArgumentException(error);
    }
}
