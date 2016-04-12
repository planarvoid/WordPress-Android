package com.soundcloud.android.playlists;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.rx.eventbus.EventBus;
import rx.functions.Func1;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.ArrayList;

public class SplitPlaylistPresenter extends PlaylistPresenter {

    @Inject
    public SplitPlaylistPresenter(PlaylistOperations playlistOperations,
                                  SwipeRefreshAttacher swipeRefreshAttacher,
                                  PlaylistHeaderPresenter headerPresenter,
                                  PlaylistAdapterFactory adapterFactory,
                                  EventBus eventBus,
                                  Navigator navigator,
                                  ViewStrategyFactory viewStrategyFactory) {
        super(playlistOperations, swipeRefreshAttacher, headerPresenter, adapterFactory, eventBus, navigator, viewStrategyFactory);
    }

    @Override
    protected Func1<PlaylistWithTracks, Iterable<ListItem>> getListItemTransformation() {
        return new Func1<PlaylistWithTracks, Iterable<ListItem>>() {
            @Override
            public Iterable<ListItem> call(PlaylistWithTracks playlistWithTracks) {
                return new ArrayList<ListItem>(playlistWithTracks.getTracks());
            }
        };
    }

    @NonNull
    @Override
    protected PlaylistSubscriber getPlaylistSubscriber() {
        return new PlaylistSubscriber(){
            @Override
            public void onNext(PlaylistWithTracks playlistWithTracks) {
                super.onNext(playlistWithTracks);
                headerPresenter.setPlaylist(PlaylistHeaderItem.create(playlistWithTracks, playSessionSource));
            }
        };
    }
}
