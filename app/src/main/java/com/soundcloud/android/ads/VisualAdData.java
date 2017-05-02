package com.soundcloud.android.ads;

import android.net.Uri;

import java.util.List;

public abstract class VisualAdData extends AdData {
    private boolean metaAdDismissed;
    private boolean metaAdClicked;
    private boolean metaAdCompleted;

    public boolean isMetaAdDismissed() {
        return metaAdDismissed;
    }

    public boolean isMetaAdClicked() {
        return metaAdClicked;
    }

    public boolean isMetaAdCompleted() {
        return metaAdCompleted;
    }

    public void setMetaAdDismissed() {
        metaAdDismissed = true;
    }

    public void setMetaAdClicked() {
        metaAdClicked = true;
    }

    public void setMetaAdCompleted() {
        metaAdCompleted = true;
    }

    public void resetMetaAdState() {
        metaAdCompleted = false;
        metaAdClicked = false;
    }

    public abstract String imageUrl();

    public abstract Uri clickthroughUrl();

    public abstract List<String> impressionUrls();

    public abstract List<String> clickUrls();
}
