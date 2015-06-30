package com.soundcloud.android.view.adapters;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.ItemAdapter;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.propeller.PropertySet;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// purely exists for bridging to legacy code from the old stack
public abstract class LegacyAdapterBridge<T extends ScModel> extends ScBaseAdapter<T> implements ItemAdapter<T> {

    protected final List<PlayableItem> listItems = new ArrayList<>(Consts.LIST_PAGE_SIZE);

    public LegacyAdapterBridge(Uri uri) {
        super(uri);
    }

    @Override
    public void updateItems(Map<Urn, PublicApiResource> updatedItems) {
        for (int i = 0; i < listItems.size(); i++) {
            final PlayableItem original = listItems.get(i);
            final Urn key = original.getEntityUrn();
            if (updatedItems.containsKey(key)) {
                listItems.set(i, toListItemKeepingReposterInfo(updatedItems.get(key), original));
            }
        }
        notifyDataSetChanged();
    }

    protected PlayableItem toListItemKeepingReposterInfo(PublicApiResource resource, PlayableItem original) {
        final PropertySet propertySet = ((Playable) resource).toPropertySet();
        final Optional<String> optionalReposter = original.getReposter();
        if (optionalReposter.isPresent()) {
            propertySet.put(PlayableProperty.REPOSTER, optionalReposter.get());
        }
        return resource.getUrn().isTrack() ? TrackItem.from(propertySet) : PlaylistItem.from(propertySet);
    }

    @Override
    public void addItems(List<T> newItems) {
        super.addItems(newItems);
        this.listItems.addAll(toPresentationModels(newItems));
    }

    @Override
    public void clearData() {
        super.clearData();
        this.listItems.clear();
    }

    protected List<PlayableItem> toPresentationModels(List<T> items) {
        final List<PlayableItem> list = new ArrayList<>(items.size());
        for (T resource : items) {
            if (resource instanceof PlayableHolder) {
                final PropertySet propertySet = toPropertySet(resource);
                final Urn urn = ((PlayableHolder) resource).getPlayable().getUrn();
                list.add(urn.isPlaylist() ? PlaylistItem.from(propertySet) : TrackItem.from(propertySet));
            } else {
                throw new IllegalStateException("List item must be a PlayableHolder");
            }
        }
        return list;
    }

    protected abstract PropertySet toPropertySet(T item);

    protected boolean isTrack(int position) {
        return listItems.get(position).getEntityUrn().isTrack();
    }

    public final class PlayableChangedSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(final EntityStateChangedEvent event) {
            final int index = Iterables.indexOf(listItems, new Predicate<PlayableItem>() {
                @Override
                public boolean apply(PlayableItem item) {
                    return item.getEntityUrn().equals(event.getFirstUrn());
                }
            });

            if (index > -1) {
                listItems.get(index).update(event.getNextChangeSet());
                notifyDataSetChanged();
            }
        }
    }

    @Override
    public void addItem(T item) {
        // no op. exists to satisy interface contract so we can use these adapters in newer code
    }

    @Override
    public void removeItem(int position) {
        // no op. exists to satisy interface contract so we can use these adapters in newer code
    }

    @Override
    public void prependItem(T t) {
        // no op. exists to satisy interface contract so we can use these adapters in newer code
    }

    @Override
    public void clear() {
        // no op. exists to satisy interface contract so we can use these adapters in newer code
    }

    @Override
    public void onNext(Iterable<T> ts) {
        // no op. exists to satisy interface contract so we can use these adapters in newer code
    }

    @Override
    public void onCompleted() {
        // no op. exists to satisy interface contract so we can use these adapters in newer code
    }

    @Override
    public void onError(Throwable e) {
        // no op. exists to satisy interface contract so we can use these adapters in newer code
    }
}
