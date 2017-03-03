package com.soundcloud.android.view;

import com.soundcloud.java.optional.Optional;

public enum EmptyStatus {
    WAITING,
    CONNECTION_ERROR,
    SERVER_ERROR,
    OK;

    // Todo : This should accept CollectionLoaderState when PlaylistDetailFragment stops using AsyncViewModel
    public static EmptyStatus fromErrorAndLoading(Optional<ViewError> viewErrorOptional, boolean isLoadingNextPage) {
        if (viewErrorOptional.isPresent()) {
            if (viewErrorOptional.get() == ViewError.CONNECTION_ERROR) {
                return EmptyStatus.CONNECTION_ERROR;
            } else {
                return EmptyStatus.SERVER_ERROR;
            }
        } else if (isLoadingNextPage) {
            return EmptyStatus.WAITING;
        } else {
            return EmptyStatus.OK;
        }
    }
}
