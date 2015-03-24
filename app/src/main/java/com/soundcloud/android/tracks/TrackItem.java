package com.soundcloud.android.tracks;

import com.google.common.base.Optional;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.propeller.PropertySet;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TrackItem extends PlayableItem {

    private static final Date MIN_DATE = new Date(0L);

    public static TrackItem from(PropertySet trackState) {
        return new TrackItem(trackState);
    }

    public static TrackItem from(ApiTrack apiTrack) {
        return new TrackItem(apiTrack.toPropertySet());
    }

    public static Func1<PropertySet, TrackItem> fromPropertySet() {
        return new Func1<PropertySet, TrackItem>() {
            @Override
            public TrackItem call(PropertySet bindings) {
                return TrackItem.from(bindings);
            }
        };
    }

    public static Func1<List<PropertySet>, List<TrackItem>> fromPropertySets() {
        return new Func1<List<PropertySet>, List<TrackItem>>() {
            @Override
            public List<TrackItem> call(List<PropertySet> bindings) {
                List<TrackItem> trackItems = new ArrayList<>(bindings.size());
                for (PropertySet source : bindings) {
                    trackItems.add(from(source));
                }
                return trackItems;
            }
        };
    }

    public static <T extends Iterable<ApiTrack>> Func1<T, List<TrackItem>> fromApiTracks() {
        return new Func1<T, List<TrackItem>>() {
            @Override
            public List<TrackItem> call(T trackList) {
                List<TrackItem> trackItems = new ArrayList<>();
                for (ApiTrack source : trackList) {
                    trackItems.add(from(source.toPropertySet()));
                }
                return trackItems;
            }
        };
    }

    TrackItem(PropertySet source) {
        super(source);
    }

    public long getDuration() {
        return source.get(TrackProperty.DURATION);
    }

    int getPlayCount() {
        return source.getOrElse(TrackProperty.PLAY_COUNT, Consts.NOT_SET);
    }

    Date getDownloadRemovedAt() {
        return source.getOrElse(OfflineProperty.REMOVED_AT, MIN_DATE);
    }

    Date getDownloadUnavailableAt() {
        return source.getOrElse(OfflineProperty.UNAVAILABLE_AT, MIN_DATE);
    }

    Optional<Date> getDownloadRequestedAt() {
        return Optional.fromNullable(source.getOrElseNull(OfflineProperty.REQUESTED_AT));
    }

    Optional<Date> getDownloadedAt() {
        return Optional.fromNullable(source.getOrElseNull(OfflineProperty.DOWNLOADED_AT));
    }

    String getGenre() {
        return source.get(TrackProperty.GENRE);
    }

    boolean isDownloading() {
        return source.getOrElse(OfflineProperty.DOWNLOADING, false);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TrackItem && ((TrackItem) o).source.equals(this.source);
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }
}
