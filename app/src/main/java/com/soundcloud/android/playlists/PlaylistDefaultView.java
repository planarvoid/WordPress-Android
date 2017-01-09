package com.soundcloud.android.playlists;

import static com.soundcloud.android.events.EventQueue.PLAYLIST_CHANGED;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.events.PlaylistTrackCountChangedEvent;
import com.soundcloud.android.rx.RxUtils;
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
        eventSubscription.unsubscribe();
        eventSubscription = eventBus.queue(PLAYLIST_CHANGED)
                                    .filter(event -> event.kind() == PlaylistTrackCountChangedEvent.Kind.TRACK_ADDED)
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(event -> presenter.reloadPlaylist());
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
}
