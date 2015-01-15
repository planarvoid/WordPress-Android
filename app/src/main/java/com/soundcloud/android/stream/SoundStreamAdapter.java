package com.soundcloud.android.stream;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.lightcycle.FragmentLightCycle;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.view.adapters.EndlessAdapter;
import com.soundcloud.android.view.adapters.ListContentChangedSubscriber;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import com.soundcloud.android.tracks.TrackChangedSubscriber;
import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

class SoundStreamAdapter extends EndlessAdapter<PropertySet> implements FragmentLightCycle {

    @VisibleForTesting static final int TRACK_ITEM_TYPE = 0;
    @VisibleForTesting static final int PLAYLIST_ITEM_TYPE = 1;

    private final EventBus eventBus;
    private final TrackItemPresenter trackPresenter;
    private Subscription eventSubscriptions = Subscriptions.empty();

    @Inject
    SoundStreamAdapter(TrackItemPresenter trackPresenter, PlaylistItemPresenter playlistPresenter, EventBus eventBus) {
        super(new CellPresenterEntity<>(TRACK_ITEM_TYPE, trackPresenter),
                new CellPresenterEntity<>(PLAYLIST_ITEM_TYPE, playlistPresenter));
        this.eventBus = eventBus;
        this.trackPresenter = trackPresenter;
    }

    @Override
    public int getItemViewType(int position) {
        final int itemViewType = super.getItemViewType(position);
        if (itemViewType == IGNORE_ITEM_VIEW_TYPE) {
            return itemViewType;
        } else if (getItem(position).get(PlayableProperty.URN).isTrack()) {
            return TRACK_ITEM_TYPE;
        } else {
            return PLAYLIST_ITEM_TYPE;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {

    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new TrackChangedSubscriber(this, trackPresenter)),
                eventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new ListContentChangedSubscriber(this))
        );
    }

    @Override
    public void onStart(Fragment fragment) {

    }

    @Override
    public void onResume(Fragment fragment) {

    }

    @Override
    public void onPause(Fragment fragment) {

    }

    @Override
    public void onStop(Fragment fragment) {

    }

    @Override
    public void onSaveInstanceState(Fragment fragment, Bundle bundle) {

    }

    @Override
    public void onRestoreInstanceState(Fragment fragment, Bundle bundle) {

    }

    @Override
    public void onDestroyView(Fragment fragment) {
        eventSubscriptions.unsubscribe();
    }

    @Override
    public void onDestroy(Fragment fragment) {

    }
}
