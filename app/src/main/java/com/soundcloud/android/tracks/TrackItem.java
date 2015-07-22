package com.soundcloud.android.tracks;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;

public class TrackItem extends PlayableItem {

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

    public TrackItem(PropertySet source) {
        super(source);
    }

    public long getDuration() {
        return source.get(TrackProperty.DURATION);
    }

    public OfflineState getDownloadedState() {
        return source.getOrElse(OfflineProperty.OFFLINE_STATE, OfflineState.NO_OFFLINE);
    }

    public boolean isMidTier() {
        // this should really be get, EVENTUALLY... (we dont have policy for everything reliably yet)
        return source.getOrElse(TrackProperty.SUB_MID_TIER, false);
    }

    int getPlayCount() {
        return source.getOrElse(TrackProperty.PLAY_COUNT, Consts.NOT_SET);
    }

    String getGenre() {
        final Optional<String> optionalGenre = source.get(TrackProperty.GENRE);
        return optionalGenre.isPresent() ? optionalGenre.get() : ScTextUtils.EMPTY_STRING;
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
