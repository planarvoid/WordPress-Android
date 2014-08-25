package com.soundcloud.android.playback.service;


import static com.soundcloud.android.playback.service.PlayQueueManager.FetchRecommendedState;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PlayQueueView implements Parcelable, Iterable<Long> {
    public static final String EXTRA = "PlayQueue";
    public static final PlayQueueView EMPTY = new PlayQueueView(Collections.<Long>emptyList(), -1);

    private final List<Long> trackIds;
    private final FetchRecommendedState fetchState;
    private int position;

    public PlayQueueView(Long id) {
        this(Lists.newArrayList(id), 0);
    }

    public PlayQueueView(List<Long> trackIds, int playPosition) {
        this(trackIds, playPosition, FetchRecommendedState.IDLE);
    }

    public PlayQueueView(List<Long> trackIds, int playPosition, FetchRecommendedState fetchState) {
        this.trackIds = ImmutableList.copyOf(trackIds);
        position = playPosition < 0 || playPosition >= trackIds.size() ? 0 : playPosition;
        this.fetchState = fetchState;
    }

    private PlayQueueView(Parcel in) {
        final int size = in.readInt();
        long[] trackIds = new long[size];
        in.readLongArray(trackIds);

        this.trackIds = Lists.newArrayListWithExpectedSize(trackIds.length);
        for (long n : trackIds) this.trackIds.add(n);
        position = in.readInt();
        fetchState = FetchRecommendedState.valueOf(in.readString());

    }

    @Override
    public Iterator<Long> iterator() {
        return trackIds.iterator();
    }

    public FetchRecommendedState getFetchRecommendedState() {
        return fetchState;
    }

    public boolean isEmpty() {
        return trackIds.isEmpty();
    }

    public int size() {
        return trackIds.size();
    }

    public int getPosition() {
        return position;
    }

    public boolean setPosition(int position) {
        if (position < trackIds.size()) {
            this.position = position;
            return true;
        } else {
            return false;
        }
    }

    public boolean isLastTrack() {
        return position >= trackIds.size() - 1;
    }

    public boolean isLoading() {
        return fetchState == FetchRecommendedState.LOADING;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(trackIds.size());
        dest.writeLongArray(Longs.toArray(trackIds));
        dest.writeInt(position);
        dest.writeString(fetchState.name());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<PlayQueueView> CREATOR = new Parcelable.Creator<PlayQueueView>() {
        public PlayQueueView createFromParcel(Parcel in) {
            return new PlayQueueView(in);
        }

        public PlayQueueView[] newArray(int size) {
            return new PlayQueueView[size];
        }
    };

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("Track IDs", trackIds)
                .add("Size", size())
                .add("Play Position", position)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayQueueView longs = (PlayQueueView) o;

        if (position != longs.position) return false;
        if (fetchState != longs.fetchState) return false;
        if (trackIds != null ? !trackIds.equals(longs.trackIds) : longs.trackIds != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = trackIds != null ? trackIds.hashCode() : 0;
        result = 31 * result + (fetchState != null ? fetchState.hashCode() : 0);
        result = 31 * result + position;
        return result;
    }
}
