package com.soundcloud.android.ads;

import android.net.Uri;

import java.util.List;

public abstract class OverlayAdData extends AdData {
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

    public abstract String getImageUrl();

    public abstract Uri getClickthroughUrl();

    public abstract List<String> getImpressionUrls();

    public abstract List<String> getClickUrls();
}
