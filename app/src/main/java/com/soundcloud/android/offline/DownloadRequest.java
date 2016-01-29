package com.soundcloud.android.offline;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.functions.Function;

@AutoValue
public abstract class DownloadRequest {

    public static final Function<DownloadRequest, Urn> TO_TRACK_URN = new Function<DownloadRequest, Urn>() {
        @Override
        public Urn apply(DownloadRequest request) {
            return request.getTrack();
        }
    };

    public static DownloadRequest create(Urn track, long duration, String waveformUrl,
                                         boolean syncable, TrackingMetadata trackingMetadata) {
        return new AutoValue_DownloadRequest(track, duration, waveformUrl, syncable, trackingMetadata);
    }

    public abstract Urn getTrack();

    public abstract long getDuration();

    public abstract String getWaveformUrl();

    public abstract boolean isSyncable();

    public abstract TrackingMetadata getTrackingData();

}
