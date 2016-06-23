package com.soundcloud.android.offline;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class DownloadRequest implements ImageResource {

    public static final Function<DownloadRequest, Urn> TO_TRACK_URN = new Function<DownloadRequest, Urn>() {
        @Override
        public Urn apply(DownloadRequest request) {
            return request.getUrn();
        }
    };

    public static DownloadRequest create(Urn track,
                                         Optional<String> imageUrlTemplate,
                                         long duration,
                                         String waveformUrl,
                                         boolean syncable,
                                         TrackingMetadata trackingMetadata) {
        return new AutoValue_DownloadRequest(track,
                                             imageUrlTemplate,
                                             duration,
                                             waveformUrl,
                                             syncable,
                                             trackingMetadata);
    }

    @Override
    public abstract Urn getUrn();

    @Override
    public abstract Optional<String> getImageUrlTemplate();

    public abstract long getDuration();

    public abstract String getWaveformUrl();

    public abstract boolean isSyncable();

    public abstract TrackingMetadata getTrackingData();

}
