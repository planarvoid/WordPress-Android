package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EntityStateChangedEvent.IS_TRACK_ADDED_TO_PLAYLIST_FILTER;
import static com.soundcloud.android.events.EventQueue.ENTITY_STATE_CHANGED;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

@AutoFactory
class PlaylistDefaultView implements PlaylistContentPresenter.PlaylistContentView {

    private final PlaylistPresenter presenter;
    private final EventBus eventBus;
    private Subscription eventSubscription = RxUtils.invalidSubscription();

    PlaylistDefaultView(@Provided EventBus eventBus, PlaylistPresenter presenter) {
        this.presenter = presenter;
        this.eventBus = eventBus;
    }

    @Override
    public void start() {
        eventSubscription = eventBus.queue(ENTITY_STATE_CHANGED)
                                    .filter(IS_TRACK_ADDED_TO_PLAYLIST_FILTER)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new ReloadSubscriber());
    }

    @Override
    public void stop() {
        eventSubscription.unsubscribe();
    }

    @Override
    public void onItemClicked(int position) {
        presenter.handlItemClick(position);
    }

    @Override
    public void onHeaderClick() {
        presenter.playFromBegninning();
    }

    private class ReloadSubscriber extends DefaultSubscriber<EntityStateChangedEvent> {
        @Override
        public void onNext(EntityStateChangedEvent event) {
            presenter.reloadPlaylist();
        }
    }
}
