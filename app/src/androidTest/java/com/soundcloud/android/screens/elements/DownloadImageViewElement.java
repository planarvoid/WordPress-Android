package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.viewelements.ViewElement;

public class DownloadImageViewElement {
    private final ViewElement wrappedElement;

    public DownloadImageViewElement(ViewElement element) {
        this.wrappedElement = element;
    }

    public boolean isVisible() {
        return wrappedElement.isOnScreen();
    }

    public boolean isUnavailable() {
        return wrappedElement.toDownloadImageView().isUnavailable();
    }

    public boolean isRequested() {
        return wrappedElement.toDownloadImageView().isRequested();
    }

    public boolean isDownloading() {
        return wrappedElement.toDownloadImageView().isDownloading();
    }

    public boolean isDownloaded() {
        return wrappedElement.toDownloadImageView().isDownloaded();
    }
}

