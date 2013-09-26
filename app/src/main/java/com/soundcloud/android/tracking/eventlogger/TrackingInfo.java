package com.soundcloud.android.tracking.eventlogger;

import java.io.Serializable;

public class TrackingInfo implements Serializable {
    private String sourceContext;
    private String exploreTag;

    public TrackingInfo(String sourceContext) {
        this(sourceContext, null);
    }

    public TrackingInfo(String sourceContext, String exploreTag) {
        this.sourceContext = sourceContext;
        this.exploreTag = exploreTag;
    }

    public String getSourceContext() {
        return sourceContext;
    }

    public void setSourceContext(String sourceContext) {
        this.sourceContext = sourceContext;
    }

    public String getExploreTag() {
        return exploreTag;
    }

    public void setExploreTag(String exploreTag) {
        this.exploreTag = exploreTag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrackingInfo that = (TrackingInfo) o;

        if (exploreTag != null ? !exploreTag.equals(that.exploreTag) : that.exploreTag != null) return false;
        if (sourceContext != null ? !sourceContext.equals(that.sourceContext) : that.sourceContext != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = sourceContext != null ? sourceContext.hashCode() : 0;
        result = 31 * result + (exploreTag != null ? exploreTag.hashCode() : 0);
        return result;
    }
}
