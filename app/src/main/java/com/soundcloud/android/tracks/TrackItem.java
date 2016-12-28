package com.soundcloud.android.tracks;

import static com.soundcloud.android.playback.Durations.getTrackPlayDuration;

import com.soundcloud.android.Consts;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import rx.functions.Func1;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class TrackItem extends PlayableItem implements TieredTrack {

    public static final TrackItem EMPTY = new TrackItem(
            PropertySet.from(
            TrackProperty.URN.bind(Urn.NOT_SET),
            TrackProperty.TITLE.bind(Strings.EMPTY),
            TrackProperty.CREATOR_NAME.bind(Strings.EMPTY),
            TrackProperty.CREATOR_URN.bind(Urn.NOT_SET),
            TrackProperty.SNIPPET_DURATION.bind(0L),
            TrackProperty.FULL_DURATION.bind(0L),
            TrackProperty.WAVEFORM_URL.bind(Strings.EMPTY),
            TrackProperty.IS_USER_LIKE.bind(false),
            TrackProperty.IS_USER_REPOST.bind(false),
            TrackProperty.LIKES_COUNT.bind(0),
            TrackProperty.PERMALINK_URL.bind(Strings.EMPTY),
            TrackProperty.IS_PRIVATE.bind(false)
    ));
    public static final String PLAYABLE_TYPE = "track";

    private boolean isPlaying;

    public static TrackItem from(PropertySet trackState) {
        return new TrackItem(trackState);
    }

    public static TrackItem from(ApiTrack apiTrack, boolean repost) {
        TrackItem trackItem = TrackItem.from(apiTrack);
        trackItem.setRepost(repost);
        return trackItem;
    }

    public static TrackItem from(ApiTrack apiTrack) {
        final Optional<Boolean> subHighTier = apiTrack.isSubHighTier();
        final PropertySet propertySet = PropertySet.from(
                TrackProperty.URN.bind(apiTrack.getUrn()),
                TrackProperty.TITLE.bind(apiTrack.getTitle()),
                TrackProperty.CREATED_AT.bind(apiTrack.getCreatedAt()),
                TrackProperty.SNIPPET_DURATION.bind(apiTrack.getSnippetDuration()),
                TrackProperty.FULL_DURATION.bind(apiTrack.getFullDuration()),
                TrackProperty.IS_PRIVATE.bind(apiTrack.isPrivate()),
                TrackProperty.WAVEFORM_URL.bind(apiTrack.getWaveformUrl()),
                TrackProperty.PERMALINK_URL.bind(apiTrack.getPermalinkUrl()),
                TrackProperty.MONETIZABLE.bind(apiTrack.isMonetizable()),
                TrackProperty.BLOCKED.bind(apiTrack.isBlocked()),
                TrackProperty.SNIPPED.bind(apiTrack.isSnipped()),
                TrackProperty.SYNCABLE.bind(apiTrack.isSyncable()),
                TrackProperty.POLICY.bind(apiTrack.getPolicy()),
                TrackProperty.SUB_HIGH_TIER.bind(subHighTier.isPresent() ? subHighTier.get() : false),
                TrackProperty.PLAY_COUNT.bind(apiTrack.getStats().getPlaybackCount()),
                TrackProperty.COMMENTS_COUNT.bind(apiTrack.getStats().getCommentsCount()),
                TrackProperty.LIKES_COUNT.bind(apiTrack.getStats().getLikesCount()),
                TrackProperty.REPOSTS_COUNT.bind(apiTrack.getStats().getRepostsCount()),
                TrackProperty.CREATOR_NAME.bind(apiTrack.getUserName()),
                TrackProperty.CREATOR_URN.bind(apiTrack.getUser() != null ? apiTrack.getUser().getUrn() : Urn.NOT_SET),
                EntityProperty.IMAGE_URL_TEMPLATE.bind(apiTrack.getImageUrlTemplate())
        );

        if (apiTrack.getGenre() != null) {
            propertySet.put(TrackProperty.GENRE, apiTrack.getGenre());
        }

        if (apiTrack.isSubMidTier().isPresent()) {
            propertySet.put(TrackProperty.SUB_MID_TIER, apiTrack.isSubMidTier().get());
        }
        if (apiTrack.getMonetizationModel().isPresent()) {
            propertySet.put(TrackProperty.MONETIZATION_MODEL, apiTrack.getMonetizationModel().get());
        }

        return new TrackItem(propertySet);
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
                    trackItems.add(from(source));
                }
                return trackItems;
            }
        };
    }

    protected TrackItem(PropertySet source) {
        super(source);
    }

    @Override
    public TrackItem updated(PropertySet playableData) {
        super.updated(playableData);
        return this;
    }

    public TrackItem updated(TrackItem trackItem) {
        return updated(trackItem.source);
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
        return getTrackPlayDuration(this);
    }

    public OfflineState getOfflineState() {
        return source.getOrElse(OfflineProperty.OFFLINE_STATE, OfflineState.NOT_OFFLINE);
    }

    public void setOfflineState(OfflineState state) {
        source.put(OfflineProperty.OFFLINE_STATE, state);
    }

    public boolean isUnavailableOffline() {
        return getOfflineState() == OfflineState.UNAVAILABLE;
    }

    public int getPlayCount() {
        return source.getOrElse(TrackProperty.PLAY_COUNT, Consts.NOT_SET);
    }

    public void setPlayCount(int playCount) {
        source.put(TrackProperty.PLAY_COUNT, playCount);
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

    public void setBlocked(boolean blocked) {
        source.put(TrackProperty.BLOCKED, blocked);
    }

    @Override
    public boolean isSnipped() {
        return source.getOrElse(TrackProperty.SNIPPED, false);
    }

    public void setSnipped(boolean snipped) {
        source.put(TrackProperty.SNIPPED, snipped);
    }

    @Override
    public boolean isSubMidTier() {
        return source.getOrElse(TrackProperty.SUB_MID_TIER, false);
    }

    public void setSubMidTier(boolean subMidTier) {
        source.put(TrackProperty.SUB_MID_TIER, subMidTier);
    }

    @Override
    public boolean isSubHighTier() {
        return source.getOrElse(TrackProperty.SUB_HIGH_TIER, false);
    }

    public void setSubHighTier(boolean subHighTier) {
        source.put(TrackProperty.SUB_HIGH_TIER, subHighTier);
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
        return MoreObjects.toStringHelper(this).addValue(source).toString();
    }

    public boolean updateNowPlaying(Urn nowPlaying) {
        final boolean isCurrent = getUrn().equals(nowPlaying);
        if (isPlaying() || isCurrent) {
            setIsPlaying(isCurrent);
            return true;
        }
        return false;
    }

    public EntityStateChangedEvent toUpdateEvent() {
        return EntityStateChangedEvent.forUpdate(source);
    }

    public void setAd(boolean ad) {
        source.put(AdProperty.IS_AUDIO_AD, ad);
    }

    public boolean isAd() {
        return source.getOrElse(AdProperty.IS_AUDIO_AD, false);
    }

    public long getSnippetDuration() {
        return source.get(TrackProperty.SNIPPET_DURATION);
    }

    public long getFullDuration() {
        return source.get(TrackProperty.FULL_DURATION);
    }

    public boolean isMonetizable() {
        return source.getOrElse(TrackProperty.MONETIZABLE, false);
    }

    public int getCommentsCount() {
        return source.getOrElse(TrackProperty.COMMENTS_COUNT, 0);
    }

    public void setCommentsCount(int commentsCount) {
        source.put(TrackProperty.COMMENTS_COUNT, commentsCount);
    }

    public String getMonetizationModel() {
        return source.get(TrackProperty.MONETIZATION_MODEL);
    }

    public String getPolicy() {
        return source.getOrElseNull(TrackProperty.POLICY);
    }

    public String getWaveformUrl() {
        return source.getOrElseNull(TrackProperty.WAVEFORM_URL);
    }

    public boolean isUserRepost() {
        return source.getOrElse(TrackProperty.IS_USER_REPOST, false);
    }

    public boolean isCommentable() {
        return source.getOrElse(TrackProperty.IS_COMMENTABLE, false);
    }

    public void setCommentable(boolean commentable) {
        source.put(TrackProperty.IS_COMMENTABLE, commentable);
    }

    public String getDescription() {
        return source.getOrElse(TrackProperty.DESCRIPTION, Strings.EMPTY);
    }

    public void setDescription(String description) {
        source.put(TrackProperty.DESCRIPTION, description);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(source, 0);
    }

    public TrackItem(Parcel in) {
        super(in.readParcelable(TrackItem.class.getClassLoader()));
    }

    public static final Parcelable.Creator<TrackItem> CREATOR = new Parcelable.Creator<TrackItem>() {
        public TrackItem createFromParcel(Parcel in) {
            return new TrackItem(in);
        }

        public TrackItem[] newArray(int size) {
            return new TrackItem[size];
        }
    };
}
