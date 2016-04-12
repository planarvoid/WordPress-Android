package com.soundcloud.android.playlists;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.rx.eventbus.EventBus;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class InlinePlaylistPresenter extends PlaylistPresenter {

    @Inject
    public InlinePlaylistPresenter(PlaylistOperations playlistOperations,
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
            public Iterable<ListItem> call(PlaylistWithTracks playlist) {
                List<ListItem> listItems = new ArrayList<>(playlist.getTracks().size() + 1);
                listItems.add(PlaylistHeaderItem.create(playlist, createPlaySessionSource(playlist)));
                listItems.addAll(playlist.getTracks());
                return listItems;
            }
        };
    }
}
