package com.soundcloud.android.likes;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.FragmentLifeCycle;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.tracks.TrackChangedSubscriber;
import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.android.utils.CallsiteToken;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.android.view.adapters.ListContentChangedSubscriber;
import com.soundcloud.android.view.adapters.ReactiveAdapter;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

public class TrackLikesAdapter extends ItemAdapter<PropertySet>
        implements ReactiveAdapter<Iterable<PropertySet>>, FragmentLifeCycle<Fragment> {

    private final CallsiteToken callsiteToken = CallsiteToken.build();

    private final TrackItemPresenter trackPresenter;
    private final EventBus eventBus;

    private Subscription eventSubscriptions = Subscriptions.empty();

    @Inject
    public TrackLikesAdapter(TrackItemPresenter trackPresenter, EventBus eventBus) {
        super(trackPresenter);
        this.trackPresenter = trackPresenter;
        this.eventBus = eventBus;
    }

    @Override
    public void onNext(Iterable<PropertySet> propertySets) {
        for (PropertySet propertySet : propertySets) {
            addItem(propertySet);
        }
        notifyDataSetChanged();
    }

    @Override
    public void onCompleted() {
        // no-op
    }

    @Override
    public void onError(Throwable e) {
        ErrorUtils.handleThrowable(e, callsiteToken);
    }

    public TrackItemPresenter getTrackPresenter() {
        return trackPresenter;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new TrackChangedSubscriber(this, trackPresenter)),
                eventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new ListContentChangedSubscriber(this))
        );
    }

    @Override
    public void onDestroyView() {
        eventSubscriptions.unsubscribe();
    }

    @Override
    public void onBind(Fragment owner) {
        // No-op
    }

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        // No-op
    }

    @Override
    public void onStart() {
        // No-op
    }

    @Override
    public void onResume() {
        // No-op
    }

    @Override
    public void onPause() {
        // No-op
    }

    @Override
    public void onStop() {
        // No-op
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        // No-op
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        // No-op
    }

    @Override
    public void onDestroy() {
        // No-op
    }
}
