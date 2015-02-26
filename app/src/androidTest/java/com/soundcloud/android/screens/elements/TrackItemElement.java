package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;

public class TrackItemElement {
    private final ViewElement wrapped;

    public TrackItemElement(ViewElement wrapped) {
        this.wrapped = wrapped;
    }

    public boolean isDownloadingOrDownloaded() {
        return wrapped.findElement(With.id(R.id.download_progress_icon)).isVisible();
    }
}
