package com.soundcloud.android.discovery;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.PropertySet;

public class ChartTrackItem extends TrackItem {
    private final static int EMPTY_POSITION = -1;
    private final ChartType chartType;
    private final int position;

    public ChartTrackItem(ChartType chartType, ApiTrack apiTrack) {
        this(chartType, apiTrack.toPropertySet(), EMPTY_POSITION);
    }

    @VisibleForTesting
    public ChartTrackItem(ChartType chartType, PropertySet propertySet, int position) {
        super(propertySet);
        this.chartType = chartType;
        this.position = position;
    }

    public ChartType chartType() {
        return chartType;
    }

    public int position() {
        return position;
    }

    public ChartTrackItem copyWithPosition(int position) {
        return new ChartTrackItem(chartType, getSource(), position);
    }
}
