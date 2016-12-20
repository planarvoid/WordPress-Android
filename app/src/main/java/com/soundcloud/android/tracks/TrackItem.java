package com.soundcloud.android.tracks;

import static com.soundcloud.android.playback.Durations.getTrackPlayDuration;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import rx.functions.Func1;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class TrackItem extends PlayableItem implements TieredTrack {

    public static final String PLAYABLE_TYPE = "track";

    private boolean isPlaying;

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
                return TrackItem.fromPropertySets(bindings);
            }
        };
    }

    @NonNull
    public static List<TrackItem> fromPropertySets(List<PropertySet> bindings) {
        List<TrackItem> trackItems = new ArrayList<>(bindings.size());
        for (PropertySet source : bindings) {
            trackItems.add(from(source));
        }
        return trackItems;
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

    @Override
    public TrackItem updated(PropertySet playableData) {
        super.updated(playableData);
        return this;
    }

    @Override
    public TrackItem updatedWithOfflineState(OfflineState offlineState) {
        super.updatedWithOfflineState(offlineState);
        return this;
    }

    public TrackItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus) {
        super.updatedWithLike(likeStatus);
        return this;
    }

    public TrackItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus) {
        super.updatedWithRepost(repostStatus);
        return this;
    }


    @Override
    public String getPlayableType() {
        return PLAYABLE_TYPE;
    }

    @Override
    public long getDuration() {
        return getTrackPlayDuration(source);
    }

    public OfflineState getOfflineState() {
        return source.getOrElse(OfflineProperty.OFFLINE_STATE, OfflineState.NOT_OFFLINE);
    }

    public boolean isUnavailableOffline() {
        return getOfflineState() == OfflineState.UNAVAILABLE;
    }

    public int getPlayCount() {
        return source.getOrElse(TrackProperty.PLAY_COUNT, Consts.NOT_SET);
    }

    public void setIsPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean hasPlayCount() {
        return getPlayCount() > 0;
    }

    @Override
    public boolean isBlocked() {
        return source.getOrElse(TrackProperty.BLOCKED, false);
    }

    @Override
    public boolean isSnipped() {
        return source.getOrElse(TrackProperty.SNIPPED, false);
    }

    @Override
    public boolean isSubMidTier() {
        return source.getOrElse(TrackProperty.SUB_MID_TIER, false);
    }

    @Override
    public boolean isSubHighTier() {
        return source.getOrElse(TrackProperty.SUB_HIGH_TIER, false);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TrackItem && ((TrackItem) o).source.equals(this.source)
                && ((TrackItem) o).isPlaying == this.isPlaying;
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(source, isPlaying);
    }

    @Override
    public String toString() {
        return source.toString();
    }

    public boolean updateNowPlaying(Urn nowPlaying) {
        final boolean isCurrent = getUrn().equals(nowPlaying);
        if (isPlaying() || isCurrent) {
            setIsPlaying(isCurrent);
            return true;
        }
        return false;
    }
}
