package com.soundcloud.android.playback;

import static com.soundcloud.java.checks.Preconditions.checkElementIndex;
import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.checks.Preconditions;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

class SimplePlayQueue extends PlayQueue {
    private final List<PlayQueueItem> playQueueItems;

    SimplePlayQueue(List<PlayQueueItem> playQueueItems) {
        this.playQueueItems = playQueueItems;
    }

    @Override
    public void moveItem(int fromPosition, int toPosition) {
        PlayQueueItem playQueueItem = playQueueItems.remove(fromPosition);
        playQueueItems.add(toPosition, playQueueItem);
    }

    @Override
    public PlayQueue copy() {
        return new SimplePlayQueue(new ArrayList<>(playQueueItems));
    }

    @Override
    public PlayQueueItem getPlayQueueItem(int position) {
        checkElementIndex(position, size());
        return playQueueItems.get(position);
    }

    @Override
    public void removeItem(PlayQueueItem item) {
        playQueueItems.remove(item);
    }

    @Override
    public void removeItemAtPosition(int position) {
        this.playQueueItems.remove(position);
    }

    @Override
    public boolean hasPreviousItem(int position) {
        return position > 0 && !playQueueItems.isEmpty();
    }

    @Override
    public boolean hasNextItem(int position) {
        return position < playQueueItems.size() - 1;
    }

    @Override
    public boolean hasTrackAsNextItem(int position) {
        return hasNextItem(position) && playQueueItems.get(position + 1) instanceof TrackQueueItem;
    }

    @Override
    public void insertAllItems(int position, List<PlayQueueItem> newplayQueueItems) {
        playQueueItems.addAll(position, newplayQueueItems);
    }

    @Override
    public Iterator<PlayQueueItem> iterator() {
        return playQueueItems.iterator();
    }

    @Override
    public int size() {
        return playQueueItems.size();
    }

    @Override
    public boolean isEmpty() {
        return playQueueItems.isEmpty();
    }

    @Override
    public boolean hasItems() {
        return !playQueueItems.isEmpty();
    }

    @Override
    public Urn getUrn(int position) {
        checkElementIndex(position, size());
        return playQueueItems.get(position).getUrn();
    }

    @Override
    public Iterable<? extends PlayQueueItem> itemsWithUrn(final Urn urn) {
        return newArrayList(Iterables.filter(playQueueItems, isMatchingItem(urn)));
    }

    @Override
    public int indexOfTrackUrn(final Urn trackUrn) {
        return Iterables.indexOf(playQueueItems, isMatchingTrackItem(trackUrn));
    }

    @Override
    public int indexOfPlayQueueItem(final PlayQueueItem playQueueItem) {
        return Iterables.indexOf(playQueueItems, isMatchingItem(playQueueItem));
    }

    @Override
    public int indexOfTrackUrn(int startPosition, final Urn urn) {
        final List<PlayQueueItem> subList = playQueueItems.subList(startPosition, this.playQueueItems.size());
        final int index = Iterables.indexOf(subList, isMatchingTrackItem(urn));
        if (index >= 0) {
            return index + startPosition;
        } else {
            return index;
        }
    }

    @Override
    public boolean hasSameTracks(PlayQueue playQueue) {
        if (playQueue.size() != size()) {
            return false;
        } else {
            for (int i = 0; i < size(); i++) {
                if (!playQueue.getPlayQueueItem(i).getUrn().equals(getPlayQueueItem(i).getUrn())) {
                    return false;
                }
            }
        }
        return true;
    }

    private Predicate<PlayQueueItem> isMatchingItem(final Urn urn) {
        return new Predicate<PlayQueueItem>() {
            @Override
            public boolean apply(PlayQueueItem input) {
                return input.getUrn().equals(urn);
            }
        };
    }

    private Predicate<PlayQueueItem> isMatchingTrackItem(final Urn urn) {
        return new Predicate<PlayQueueItem>() {
            @Override
            public boolean apply(PlayQueueItem input) {
                return input.isTrack() && input.getUrn().equals(urn);
            }
        };
    }

    @Override
    public List<Urn> getTrackItemUrns() {
        final List<Urn> trackItemUrns = new ArrayList<>();
        for (PlayQueueItem item : playQueueItems) {
            if (item.isTrack()) {
                trackItemUrns.add(item.getUrn());
            }
        }
        return trackItemUrns;
    }

    @Override
    public List<Urn> getItemUrns(int from, int count) {
        final int to = Math.min(size(), from + count);
        if (to >= from) {
            final List<Urn> itemUrns = new ArrayList<>(to - from);
            for (int i = from; i < to; i++) {
                itemUrns.add(getUrn(i));
            }
            return itemUrns;
        } else {
            // debugging #5168
            ErrorUtils.handleSilentException(new IllegalStateException("Error getting item urns. size = ["
                                                                               + size() + "], from = [" + from + "], count = [" + count + "]"));
            return Collections.emptyList();
        }
    }

    @Override
    public boolean shouldPersistItemAt(int position) {
        return position >= 0 && position < playQueueItems.size() && playQueueItems.get(position).shouldPersist();
    }

    @Override
    public Optional<AdData> getAdData(int position) {
        checkElementIndex(position, size());
        return playQueueItems.get(position).getAdData();
    }

    @Override
    public void insertPlayQueueItem(int position, PlayQueueItem playQueueItem) {
        Preconditions.checkArgument(position >= 0 && position <= size(),
                                    String.format(Locale.getDefault(),
                                                  "Cannot insert item at position:%d, size:%d",
                                                  position,
                                                  playQueueItems.size()));
        playQueueItems.add(position, playQueueItem);
    }

    @Override
    public void replaceItem(int position, List<PlayQueueItem> newItems) {
        Preconditions.checkArgument(position >= 0 && position < size(),
                                    String.format(Locale.getDefault(),
                                                  "Cannot replace item at position:%d, size:%d",
                                                  position,
                                                  newItems.size()));
        playQueueItems.remove(position);
        playQueueItems.addAll(position, newItems);
    }

    @Override
    public void addPlayQueueItem(PlayQueueItem playQueueItem) {
        playQueueItems.add(playQueueItem);
    }

    @Override
    public void addAllPlayQueueItems(Iterable<PlayQueueItem> somePlayQueueItems) {
        Iterables.addAll(playQueueItems, somePlayQueueItems);
    }

    @Override
    ShuffledPlayQueue shuffle(int start) {
        return ShuffledPlayQueue.from(this, start);
    }

    @Override
    boolean isShuffled() {
        return false;
    }

    @Override
    protected List<PlayQueueItem> items() {
        return playQueueItems;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PlayQueue playQueue = (PlayQueue) o;
        return MoreObjects.equal(getTrackItemUrns(), playQueue.getTrackItemUrns());
    }

    @Override
    public int hashCode() {
        return playQueueItems.hashCode();
    }


}
