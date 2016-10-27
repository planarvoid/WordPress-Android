package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.rx.eventbus.EventBus;

@AutoFactory
class PlaylistEditView implements PlaylistContentPresenter.PlaylistContentView {

    private final EventBus eventBus;
    private final PlaylistPresenter presenter;

    PlaylistEditView(@Provided EventBus eventBus,
                     PlaylistPresenter presenter) {
        this.eventBus = eventBus;
        this.presenter = presenter;
    }

    @Override
    public void start() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.hidePlayer());
        presenter.showEditMode();
    }

    @Override
    public void stop() {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer());
        presenter.savePlaylist();
    }

    @Override
    public void onItemClicked(int position) {
        // no-op
    }

    @Override
    public void onHeaderClick() {
        // no-op
    }
}
